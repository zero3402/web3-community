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
class NaverOAuthClient(
    private val oAuthProperties: OAuthProperties,
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOKEN_URL = "https://nid.naver.com/oauth2.0/token"
        private const val USER_INFO_URL = "https://openapi.naver.com/v1/nid/me"
    }

    fun getUserInfo(code: String, redirectUri: String): OAuthUserInfo {
        val accessToken = exchangeCodeForToken(code, redirectUri)
        return fetchUserInfo(accessToken)
    }

    private fun exchangeCodeForToken(code: String, redirectUri: String): String {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", oAuthProperties.naver.clientId)
            add("client_secret", oAuthProperties.naver.clientSecret)
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
            log.error("Naver token exchange failed: {}", e.message)
            throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }
    }

    @Suppress("UNCHECKED_CAST")
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
            val naverResponse = body["response"] as? Map<String, Any>
                ?: throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)

            val id = naverResponse["id"] as? String
                ?: throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
            val email = naverResponse["email"] as? String
                ?: throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
            val nickname = naverResponse["nickname"] as? String
                ?: naverResponse["name"] as? String
                ?: email.substringBefore("@")

            return OAuthUserInfo(
                provider = AuthProvider.NAVER,
                providerId = id,
                email = email,
                nickname = nickname
            )
        } catch (e: RestClientException) {
            log.error("Naver user info fetch failed: {}", e.message)
            throw BusinessException(ErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }
    }
}
