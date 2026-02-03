# =============================================================================
// 🌟 Web3 Community Platform - API Gateway Application
// =============================================================================
// 설명: Spring Cloud Gateway 기반 API 라우팅 및 인증 서비스
// 특징: WebFlux 리액티브, Kotlin DSL, Circuit Breaker, Rate Limiting
// 목적: 마이크로서비스 통합 진입점, 인증/인가, 라우팅 관리
// =============================================================================

package com.web3.community.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersChain
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.context.ReactiveSecurityContextHolder
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.cors.reactive.CorsConfiguration
import org.springframework.web.server.ServerWebExchange
import org.springframework.http.server.reactive.ServerHttpResponse
import reactor.core.publisher.Mono
import java.util.Arrays

// =============================================================================
// 🚀 메인 애플리케이션 클래스
// =============================================================================
@SpringBootApplication
@EnableWebFluxSecurity
class Web3CommunityGatewayApplication

// =============================================================================
// 🔄 라우팅 설정 Bean
// =============================================================================
@Bean
fun customRouteLocator(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator {
    return routeLocatorBuilder.routes
        // =============================================================================
        // 👥 사용자 서비스 라우팅
        // =============================================================================
        .route("user-service") { r ->
            r.path("/api/users/**")
                .filters {
                    it.rewritePath("/api/(?<segment>.*)", "/\${segment}")
                    it.filter(filt ->
                        filt.retry(3)
                            .circuitBreaker("user-service-cb")
                    )
                }
                .uri("lb://user-service")
        }
        
        // =============================================================================
        // 📝 게시글 서비스 라우팅
        // =============================================================================
        .route("post-service") { r ->
            r.path("/api/posts/**")
                .filters {
                    it.rewritePath("/api/(?<segment>.*)", "/\${segment}")
                    it.filter(filt ->
                        filt.retry(2)
                            .circuitBreaker("post-service-cb")
                    )
                }
                .uri("lb://post-service")
        }
        
        // =============================================================================
        // 💬 댓글 서비스 라우팅
        // =============================================================================
        .route("comment-service") { r ->
            r.path("/api/comments/**")
                .filters {
                    it.rewritePath("/api/(?<segment>.*)", "/\${segment}")
                    it.filter(filt ->
                        filt.retry(2)
                            .circuitBreaker("comment-service-cb")
                    )
                }
                .uri("lb://comment-service")
        }
        
        // =============================================================================
        // 🔐 인증 서비스 라우팅
        // =============================================================================
        .route("auth-service") { r ->
            r.path("/api/auth/**")
                .filters {
                    it.rewritePath("/api/(?<segment>.*)", "/\${segment}")
                    it.filter(filt ->
                        filt.retry(2)
                            .circuitBreaker("auth-service-cb")
                    )
                }
                .uri("lb://auth-service")
        }
        
        // =============================================================================
        // 🔔 알림 서비스 라우팅
        // =============================================================================
        .route("notification-service") { r ->
            r.path("/api/notifications/**")
                .filters {
                    it.rewritePath("/api/(?<segment>.*)", "/\${segment}")
                    it.filter(filt ->
                        filt.retry(2)
                            .circuitBreaker("notification-service-cb")
                    )
                }
                .uri("lb://notification-service")
        }
        
        // =============================================================================
        // 🔍 Actuator 엔드포인트 직접 접근 (개발 환경용)
        // =============================================================================
        .route("actuator") { r ->
            r.path("/actuator/**")
                .uri("lb://actuator")
        }
        
        // =============================================================================
        // 🏠 기본 홈페이지 라우팅
        // =============================================================================
        .route("home") { r ->
            r.path("/")
                .uri("lb://frontend")
        }
        
        // =============================================================================
        // 📱 모바일 라우팅 (미래보)
        // =============================================================================
        .route("mobile") { r ->
            r.path("/m/**")
                .filters {
                    it.rewritePath("/m/(?<segment>.*)", "/\${segment}")
                }
                .uri("lb://frontend")
        }
        
        // =============================================================================
        // 🔍 H2C 그레이스케이딩 (서비스 그래이드 필터)
        // =============================================================================
        .route("h2c") { r ->
            r.order(0)
                .matches(HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE, HttpMethod.CONNECT)
                .uri("lb://httpbin")
        }
}

// =============================================================================
// 🔒 보안 설정 Bean
// =============================================================================
@Bean
fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    return http
        .csrf().disable()
        .authorizeExchange(exchanges = arrayOf(
            // 인증이 필요 없는 경로
            http.pathMatchers(
                ServerWebExchangeMatchers.pathMatchers(
                    "/",
                    "/actuator/**",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh"
                )
            ).permitAll()
            
            // 인증이 필요한 API 경로
            http.pathMatchers(
                ServerWebExchangeMatchers.pathMatchers(
                    "/api/users/**",
                    "/api/posts/**",
                    "/api/comments/**",
                    "/api/notifications/**"
                )
            ).authenticated()
            
            // 그 외 모든 경로는 거부
            http.anyExchange().denyAll()
        )
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(unauthorizedEntryPoint())
        .and()
        .build()
}

// =============================================================================
// 🔐 인증 진입점 설정
// =============================================================================
@Bean
fun unauthorizedEntryPoint(): ServerAuthenticationEntryPoint {
    return ServerAuthenticationEntryPoint { exchange, ex ->
        val response = exchange.response
        response.statusCode = org.springframework.http.HttpStatus.UNAUTHORIZED
        response.headers.set("WWW-Authenticate", "Bearer")
        response.headers.set("Content-Type", "application/json")
        
        val errorResponse = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "status" to 401,
            "error" to "Unauthorized",
            "path" to exchange.request.path.value,
            "message" to "Authentication is required"
        )
        
        response.writeWith(Mono.just(response.bufferFactory()
            .wrap(objectMapper.writeValueAsString(errorResponse)))
    }
}

// =============================================================================
// 🌍 CORS 설정 Bean
// =============================================================================
@Bean
fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
    val configuration = CorsConfiguration()
    
    // 허용할 출처 설정 (개발 환경)
    configuration.allowedOriginPatterns = Arrays.asList(
        "http://localhost:3000",
        "http://localhost:3001",
        "http://127.0.0.1:3000",
        "http://web3-community.local"
    )
    
    // 허용할 HTTP 메서드
    configuration.allowedMethods = Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
    )
    
    // 허용할 헤더
    configuration.allowedHeaders = Arrays.asList(
        "Authorization",
        "Content-Type",
        "Accept",
        "Origin",
        "X-Requested-With",
        "Cache-Control",
        "Pragma"
    )
    
    // 노출될 헤더
    configuration.exposedHeaders = Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Total-Count",
        "X-Page-Count"
    )
    
    // 자격 증명 허용
    configuration.allowCredentials = true
    
    // 사전 요청 캐시 시간 (초)
    configuration.maxAge = 3600L
    
    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    return source
}

