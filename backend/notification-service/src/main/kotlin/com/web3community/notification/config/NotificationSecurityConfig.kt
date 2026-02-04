package com.web3community.notification.config

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
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class NotificationSecurityConfig(
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
                    // Public endpoints
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers("/topic/**").permitAll()
                    .requestMatchers("/queue/**").permitAll()
                    // User-specific endpoints require authentication
                    .requestMatchers("/notifications/my/**").authenticated()
                    .requestMatchers("/notifications/**").authenticated()
                    // Admin endpoints
                    .requestMatchers("/notifications/system/**").hasRole("ADMIN")
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