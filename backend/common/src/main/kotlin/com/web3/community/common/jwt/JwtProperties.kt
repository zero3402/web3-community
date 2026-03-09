package com.web3.community.common.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String = "",
    val accessTokenExpiration: Long = 3600000,
    val refreshTokenExpiration: Long = 604800000,
    val issuer: String = "web3-community"
)
