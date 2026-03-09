package com.web3community.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

/**
 * Redis 기반 Rate Limiting 게이트웨이 필터
 *
 * ## 알고리즘: Sliding Window Counter
 * 고정 윈도우(Fixed Window)는 윈도우 경계에서 순간적으로 2배 요청이 가능한 문제가 있다.
 * 슬라이딩 윈도우는 현재 시각을 기준으로 과거 N초를 실시간으로 계산하므로 더 정확하다.
 *
 * ### Redis 자료구조: Sorted Set (ZSet)
 * - Key: `rate_limit:{identifier}` (IP 또는 userId)
 * - Value: 요청 타임스탬프(밀리초)를 Score로 저장
 * - TTL: 윈도우 크기(60초)로 자동 만료
 *
 * ### 처리 단계 (원자성을 위해 Lua 스크립트 사용 권장, 여기서는 파이프라인 근사치)
 * 1. 현재 윈도우(현재 시각 - 60초) 이전 항목 제거 (ZREMRANGEBYSCORE)
 * 2. 현재 타임스탬프 추가 (ZADD)
 * 3. 전체 카운트 조회 (ZCARD)
 * 4. TTL 갱신 (EXPIRE)
 * 5. 카운트가 한도 초과 시 429 반환
 *
 * ## 제한 기준
 * - **IP 기준** (미인증 요청): 분당 100 요청
 * - **인증된 사용자 기준**: 분당 300 요청 (X-User-Id 헤더 존재 시)
 * - **블록체인 서비스**: RouteConfig에서 오버라이드하여 분당 30 요청 적용
 *
 * ## 응답 헤더
 * - `X-RateLimit-Limit`: 허용 한도
 * - `X-RateLimit-Remaining`: 남은 요청 수
 * - `X-RateLimit-Reset`: 윈도우 초기화까지 남은 시간(초)
 *
 * @param redisTemplate Redis 연산을 위한 Reactive 템플릿
 */
