package com.web3.community.common.jwt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Base64

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var properties: JwtProperties

    @BeforeEach
    fun setUp() {
        val rawSecret = "this is a very long secret key for jwt token generation"
        properties = JwtProperties(
            secret = Base64.getEncoder().encodeToString(rawSecret.toByteArray()),
            accessTokenExpiration = 3600000,
            refreshTokenExpiration = 604800000,
            issuer = "test"
        )
        jwtTokenProvider = JwtTokenProvider(properties)
    }

    @Test
    fun `should generate access token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER", "tester")
        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `should generate refresh token`() {
        val token = jwtTokenProvider.generateRefreshToken(1L)
        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `should validate valid token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER", "tester")
        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `should reject invalid token`() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"))
    }

    @Test
    fun `should extract userId from token`() {
        val token = jwtTokenProvider.generateAccessToken(42L, "test@test.com", "USER", "tester")
        assertEquals(42L, jwtTokenProvider.getUserIdFromToken(token))
    }

    @Test
    fun `should extract email from token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER", "tester")
        assertEquals("test@test.com", jwtTokenProvider.getEmailFromToken(token))
    }

    @Test
    fun `should extract role from token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@test.com", "ADMIN", "tester")
        assertEquals("ADMIN", jwtTokenProvider.getRoleFromToken(token))
    }

    @Test
    fun `should extract nickname from token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER", "mynickname")
        assertEquals("mynickname", jwtTokenProvider.getNicknameFromToken(token))
    }

    @Test
    fun `should return positive remaining expiration for fresh token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER", "tester")
        assertTrue(jwtTokenProvider.getRemainingExpiration(token) > 0)
    }

    @Test
    fun `should get expiration date from token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER", "tester")
        val expiration = jwtTokenProvider.getExpirationFromToken(token)
        assertNotNull(expiration)
        assertTrue(expiration.time > System.currentTimeMillis())
    }
}
