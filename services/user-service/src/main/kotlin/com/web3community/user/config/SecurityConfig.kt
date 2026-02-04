package com.web3community.user.config

import com.web3community.user.repository.UserRepository
import com.web3community.util.JwtUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.ReactiveAuthorizationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authorization.AuthorizationContext
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import reactor.core.publisher.Mono

/**
 * Spring Security 설정 클래스
 * WebFlux 기반의 리액티브 보안 설정
 * JWT 토큰 기반 인증/인가 처리
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil
) {

    /**
     * 보안 필터 체인 설정
     * HTTP 보안 규칙, 인증/인가 설정
     */
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            // CSRF 비활성화 (REST API이므로)
            .csrf { it.disable() }
            
            // CORS 활성화
            .cors { it.disable() }
            
            // 요청별 접근 권한 설정
            .authorizeExchange { exchanges ->
                exchanges
                    // 공개 접근 경로
                    .pathMatchers(
                        "/api/v1/users/register",
                        "/api/v1/users/login",
                        "/api/v1/users/email/*/exists",
                        "/api/v1/users/username/*/exists",
                        "/api/v1/health",
                        "/actuator/**"
                    ).permitAll()
                    
                    // GET 요청은 인증 없이 접근 가능 (읽기 전용)
                    .pathMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                    
                    // 관리자 전용 경로
                    .pathMatchers("/api/v1/users/*/permanent").hasRole("ADMIN")
                    .pathMatchers("/api/v1/users/*/role").hasRole("ADMIN")
                    .pathMatchers("/api/v1/users/stats").hasRole("ADMIN")
                    
                    // 기타 모든 요청은 인증 필요
                    .anyExchange().authenticated()
            }
            
            // JWT 인증 필터 추가
            .addFilterAt(jwtAuthenticationFilter(), org.springframework.security.web.server.context.SecurityContextWebFilter::class.java)
            
            // 예외 처리 설정
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { exchange, ex ->
                        val response = exchange.response
                        response.statusCode = HttpStatus.UNAUTHORIZED
                        response.writeWith(
                            Mono.just(response.bufferFactory().wrap(
                                """{"error": "인증이 필요합니다.", "message": "${ex.message}"}""".toByteArray()
                            ))
                        )
                    }
                    .accessDeniedHandler { exchange, denied ->
                        val response = exchange.response
                        response.statusCode = HttpStatus.FORBIDDEN
                        response.writeWith(
                            Mono.just(response.bufferFactory().wrap(
                                """{"error": "접근 권한이 없습니다.", "message": "권한이 부족합니다."}""".toByteArray()
                            ))
                        )
                    }
            }
            
            .build()
    }

    /**
     * JWT 인증 필터
     * Authorization 헤더에서 JWT 토큰 추출 및 검증
     */
    @Bean
    fun jwtAuthenticationFilter(): org.springframework.security.web.server.WebFilter {
        return org.springframework.security.web.server.WebFilter { exchange, chain ->
            val token = jwtUtil.extractTokenFromBearer(
                exchange.request.headers.getFirst("Authorization")
            )
            
            if (token != null && jwtUtil.validateToken(token)) {
                val subject = jwtUtil.getSubjectFromToken(token)
                val role = jwtUtil.getRoleFromToken(token)
                
                if (subject != null && role != null) {
                    userRepository.findByEmail(subject)
                        .flatMap { user ->
                            if (user.isActive) {
                                val authorities = org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_${role}")
                                val authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    user, null, listOf(authorities)
                                )
                                val securityContext = org.springframework.security.core.context.SecurityContextImpl(authentication)
                                
                                chain.filter(exchange).contextWrite(
                                    org.springframework.security.core.context.ReactorSecurityContextHolder.withSecurityContext(
                                        Mono.just(securityContext)
                                    )
                                )
                            } else {
                                chain.filter(exchange)
                            }
                        }
                        .switchIfEmpty(chain.filter(exchange))
                } else {
                    chain.filter(exchange)
                }
            } else {
                chain.filter(exchange)
            }
        }
    }

    /**
     * 커스텀 권한 관리자
     * 역할 기반의 세밀한 권한 제어
     */
    @Bean
    fun customAuthorizationManager(): ReactiveAuthorizationManager<AuthorizationContext> {
        return ReactiveAuthorizationManager { authentication, context ->
            if (authentication == null || !authentication.isAuthenticated) {
                Mono.just(AuthorizationDecision(false))
            } else {
                val requestPath = context.exchange.request.path.value()
                val user = authentication.principal as? com.web3community.user.entity.User
                
                if (user != null) {
                    val hasAccess = when {
                        // 관리자는 모든 접근 가능
                        user.role.name == "ADMIN" -> true
                        
                        // 모더레이터는 특정 경로 접근 가능
                        user.role.name == "MODERATOR" && requestPath.matches(
                            Regex("/api/v1/users/[^/]+/(toggle-active|role)")
                        ) -> true
                        
                        // 일반 사용자는 본인 정보만 수정 가능
                        user.role.name == "USER" && requestPath.matches(
                            Regex("/api/v1/users/\\d+")
                        ) -> {
                            val userIdFromPath = requestPath.substringAfterLast("/").toLongOrNull()
                            userIdFromPath == user.id
                        }
                        
                        else -> false
                    }
                    
                    Mono.just(AuthorizationDecision(hasAccess))
                } else {
                    Mono.just(AuthorizationDecision(false))
                }
            }
        }
    }

    /**
     * 비밀번호 인코더 Bean
     * BCrypt 알고리즘 사용
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * CORS 설정 Bean
     * Cross-Origin Resource Sharing 설정
     */
    @Bean
    fun corsConfigurationSource(): org.springframework.web.cors.reactive.CorsConfigurationSource {
        val config = org.springframework.web.cors.CorsConfiguration()
        
        // 허용할 출처 설정
        config.allowedOriginPatterns = listOf("*")
        
        // 허용할 HTTP 메소드
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        
        // 허용할 헤더
        config.allowedHeaders = listOf("*")
        
        // 인증 정보 포함 허용
        config.allowCredentials = true
        
        // preflight 요청 캐시 시간
        config.maxAge = 3600L
        
        val source = org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        
        return source
    }

    /**
     * ReactiveJwtAuthenticationManager Bean
     * JWT 인증 관리자
     */
    @Bean
    fun reactiveJwtAuthenticationManager(): org.springframework.security.authentication.ReactiveAuthenticationManager {
        return org.springframework.security.authentication.ReactiveAuthenticationManager { authentication ->
            val token = jwtUtil.extractTokenFromBearer(
                "Bearer ${authentication.credentials}"
            )
            
            if (token != null && jwtUtil.validateToken(token)) {
                val subject = jwtUtil.getSubjectFromToken(token)
                val role = jwtUtil.getRoleFromToken(token)
                
                if (subject != null && role != null) {
                    userRepository.findByEmail(subject)
                        .flatMap { user ->
                            if (user.isActive) {
                                val authorities = listOf(
                                    org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_$role")
                                )
                                val auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    user, null, authorities
                                )
                                Mono.just(auth)
                            } else {
                                Mono.empty()
                            }
                        }
                } else {
                    Mono.empty()
                }
            } else {
                Mono.empty()
            }
        }
    }
}