package com.web3.community.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth")
data class OAuthProperties(
    val google: ProviderProperties = ProviderProperties(),
    val naver: ProviderProperties = ProviderProperties()
) {
    data class ProviderProperties(
        val clientId: String = "",
        val clientSecret: String = ""
    )
}
