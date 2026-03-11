package com.web3community.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

/**
 * Spring Cloud Gateway 공통 설정
 *
 * ## 역할
 * application.yml의 `key-resolver: "#{@ipKeyResolver}"` 참조를 해결하기 위한
 * [KeyResolver] 빈을 등록한다. Rate Limiter가 어떤 키를 기준으로 제한을 적용할지
 * 결정하는 전략 컴포넌트이다.
 *
 * ## IP 추출 우선순위
 * 1. `X-Forwarded-For` 헤더의 첫 번째 IP (리버스 프록시/로드밸런서 뒤에 있을 때)
 * 2. TCP 연결의 실제 원격 주소 (`remoteAddress`)
 * 3. 추출 실패 시 `"unknown"` 문자열 사용 (Rate Limit은 동작하나 식별 불가)
 *
 * ## 주의사항
 * - 신뢰할 수 없는 클라이언트가 `X-Forwarded-For`를 조작하면 IP 우회가 가능하다.
 *   프록시/로드밸런서에서 이 헤더를 덮어쓰도록 인프라 설정을 반드시 구성해야 한다.
 * - 운영 환경에서는 Nginx/ALB가 `X-Real-IP` 또는 `X-Forwarded-For`를 주입하는지 확인하라.
 */
@Configuration
class GatewayConfig {

    /**
     * IP 기반 Rate Limiter 키 리졸버 빈 등록
     *
     * application.yml의 `RequestRateLimiter` 필터에서 `key-resolver: "#{@ipKeyResolver}"`로
     * 참조되는 빈. 요청마다 클라이언트 IP를 추출하여 Redis의 Rate Limit 카운터 키로 사용한다.
     *
     * ### 동작 방식
     * ```
     * Request → GatewayConfig.ipKeyResolver
     *     ↓ X-Forwarded-For 헤더 확인
     *     ↓ 없으면 TCP remoteAddress 사용
     *     ↓ 둘 다 없으면 "unknown" 반환
     * Redis Rate Limiter
     *     ↓ "rate_limiter.{ip}.tokens" 키로 토큰 버킷 관리
     * 허용/거부 결정
     * ```
     *
     * @return IP 주소를 키로 반환하는 [KeyResolver] 빈
     */
    @Bean
    fun ipKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            // 1. X-Forwarded-For 헤더에서 원본 클라이언트 IP 추출
            //    - 다중 프록시 환경에서 "client, proxy1, proxy2" 형태일 수 있으므로
            //      첫 번째 값(원본 클라이언트)만 사용
            val forwardedFor = exchange.request.headers
                .getFirst("X-Forwarded-For")
                ?.split(",")
                ?.first()
                ?.trim()

            // 2. 헤더가 없으면 TCP 연결의 실제 원격 주소 사용
            val remoteAddress = exchange.request.remoteAddress
                ?.address
                ?.hostAddress

            // 3. 최종 IP 결정 (null-safe 체이닝)
            val ip = forwardedFor
                ?: remoteAddress
                ?: "unknown"

            Mono.just(ip)
        }
    }
}
