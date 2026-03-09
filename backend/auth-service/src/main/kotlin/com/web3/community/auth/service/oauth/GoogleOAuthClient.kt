package com.web3.community.auth.service.oauth

import com.web3.community.auth.config.OAuthProperties
import com.web3.community.auth.entity.AuthProvider
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Component
class GoogleOAuthClient(
    private val oAuthProperties: OAuthProperties,
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
    }

    fun getUserInfo(code: String, redirectUri: String): OAuthUserInfo {
        val accessToken = exchangeCodeForToken(code, redirectUri)
        return fetchUserInfo(accessToken)
    }

    private fun exchangeCodeForToken(code: String, redirectUri: String): String {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", oAuthProperties.google.clientId)
            add("client_secret", oAuthProperties.google.clientSecret)
            add("code", code)
            add("redirect_uri", redirectUri)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        try {
            val response = restTemplate.exchange(
                TOKEN_URL,
                HttpMethod.POST,
                HttpEntity(params, headers),
                Map::class.java
            )

            return response.body?.get("access_token") as? String
                ?: throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
        } catch (e: RestClientException) {
            log.error("Google token exchange failed: {}", e.message)
            throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }
    }

    private fun fetchUserInfo(accessToken: String): OAuthUserInfo {
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
        }

        try {
            val response = restTemplate.exchange(
                USER_INFO_URL,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                Map::class.java
            )

            val body = response.body ?: throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)

            val id = body["id"] as? String
                ?: throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
            val email = body["email"] as? String
                ?: throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
            val name = body["name"] as? String ?: email.substringBefore("@")

            return OAuthUserInfo(
                provider = AuthProvider.GOOGLE,
                providerId = id,
                email = email,
                nickname = name
            )
        } catch (e: RestClientException) {
            log.error("Google user info fetch failed: {}", e.message)
            throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }
    }
}