@Component
class RateLimitFilter(
    private val redisTemplate: ReactiveStringRedisTemplate,
) : AbstractGatewayFilterFactory<RateLimitFilter.Config>(Config::class.java) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        /** Redis Key 접두사: 다른 키와 충돌 방지 */
        private const val RATE_LIMIT_KEY_PREFIX = "rate_limit:"

        /** 슬라이딩 윈도우 크기: 60초 */
        private val WINDOW_SIZE: Duration = Duration.ofSeconds(60)

        /** 기본 IP 기준 분당 허용 요청 수 */
        private const val DEFAULT_IP_RATE = 100L

        /** 기본 인증 사용자 기준 분당 허용 요청 수 */
        private const val DEFAULT_USER_RATE = 300L
    }

    /**
     * Rate Limit 필터 설정 클래스
     *
     * [RouteConfig]에서 라우트별로 다른 Rate Limit을 적용할 때 사용.
     *
     * @param useUserId true이면 X-User-Id 헤더 기반 제한, false이면 IP 기반 제한
     * @param replenishRate 분당 허용 요청 수 (0이면 기본값 사용)
     * @param burstCapacity 윈도우 내 최대 허용 요청 수 (replenishRate와 동일하게 사용)
     */
    data class Config(
        val useUserId: Boolean = false,
        val replenishRate: Long = 0L,     // 0이면 useUserId에 따른 기본값 사용
        val burstCapacity: Long = 0L,
    )

    /**
     * Rate Limit 게이트웨이 필터를 생성한다.
     *
     * @param config 라우트별 Rate Limit 설정
     * @return Rate Limit 로직이 적용된 [GatewayFilter]
     */
    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request

            // ── Rate Limit 키 결정 ─────────────────────────────────────────────
            // useUserId=true이고 X-User-Id 헤더가 있으면 사용자 기준, 없으면 IP 기준으로 폴백
            val userId = request.headers.getFirst("X-User-Id")
            val identifier = if (config.useUserId && userId != null) {
                "user:$userId"
            } else {
                // X-Forwarded-For: 로드밸런서/프록시 뒤의 실제 클라이언트 IP
                val ip = request.headers.getFirst("X-Forwarded-For")?.split(",")?.first()?.trim()
                    ?: request.remoteAddress?.address?.hostAddress
                    ?: "unknown"
                "ip:$ip"
            }

            // ── 허용 한도 결정 ─────────────────────────────────────────────────
            val limit = when {
                config.replenishRate > 0 -> config.replenishRate  // 명시적 설정값 우선
                config.useUserId -> DEFAULT_USER_RATE              // 인증 사용자 기본값
                else -> DEFAULT_IP_RATE                            // IP 기본값
            }

            val redisKey = "$RATE_LIMIT_KEY_PREFIX$identifier"
            val now = Instant.now()
            val nowMs = now.toEpochMilli()
            // 슬라이딩 윈도우 시작점: 현재 시각 - 60초
            val windowStartMs = now.minus(WINDOW_SIZE).toEpochMilli()

            // ── Redis Sliding Window 처리 ──────────────────────────────────────
            // Reactive 체인: ZSet 연산으로 슬라이딩 윈도우 카운터 구현
            isRateLimited(redisKey, nowMs, windowStartMs, limit)
                .flatMap { (limited, currentCount) ->
                    if (limited) {
                        // 한도 초과: 429 Too Many Requests 반환
                        log.warn(
                            "Rate Limit 초과: key={}, count={}, limit={}",
                            redisKey, currentCount, limit
                        )
                        val response = exchange.response
                        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                        response.headers.contentType = MediaType.APPLICATION_JSON
                        // Retry-After 헤더: 클라이언트가 다음 요청까지 대기해야 할 시간(초)
                        response.headers.set("Retry-After", WINDOW_SIZE.seconds.toString())
                        // Rate Limit 상태 헤더 추가
                        response.headers.set("X-RateLimit-Limit", limit.toString())
                        response.headers.set("X-RateLimit-Remaining", "0")
                        response.headers.set("X-RateLimit-Reset", WINDOW_SIZE.seconds.toString())

                        val body = """{"status":429,"error":"Too Many Requests","message":"요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.","retryAfter":${WINDOW_SIZE.seconds}}"""
                        val buffer = response.bufferFactory()
                            .wrap(body.toByteArray(StandardCharsets.UTF_8))
                        response.writeWith(Mono.just(buffer))
                    } else {
                        // 허용: 남은 요청 수 헤더를 추가하고 다음 필터로 전달
                        val remaining = (limit - currentCount).coerceAtLeast(0)
                        val mutatedExchange = exchange.mutate()
                            .response(exchange.response.apply {
                                headers.set("X-RateLimit-Limit", limit.toString())
                                headers.set("X-RateLimit-Remaining", remaining.toString())
                            })
                            .build()
                        chain.filter(mutatedExchange)
                    }
                }
        }
    }

    /**
     * Redis Sorted Set을 이용한 슬라이딩 윈도우 Rate Limit 검사
     *
     * ## Redis 연산 순서
     * 1. [zRemRangeByScore]: 윈도우 밖의 오래된 타임스탬프 제거
     * 2. [zAdd]: 현재 요청 타임스탬프를 Score로 추가
     *    (Value도 타임스탬프 문자열로 저장하여 중복 방지)
     * 3. [zCard]: 현재 윈도우 내 요청 수 조회
     * 4. [expire]: TTL 갱신 (윈도우 크기 + 1초 여유)
     *
     * ## 주의: 원자성(Atomicity)
     * 위 연산들은 각각 별도의 Redis 명령이므로 엄밀히는 원자적이지 않다.
     * 고트래픽 환경에서는 Redis Lua 스크립트로 원자성을 보장해야 한다.
     * 현재 구현은 개발/중간 규모 환경에 적합하다.
     *
     * @param key Redis 키
     * @param nowMs 현재 시각 (밀리초)
     * @param windowStartMs 슬라이딩 윈도우 시작 시각 (밀리초)
     * @param limit 허용 최대 요청 수
     * @return (한도초과여부, 현재카운트) 쌍을 담은 Mono
     */
    private fun isRateLimited(
        key: String,
        nowMs: Long,
        windowStartMs: Long,
        limit: Long,
    ): Mono<Pair<Boolean, Long>> {
        val zSetOps = redisTemplate.opsForZSet()

        return zSetOps
            // 1단계: 윈도우 이전(60초 초과) 타임스탬프 제거
            .removeRangeByScore(key, 0.0, windowStartMs.toDouble() - 1)
            .then(
                // 2단계: 현재 요청 타임스탬프 추가
                // Value를 "{nowMs}-{nanoTime}"으로 지정하여 동시 요청 간 중복 방지
                zSetOps.add(
                    key,
                    "$nowMs-${System.nanoTime()}",  // 고유 멤버 값
                    nowMs.toDouble()                 // Score = 타임스탬프 (정렬 기준)
                )
            )
            .then(
                // 3단계: 현재 윈도우 내 총 요청 수 조회
                zSetOps.size(key)
            )
            .flatMap { currentCount ->
                // 4단계: TTL 갱신 (윈도우 크기 + 여유 시간)
                // TTL이 없으면 키가 영구 보관되어 메모리 누수 발생
                redisTemplate.expire(key, WINDOW_SIZE.plusSeconds(1))
                    .thenReturn(currentCount ?: 0L)
            }
            .map { currentCount ->
                // limit 초과 여부 판단 (currentCount는 방금 추가된 요청 포함)
                val limited = currentCount > limit
                Pair(limited, currentCount)
            }
            .onErrorResume { e ->
                // Redis 장애 시: Rate Limit 통과 처리 (Fail Open 전략)
                // 서비스 가용성 우선. Fail Closed가 필요하면 Pair(true, 0L) 반환
                log.error("Redis Rate Limit 처리 중 오류 발생: key={}", key, e)
                Mono.just(Pair(false, 0L))
            }
    }
}
