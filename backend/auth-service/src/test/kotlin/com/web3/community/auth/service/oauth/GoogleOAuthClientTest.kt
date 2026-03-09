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

class GoogleOAuthClientTest {

    private val restTemplate = mockk<RestTemplate>()
    private val oAuthProperties = OAuthProperties(
        google = OAuthProperties.ProviderProperties(clientId = "google-client-id", clientSecret = "google-secret"),
        naver = OAuthProperties.ProviderProperties()
    )
    private lateinit var googleOAuthClient: GoogleOAuthClient

    @BeforeEach
    fun setUp() {
        googleOAuthClient = GoogleOAuthClient(oAuthProperties, restTemplate)
    }

    @Test
    fun `getUserInfo should return user info for valid code`() {
        val tokenResponse = mapOf("access_token" to "google-access-token")
        val userInfoResponse = mapOf(
            "id" to "12345",
            "email" to "user@gmail.com",
            "name" to "Test User"
        )

        every {
            restTemplate.exchange(
                eq("https://oauth2.googleapis.com/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(tokenResponse, HttpStatus.OK)

        every {
            restTemplate.exchange(
                eq("https://www.googleapis.com/oauth2/v2/userinfo"),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(userInfoResponse, HttpStatus.OK)

        val result = googleOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback")

        assertEquals(AuthProvider.GOOGLE, result.provider)
        assertEquals("12345", result.providerId)
        assertEquals("user@gmail.com", result.email)
        assertEquals("Test User", result.nickname)
    }

    @Test
    fun `getUserInfo should throw when token exchange fails`() {
        every {
            restTemplate.exchange(
                eq("https://oauth2.googleapis.com/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } throws RestClientException("Connection refused")

        val exception = assertThrows<BusinessException> {
            googleOAuthClient.getUserInfo("bad-code", "http://localhost:5173/callback")
        }
        assertEquals(ErrorCode.OAUTH_AUTHENTICATION_FAILED, exception.errorCode)
    }

    @Test
    fun `getUserInfo should throw when user info fetch fails`() {
        val tokenResponse = mapOf("access_token" to "google-access-token")

        every {
            restTemplate.exchange(
                eq("https://oauth2.googleapis.com/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(tokenResponse, HttpStatus.OK)

        every {
            restTemplate.exchange(
                eq("https://www.googleapis.com/oauth2/v2/userinfo"),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } throws RestClientException("Unauthorized")

        val exception = assertThrows<BusinessException> {
            googleOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback")
        }
        assertEquals(ErrorCode.OAUTH_AUTHENTICATION_FAILED, exception.errorCode)
    }

    @Test
    fun `getUserInfo should use email prefix as nickname when name is missing`() {
        val tokenResponse = mapOf("access_token" to "google-access-token")
        val userInfoResponse = mapOf(
            "id" to "12345",
            "email" to "user@gmail.com"
        )

        every {
            restTemplate.exchange(
                eq("https://oauth2.googleapis.com/token"),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(tokenResponse, HttpStatus.OK)

        every {
            restTemplate.exchange(
                eq("https://www.googleapis.com/oauth2/v2/userinfo"),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(Map::class.java)
            )
        } returns ResponseEntity(userInfoResponse, HttpStatus.OK)

        val result = googleOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback")

        assertEquals("user", result.nickname)
    }
}
