package com.web3.community.gateway.config

import com.web3.community.common.jwt.JwtProperties
import com.web3.community.common.jwt.JwtTokenProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class GatewayConfig {

    @Bean
    fun jwtTokenProvider(properties: JwtProperties): JwtTokenProvider {
        return JwtTokenProvider(properties)
    }
}
