package com.web3community.gateway.config

import com.web3community.gateway.filter.JwtAuthFilter
import com.web3community.gateway.filter.LoggingFilter
import com.web3community.gateway.filter.RateLimitFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveStringRedisTemplate

/**
 * Spring Cloud Gateway 라우팅 설정 (프로그래밍 방식)
 *
 * application.yml의 선언적 방식 대신 코드로 라우팅 규칙을 정의한다.
 * 장점:
 * - 타입 안전성: IDE 자동완성 및 컴파일 타임 오류 감지
 * - 동적 라우팅: 런타임에 조건부 라우트 추가 가능
 * - 테스트 용이성: 라우팅 로직을 단위 테스트로 검증 가능
 *
 * 참고: application.yml의 routes 설정과 이 Bean은 병합되어 동작한다.
 * 중복 라우트 ID가 있을 경우 코드 기반이 우선 적용된다.
 */
@Configuration
class RouteConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val loggingFilter: LoggingFilter,
    private val rateLimitFilter: RateLimitFilter,
) {

    /**
     * 라우트 로케이터 빈 등록
     *
     * Spring Cloud Gateway의 [RouteLocatorBuilder]를 사용하여
     * 각 마이크로서비스로의 라우팅 규칙을 프로그래밍 방식으로 정의한다.
     *
     * @param builder Spring이 주입하는 RouteLocatorBuilder
     * @return 완성된 RouteLocator (Gateway가 요청 라우팅에 사용)
     */
    @Bean
    fun routeLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes {

            // ── Auth 서비스 라우트 ──────────────────────────────────────────────
            // 경로: /api/v1/auth/** → auth-service (인증 불필요)
            // 로그인, 회원가입, 토큰 갱신 등 공개 엔드포인트
            route(id = "auth-service-code") {
                path("/api/v1/auth/**")
                filters {
                    // /api/v1 두 세그먼트 제거 → auth-service는 /auth/** 로 수신
                    stripPrefix(2)
                    // 로깅 필터: 모든 요청에 X-Request-ID 주입 및 처리 시간 기록
                    filter(loggingFilter.apply(LoggingFilter.Config()))
                    // Rate Limit 필터: IP 기준 적용 (인증 없으므로 userId 기준 불가)
                    filter(rateLimitFilter.apply(RateLimitFilter.Config(useUserId = false)))
                    // 주의: JwtAuthFilter는 적용하지 않음 (인증 불필요 경로)
                }
                // Eureka 서비스 디스커버리: lb:// 스킴으로 로드밸런서 활성화
                uri("lb://auth-service")
            }

            // ── User 서비스 라우트 ─────────────────────────────────────────────
            // 경로: /api/v1/users/** → user-service (인증 필요)
            // 회원 프로필 조회/수정, 블록체인 지갑 정보 등
            route(id = "user-service-code") {
                path("/api/v1/users/**")
                filters {
                    stripPrefix(2)
                    filter(loggingFilter.apply(LoggingFilter.Config()))
                    // JWT 인증 필터: 토큰 검증 후 X-User-Id 헤더를 downstream에 전달
                    filter(jwtAuthFilter.apply(JwtAuthFilter.Config()))
                    // Rate Limit: 인증된 사용자 기준 (분당 300 요청)
                    filter(rateLimitFilter.apply(RateLimitFilter.Config(useUserId = true)))
                }
                uri("lb://user-service")
            }

            // ── Board 서비스 라우트 ────────────────────────────────────────────
            // 경로: /api/v1/boards/** → board-service (인증 필요)
            // 게시글 CRUD, 댓글, 좋아요/싫어요 등
            route(id = "board-service-code") {
                path("/api/v1/boards/**")
                filters {
                    stripPrefix(2)
                    filter(loggingFilter.apply(LoggingFilter.Config()))
                    filter(jwtAuthFilter.apply(JwtAuthFilter.Config()))
                    filter(rateLimitFilter.apply(RateLimitFilter.Config(useUserId = true)))
                }
                uri("lb://board-service")
            }

            // ── Blockchain 서비스 라우트 ───────────────────────────────────────
            // 경로: /api/v1/blockchain/** → blockchain-service (인증 필요)
            // BTC/ETH 지갑 생성, 토큰 전송, 다중 출금 등
            // 보안 민감도가 가장 높은 서비스이므로 Rate Limit을 더 엄격하게 적용
            route(id = "blockchain-service-code") {
                path("/api/v1/blockchain/**")
                filters {
                    stripPrefix(2)
                    filter(loggingFilter.apply(LoggingFilter.Config()))
                    filter(jwtAuthFilter.apply(JwtAuthFilter.Config()))
                    // 블록체인 전송은 비용이 발생하므로 더 낮은 Rate Limit 적용
                    filter(rateLimitFilter.apply(
                        RateLimitFilter.Config(
                            useUserId = true,
                            replenishRate = 30,   // 분당 30 요청 (기본 300의 1/10)
                            burstCapacity = 50,
                        )
                    ))
                }
                uri("lb://blockchain-service")
            }
        }
    }
}
