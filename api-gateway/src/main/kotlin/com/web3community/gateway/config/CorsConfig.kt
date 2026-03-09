package com.web3community.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * CORS(Cross-Origin Resource Sharing) 설정
 *
 * ## 필요성
 * Vue.js 개발 서버(localhost:5173)에서 API Gateway(localhost:8080)로 요청 시
 * 브라우저가 Origin이 다르다고 판단하여 Preflight(OPTIONS) 요청을 먼저 보낸다.
 * 서버가 적절한 CORS 헤더를 반환하지 않으면 브라우저가 요청을 차단한다.
 *
 * ## Webflux에서의 CORS 처리
 * - Spring MVC: CorsFilter (서블릿 필터)
 * - Spring Webflux/Gateway: [CorsWebFilter] (리액티브 웹 필터)
 * - Gateway는 반드시 [CorsWebFilter] Bean을 사용해야 한다.
 *   application.yml의 `globalcors` 설정과 병행 사용 시 중복 헤더가 발생할 수 있으므로
 *   둘 중 하나만 사용하거나, yml 설정을 제거하고 이 Bean을 단독으로 사용하는 것을 권장.
 *
 * ## 운영 환경 고려사항
 * FRONTEND_URL 환경 변수로 허용 Origin을 주입한다.
 * 운영에서는 와일드카드(*) 대신 명시적 도메인을 사용해야 한다.
 */
@Configuration
class CorsConfig {

    /**
     * 허용할 프론트엔드 URL (환경 변수로 주입)
     * 기본값: Vue.js Vite 개발 서버 포트
     */
    @Value("\${FRONTEND_URL:http://localhost:5173}")
    private lateinit var frontendUrl: String

    /**
     * CORS 웹 필터 빈 등록
     *
     * Webflux 환경에서 CORS 정책을 적용하는 필터.
     * 모든 라우트에 공통 적용된다.
     *
     * @return CORS 정책이 적용된 [CorsWebFilter]
     */
    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration().apply {

            // ── 허용 Origin ───────────────────────────────────────────────────
            // 브라우저가 요청을 보낼 수 있는 출처(Origin) 목록
            // 운영: 실제 도메인만 허용, 개발: localhost 포트 추가
            allowedOrigins = listOf(
                frontendUrl,                    // 환경 변수로 주입된 운영/스테이징 URL
                "http://localhost:5173",        // Vite 기본 포트
                "http://localhost:3000",        // Vue CLI 기본 포트 (대체)
                "http://127.0.0.1:5173",        // Vite (IP 주소 형식)
            )

            // ── 허용 HTTP 메서드 ──────────────────────────────────────────────
            // RESTful API가 사용하는 모든 메서드 허용
            allowedMethods = listOf(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.OPTIONS.name(),      // Preflight 요청 처리를 위해 필수
            )

            // ── 허용 헤더 ─────────────────────────────────────────────────────
            // 클라이언트가 보낼 수 있는 요청 헤더 목록
            // "*"는 모든 헤더를 허용하지만, 운영에서는 명시적으로 지정 권장
            allowedHeaders = listOf(
                HttpHeaders.AUTHORIZATION,      // JWT Bearer 토큰
                HttpHeaders.CONTENT_TYPE,       // application/json
                HttpHeaders.ACCEPT,
                "X-Request-ID",                 // 클라이언트가 직접 지정하는 추적 ID
                "X-User-Id",                    // 내부 서비스 간 사용자 식별 헤더
            )

            // ── 노출 헤더 ─────────────────────────────────────────────────────
            // 브라우저 JS가 읽을 수 있도록 노출할 응답 헤더 목록
            // 기본적으로 브라우저는 제한된 헤더만 읽을 수 있음
            exposedHeaders = listOf(
                "X-Request-ID",                 // 추적 ID를 클라이언트가 읽을 수 있도록 노출
                HttpHeaders.CONTENT_DISPOSITION, // 파일 다운로드 시 필요
            )

            // ── 자격증명 허용 ─────────────────────────────────────────────────
            // Authorization 헤더나 쿠키를 포함한 요청을 허용
            // true일 경우 allowedOrigins에 "*" 사용 불가 (명시적 Origin 필요)
            allowCredentials = true

            // ── Preflight 캐시 시간 ───────────────────────────────────────────
            // OPTIONS Preflight 요청 결과를 브라우저가 캐시하는 시간(초)
            // 1시간으로 설정하여 불필요한 Preflight 요청 감소
            maxAge = 3600L
        }

        // 모든 경로에 CORS 설정 적용
        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfig)
        }

        return CorsWebFilter(source)
    }
}
