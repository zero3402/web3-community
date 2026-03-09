package com.web3community.gateway.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

/**
 * JWT 인증 게이트웨이 필터
 *
 * ## 처리 흐름
 * 1. 요청 경로가 화이트리스트(인증 제외 경로)에 해당하면 필터를 건너뜀
 * 2. `Authorization: Bearer <token>` 헤더에서 JWT 토큰 추출
 * 3. 토큰 서명 검증 및 만료 시간 확인
 * 4. 검증 성공 시 Claim에서 userId를 추출하여 `X-User-Id` 헤더로 downstream에 전달
 * 5. 검증 실패 시 401 Unauthorized 또는 403 Forbidden 반환
 *
 * ## downstream 서비스에서의 사용
 * ```kotlin
 * // User, Board, Blockchain 서비스에서 헤더로 사용자 식별
 * val userId = request.getHeader("X-User-Id")
 * ```
 *
 * ## 보안 고려사항
 * - JWT 시크릿 키는 반드시 환경 변수로 주입 (application.yml 하드코딩 금지)
 * - Auth 서비스와 동일한 시크릿 키를 사용해야 검증 가능
 * - 토큰 탈취 대비: Refresh Token은 Auth 서비스에서만 처리, Gateway는 Access Token만 검증
 *
 * @see GatewayFilter
 * @see AbstractGatewayFilterFactory
 */
