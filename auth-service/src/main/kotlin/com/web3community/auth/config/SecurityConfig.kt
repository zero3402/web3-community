package com.web3community.auth.config

import com.web3community.auth.service.OAuth2Service
import com.web3community.common.security.JwtAuthenticationFilter
import com.web3community.common.security.JwtUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 설정 클래스
 *
 * Auth Service의 보안 정책을 정의한다:
 * - JWT 기반 Stateless 인증 (세션 미사용)
 * - OAuth2 Authorization Code Flow 소셜 로그인
 * - 공개 엔드포인트 / 인증 필요 엔드포인트 구분
 * - CORS 설정 (프론트엔드 Vue.js 연동)
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtUtils: JwtUtils,                    // JWT 생성/검증 유틸 (common-module)
    private val oauth2Service: OAuth2Service,           // OAuth2 사용자 정보 처리 서비스
) {

    /**
     * 비밀번호 인코더 빈 등록
     *
     * BCrypt 해시 알고리즘을 사용한다.
     * - work factor(strength): 기본값 10 (2^10 = 1024번 반복 해시)
     * - 매 해시마다 랜덤 salt를 생성하므로 같은 비밀번호라도 결과가 다름
     * - 레인보우 테이블 공격, 브루트포스 공격에 강함
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /**
     * AuthenticationManager 빈 등록
     *
     * 일반 로그인 시 이메일/비밀번호 검증에 사용된다.
     * Spring Security 내부에서 UserDetailsService와 PasswordEncoder를 조합하여
     * 인증을 처리한다.
     *
     * @param authenticationConfiguration Spring Security 기본 인증 설정
     * @return AuthenticationManager 인스턴스
     */
    @Bean
    fun authenticationManager(
        authenticationConfiguration: AuthenticationConfiguration
    ): AuthenticationManager = authenticationConfiguration.authenticationManager

    /**
     * JWT 인증 필터 빈 등록
     *
     * 모든 HTTP 요청에서 Authorization 헤더를 검사하여 JWT를 검증한다.
     * UsernamePasswordAuthenticationFilter 앞에 위치하여 JWT 검증을 먼저 수행한다.
     *
     * @return JwtAuthenticationFilter 인스턴스
     */
    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter =
        JwtAuthenticationFilter(jwtUtils)

    /**
     * 보안 필터 체인 설정
     *
     * Spring Security의 핵심 보안 정책을 정의한다.
     *
     * 엔드포인트 접근 정책:
     * - 공개 (인증 불필요): /auth/login, /auth/register, /auth/refresh, /auth/oauth2/**
     * - 인증 필요: 그 외 모든 엔드포인트
     *
     * @param http HttpSecurity 빌더
     * @return 구성된 SecurityFilterChain
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF 비활성화: JWT 기반 Stateless API는 CSRF 공격 벡터가 없음
            // (쿠키 기반 세션을 사용하지 않으므로 CSRF 토큰이 불필요)
            .csrf { it.disable() }

            // CORS 설정 활성화 (corsConfigurationSource 빈 사용)
            .cors { it.configurationSource(corsConfigurationSource()) }

            // 세션 정책: STATELESS
            // JWT를 사용하므로 서버 측 세션을 전혀 생성/사용하지 않는다.
            // 수평 확장(Scale-out) 시 세션 공유 문제가 없다.
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // HTTP 기본 인증 비활성화 (브라우저 팝업 방지)
            .httpBasic { it.disable() }

            // 폼 로그인 비활성화 (REST API는 JSON으로 인증)
            .formLogin { it.disable() }

            // ── 엔드포인트 접근 권한 설정 ──────────────────────────────────
            .authorizeHttpRequests { auth ->
                auth
                    // 인증 관련 공개 엔드포인트
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()

                    // OAuth2 소셜 로그인 콜백 (인가 코드 수신)
                    .requestMatchers("/auth/oauth2/**").permitAll()

                    // Spring Security OAuth2 기본 엔드포인트
                    .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()

                    // Actuator Health Check (쿠버네티스 Liveness/Readiness 프로브)
                    .requestMatchers("/actuator/health").permitAll()

                    // 그 외 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }

            // ── OAuth2 로그인 설정 ─────────────────────────────────────────
            .oauth2Login { oauth2 ->
                oauth2
                    // 사용자 정보 엔드포인트 처리: OAuth2Service가 각 제공자별 파싱 담당
                    .userInfoEndpoint { it.userService(oauth2Service) }
                    // OAuth2 로그인 성공 핸들러는 OAuth2Controller에서 수동으로 처리
                    // (JWT 발급 로직을 컨트롤러에 집중시키기 위해 핸들러 대신 Service 활용)
            }

            // ── JWT 인증 필터 등록 ──────────────────────────────────────────
            // UsernamePasswordAuthenticationFilter 이전에 실행되어 JWT를 먼저 검증
            .addFilterBefore(
                jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정
     *
     * 프론트엔드(Vue.js)에서 Auth Service API를 호출할 수 있도록 허용한다.
     * 운영 환경에서는 allowedOrigins를 실제 도메인으로 제한해야 한다.
     *
     * @return CorsConfigurationSource CORS 정책 소스
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // 허용할 오리진 (개발: localhost, 운영: 실제 도메인)
            allowedOriginPatterns = listOf("*")

            // 허용할 HTTP 메서드
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")

            // 허용할 헤더
            allowedHeaders = listOf("*")

            // 인증 정보(쿠키, Authorization 헤더) 포함 허용
            allowCredentials = true

            // Preflight 응답 캐시 시간 (초)
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
