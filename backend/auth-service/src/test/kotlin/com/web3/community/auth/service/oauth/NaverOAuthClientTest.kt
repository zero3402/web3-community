package com.web3.community.auth.service.oauth

import com.web3.community.auth.config.OAuthProperties
import com.web3.community.auth.entity.AuthProvider
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class NaverOAuthClientTest {

    private val restTemplate = mockk<RestTemplate>()
    private val oAuthProperties = OAuthProperties(
        google = OAuthProperties.ProviderProperties(),
        naver = OAuthProperties.ProviderProperties(clientId = "naver-client-id", clientSecret = "naver-secret")
    )
    private lateinit var naverOAuthClient: NaverOAuthClient

    @BeforeEach
    fun setUp() {
        naverOAuthClient = NaverOAuthClient(oAuthProperties, restTemplate)
    }

    @Test
    fun `getUserInfo should return user info for valid code`() {
        val tokenResponse = mapOf("access_token" to "naver-access-token")
        val userInfoResponse = mapOf(
            "response" to mapOf(
                "id" to "naver-12345",
                "email" to "user@naver.com",
                "nickname" to "NaverUser"
            )
        )

        every {
            restTemplate.exchange(
                eq("https://nid.naver.com/oauth2.0/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(tokenResponse, HttpStatus.OK)

        every {
            restTemplate.exchange(
                eq("https://openapi.naver.com/v1/nid/me"),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(userInfoResponse, HttpStatus.OK)

        val result = naverOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback")

        assertEquals(AuthProvider.NAVER, result.provider)
        assertEquals("naver-12345", result.providerId)
        assertEquals("user@naver.com", result.email)
        assertEquals("NaverUser", result.nickname)
    }

    @Test
    fun `getUserInfo should throw when token exchange fails`() {
        every {
            restTemplate.exchange(
                eq("https://nid.naver.com/oauth2.0/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } throws RestClientException("Connection refused")

        val exception = assertThrows<BusinessException> {
            naverOAuthClient.getUserInfo("bad-code", "http://localhost:5173/callback")
        }
        assertEquals(ErrorCode.OAUTH_AUTHENTICATION_FAILED, exception.errorCode)
    }

    @Test
    fun `getUserInfo should throw when user info fetch fails`() {
        val tokenResponse = mapOf("access_token" to "naver-access-token")

        every {
            restTemplate.exchange(
                eq("https://nid.naver.com/oauth2.0/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(tokenResponse, HttpStatus.OK)

        every {
            restTemplate.exchange(
                eq("https://openapi.naver.com/v1/nid/me"),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } throws RestClientException("Unauthorized")

        val exception = assertThrows<BusinessException> {
            naverOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback")
        }
        assertEquals(ErrorCode.OAUTH_AUTHENTICATION_FAILED, exception.errorCode)
    }

    @Test
    fun `getUserInfo should fallback to name when nickname is missing`() {
        val tokenResponse = mapOf("access_token" to "naver-access-token")
        val userInfoResponse = mapOf(
            "response" to mapOf(
                "id" to "naver-12345",
                "email" to "user@naver.com",
                "name" to "Real Name"
            )
        )

        every {
            restTemplate.exchange(
                eq("https://nid.naver.com/oauth2.0/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(tokenResponse, HttpStatus.OK)

        every {
            restTemplate.exchange(
                eq("https://openapi.naver.com/v1/nid/me"),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(userInfoResponse, HttpStatus.OK)

        val result = naverOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback")

        assertEquals("Real Name", result.nickname)
    }

    @Test
    fun `getUserInfo should use email prefix when both nickname and name are missing`() {
        val tokenResponse = mapOf("access_token" to "naver-access-token")
        val userInfoResponse = mapOf(
            "response" to mapOf(
                "id" to "naver-12345",
                "email" to "user@naver.com"
            )
        )

        every {
            restTemplate.exchange(
                eq("https://nid.naver.com/oauth2.0/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(tokenResponse, HttpStatus.OK)

        every {
            restTemplate.exchange(
                eq("https://openapi.naver.com/v1/nid/me"),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(userInfoResponse, HttpStatus.OK)

        val result = naverOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback")

        assertEquals("user", result.nickname)
    }
}