@Component
class JwtAuthFilter(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,

    @Value("\${jwt.whitelist}")
    private val whitelist: List<String>,
) : AbstractGatewayFilterFactory<JwtAuthFilter.Config>(Config::class.java) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Ant 스타일 경로 매칭 유틸리티 (/api/v1/auth/** 패턴 처리) */
    private val antPathMatcher = AntPathMatcher()

    /**
     * HMAC-SHA256 서명 검증에 사용할 키
     * lazy 초기화: 첫 요청 시 한 번만 생성하여 재사용 (성능 최적화)
     */
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * JWT 파서 인스턴스 (스레드 안전, 재사용 가능)
     */
    private val jwtParser by lazy {
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
    }

    /**
     * 필터 적용 설정 클래스
     *
     * application.yml 또는 [RouteConfig]에서 필터 파라미터를 전달할 때 사용.
     * 현재는 추가 설정 없이 기본값을 사용하지만, 향후 역할(Role) 기반 접근 제어 등
     * 확장 포인트로 활용 가능하다.
     */
    class Config {
        // 향후 확장: requiredRoles, adminOnly 등 역할 기반 접근 제어 속성 추가 가능
    }

    /**
     * 게이트웨이 필터 생성 메서드
     *
     * Spring Cloud Gateway가 요청마다 이 메서드를 호출하여 필터 체인을 구성한다.
     *
     * @param config 필터 설정 (현재 미사용)
     * @return 요청을 처리하는 [GatewayFilter]
     */
    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val path = request.uri.path

            // ── 화이트리스트 확인 ─────────────────────────────────────────────
            // /api/v1/auth/**, /actuator/** 등 인증 불필요 경로는 필터 건너뜀
            if (isWhitelisted(path)) {
                log.debug("화이트리스트 경로 통과: {}", path)
                return@GatewayFilter chain.filter(exchange)
            }

            // ── Authorization 헤더 추출 ───────────────────────────────────────
            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 401 반환
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization 헤더 없음 또는 형식 오류: path={}", path)
                return@GatewayFilter unauthorizedResponse(exchange, "Authorization 헤더가 필요합니다.")
            }

            // "Bearer " 이후 7글자부터 실제 토큰
            val token = authHeader.substring(7)

            // ── JWT 토큰 검증 ──────────────────────────────────────────────────
            val claims = try {
                parseToken(token)
            } catch (e: ExpiredJwtException) {
                // 만료된 토큰: 클라이언트가 Refresh Token으로 갱신 필요
                log.warn("만료된 JWT 토큰: path={}, message={}", path, e.message)
                return@GatewayFilter unauthorizedResponse(exchange, "토큰이 만료되었습니다. 재로그인이 필요합니다.")
            } catch (e: SignatureException) {
                // 서명 불일치: 위조된 토큰
                log.warn("JWT 서명 검증 실패: path={}", path)
                return@GatewayFilter forbiddenResponse(exchange, "유효하지 않은 토큰입니다.")
            } catch (e: MalformedJwtException) {
                // 토큰 형식 오류: 잘못된 JWT 형식
                log.warn("잘못된 JWT 형식: path={}", path)
                return@GatewayFilter unauthorizedResponse(exchange, "토큰 형식이 올바르지 않습니다.")
            } catch (e: UnsupportedJwtException) {
                log.warn("지원하지 않는 JWT: path={}", path)
                return@GatewayFilter unauthorizedResponse(exchange, "지원하지 않는 토큰 형식입니다.")
            } catch (e: Exception) {
                // 예상치 못한 오류: 보안을 위해 세부 내용 노출하지 않음
                log.error("JWT 검증 중 예외 발생: path={}", path, e)
                return@GatewayFilter unauthorizedResponse(exchange, "토큰 검증에 실패했습니다.")
            }

            // ── userId 추출 및 헤더 전달 ──────────────────────────────────────
            // JWT Claim의 "sub"(subject) 필드에서 userId 추출
            val userId = claims.subject
                ?: return@GatewayFilter unauthorizedResponse(exchange, "토큰에 사용자 정보가 없습니다.")

            log.debug("JWT 인증 성공: userId={}, path={}", userId, path)

            // X-User-Id 헤더를 추가하여 downstream 서비스로 전달
            // downstream 서비스는 Authorization 헤더를 재검증할 필요 없이 이 헤더만 사용
            val mutatedExchange = exchange.mutate()
                .request { requestBuilder ->
                    requestBuilder
                        // 인증된 사용자 ID 전달
                        .header("X-User-Id", userId)
                        // 추가 Claim 전달 (역할, 이메일 등 필요 시 활용)
                        .header("X-User-Role", claims["role"]?.toString() ?: "USER")
                }
                .build()

            chain.filter(mutatedExchange)
        }
    }

    /**
     * JWT 토큰을 파싱하여 Claims를 반환한다.
     *
     * @param token Bearer 토큰 문자열 (앞의 "Bearer " 제거 후)
     * @return 파싱된 JWT Claims (subject, role 등 포함)
     * @throws ExpiredJwtException 토큰 만료 시
     * @throws SignatureException 서명 불일치 시
     * @throws MalformedJwtException 잘못된 토큰 형식
     */
    private fun parseToken(token: String): Claims {
        return jwtParser
            .parseSignedClaims(token)
            .payload
    }

    /**
     * 요청 경로가 화이트리스트에 해당하는지 확인한다.
     *
     * Ant 스타일 패턴 매칭을 사용한다.
     * 예: `/api/v1/auth/**` 패턴은 `/api/v1/auth/login`, `/api/v1/auth/refresh` 등에 매칭
     *
     * @param path 요청 경로
     * @return 화이트리스트에 포함되면 true
     */
    private fun isWhitelisted(path: String): Boolean {
        return whitelist.any { pattern ->
            antPathMatcher.match(pattern, path)
        }
    }

    /**
     * 401 Unauthorized 응답을 생성한다.
     *
     * 인증 정보가 없거나 형식이 잘못된 경우 사용.
     * WWW-Authenticate 헤더를 포함하여 클라이언트가 인증 방법을 알 수 있도록 한다.
     *
     * @param exchange 현재 서버 웹 교환 컨텍스트
     * @param message 에러 메시지 (응답 바디에 포함)
     * @return 완결된 Mono (이후 필터 체인 실행 중단)
     */
    private fun unauthorizedResponse(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON
        // RFC 7235: WWW-Authenticate 헤더로 인증 스킴 안내
        response.headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"web3-community\"")

        val body = """{"status":401,"error":"Unauthorized","message":"$message"}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray(StandardCharsets.UTF_8))
        return response.writeWith(Mono.just(buffer))
    }

    /**
     * 403 Forbidden 응답을 생성한다.
     *
     * 인증은 되었지만 위조된 토큰 등 보안 위협이 감지된 경우 사용.
     *
     * @param exchange 현재 서버 웹 교환 컨텍스트
     * @param message 에러 메시지
     * @return 완결된 Mono
     */
    private fun forbiddenResponse(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.FORBIDDEN
        response.headers.contentType = MediaType.APPLICATION_JSON

        val body = """{"status":403,"error":"Forbidden","message":"$message"}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray(StandardCharsets.UTF_8))
        return response.writeWith(Mono.just(buffer))
    }
}
