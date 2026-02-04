package com.web3community.auth

import com.web3community.util.property.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcRepositories
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import com.web3community.util.security.JwtAuthenticationFilter
import com.web3community.util.property.PasswordProperties
import org.springframework.context.annotation.Bean

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableR2dbcRepositories
@EnableConfigurationProperties(JwtProperties::class, PasswordProperties::class)
class AuthSecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationConverter: ServerAuthenticationConverter
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/auth/register").permitAll()
                    .pathMatchers("/auth/login").permitAll()
                    .pathMatchers("/auth/refresh").permitAll()
                    .pathMatchers("/auth/reset-password").permitAll()
                    .pathMatchers("/auth/reset-password-confirm").permitAll()
                    // All other requests need authentication
                    .anyExchange().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.server.authentication.AuthenticationWebFilter::class.java)
            .build()
    }

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration()
        corsConfig.applyPermitDefaultValues()
        corsConfig.addAllowedMethod("*")
        corsConfig.addAllowedHeader("*")
        corsConfig.setAllowCredentials(true)
        corsConfig.allowedOrigins = listOf("*")
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        
        return CorsWebFilter(source)
    }
}