// =============================================================================
// 🌍 CORS 필터 설정
// =============================================================================
@Bean
fun corsFilter(): CorsWebFilter {
    val corsWebFilter = CorsWebFilter(corsConfigurationSource())
    corsWebFilter.setOrder(1)
    return corsWebFilter
}

// =============================================================================
// 🔐 JWT 토큰 유효성 검사 (필요시 확장)
// =============================================================================
// 이 부분은 실제 JWT 구현 시 확장
// 실제 프로덕션에서는 UserDetailsService, JwtAuthenticationManager 등 필요

// =============================================================================
// 📊 모니터링 Bean 설정
// =============================================================================
@Bean
fun customHealthCheck(): HealthIndicator {
    return object : HealthIndicator {
        override fun health(): Health {
            return Health.Builder()
                .up()
                .withDetail("gateway", "API Gateway is running")
                .withDetail("routes", "Routes are configured")
                .withDetail("circuit-breakers", "Circuit breakers are active")
                .build()
        }
    }
}

// =============================================================================
// 📝 환경 설정 클래스
// =============================================================================
@ConfigurationProperties(prefix = "web3.gateway")
data class GatewayProperties(
    val title: String = "Web3 Community Platform",
    val version: String = "1.0.0",
    val description: String = "MSA-based community platform",
    val allowedOrigins: List<String> = listOf(
        "http://localhost:3000",
        "http://web3-community.local"
    ),
    val security: SecurityProperties = SecurityProperties(),
    val circuitBreaker: CircuitBreakerProperties = CircuitBreakerProperties()
)

// =============================================================================
// 🛡️ 보안 속성 클래스
// =============================================================================
@ConfigurationProperties(prefix = "web3.gateway.security")
data class SecurityProperties(
    val jwtSecret: String = "web3-community-jwt-secret-key",
    val jwtExpiration: Long = 86400L, // 24시간
    val refreshTokenExpiration: Long = 604800L, // 7일
    val ignoredPaths: List<String> = listOf(
        "/",
        "/actuator/**",
        "/api/auth/login",
        "/api/auth/register"
    )
)

// =============================================================================
// ⚡ 서킷 브레이커 속성 클래스
// =============================================================================
@ConfigurationProperties(prefix = "web3.gateway.circuit-breaker")
data class CircuitBreakerProperties(
    val timeout: String = "5s",
    val failureRateThreshold: Float = 50f,
    val slowCallDurationThreshold: String = "2s",
    val slidingWindowType: String = "count_based",
    val minimumNumberOfCalls: Int = 10,
    val slidingWindowSize: Int = 10
)

// =============================================================================
// 🎯 유틸리티 확장 함수
// =============================================================================
object Web3CommunityGatewayExtensions {
    fun logRequest(request: ServerHttpRequest): String {
        return "Gateway Request: ${request.methodValue} ${request.uri.value}"
    }
    
    fun logResponse(response: ServerHttpResponse): String {
        return "Gateway Response: ${response.statusCode.value()}"
    }
    
    fun getUserFromAuth(auth: Authentication?): String? {
        return auth?.name
    }
    
    fun isPublicPath(path: String): Boolean {
        val publicPaths = listOf(
            "/", "/actuator", "/api/auth/login", "/api/auth/register"
        )
        return publicPaths.any { path.startsWith(it) }
    }
}

// =============================================================================
// 📊 라우팅 모니터링 (선택사항)
// =============================================================================
@Component
class RouteMonitor {
    
    @EventListener
    fun onRefresh(event: RefreshRoutesEvent) {
        println("Routes have been refreshed: ${event.source}")
    }
    
    @EventListener
    fun onPredicateAdded(event: PredicateDefinitionAddedEvent) {
        println("Predicate added: ${event.predicateDefinition}")
    }
    
    @EventListener
    fun onFilterAdded(event: FilterDefinitionAddedEvent) {
        println("Filter added: ${event.filterDefinition}")
    }
}

// =============================================================================
// 🚀 메인 함수
// =============================================================================
fun main(args: Array<String>) {
    runApplication<Web3CommunityGatewayApplication>(*args)
}