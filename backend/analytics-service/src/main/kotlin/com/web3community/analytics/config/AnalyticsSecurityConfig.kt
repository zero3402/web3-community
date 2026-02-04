package com.web3community.analytics.config

import com.web3community.util.property.JwtProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import com.web3community.util.security.JwtAuthenticationFilter
import com.web3community.util.security.JwtUtil
import java.util.*

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableJpaAuditing(auditorProviderRef = "auditorProvider")
class AnalyticsSecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtUtil: JwtUtil,
    private val jwtProperties: JwtProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authz ->
                authz
                    // Public endpoints for event tracking
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/analytics/events").permitAll()
                    .requestMatchers("/analytics/events/bulk").permitAll()
                    // User-specific endpoints require authentication
                    .requestMatchers("/analytics/my/**").authenticated()
                    // Admin endpoints
                    .requestMatchers("/analytics/dashboard/**").hasRole("ADMIN")
                    .requestMatchers("/analytics/user/**").hasRole("ADMIN")
                    .requestMatchers("/analytics/events/search").hasRole("ADMIN")
                    .requestMatchers("/analytics/summary/**").hasRole("ADMIN")
                    .requestMatchers("/analytics/export/**").hasRole("ADMIN")
                    // All other requests need authentication
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun auditorProvider(): AuditorAware<Long> {
        return AuditorAware { 
            Optional.ofNullable(jwtUtil.getCurrentUserId())
        }
    }
}