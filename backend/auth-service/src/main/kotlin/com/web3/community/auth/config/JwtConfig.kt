package com.web3.community.auth.config

import com.web3.community.common.jwt.JwtProperties
import com.web3.community.common.jwt.JwtTokenProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(JwtProperties::class, OAuthProperties::class)
class JwtConfig {

    @Bean
    fun jwtTokenProvider(properties: JwtProperties): JwtTokenProvider {
        return JwtTokenProvider(properties)
    }

    @Bean
    fun restTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5000)
            setReadTimeout(5000)
        }
        return RestTemplate(factory)
    }
}
