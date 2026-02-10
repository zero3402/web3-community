package com.web3.community.auth.service

import com.web3.community.auth.dto.*
import com.web3.community.auth.entity.AuthCredential
import com.web3.community.auth.entity.Role
import com.web3.community.auth.repository.AuthCredentialRepository
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.common.jwt.JwtProperties
import com.web3.community.common.jwt.JwtTokenProvider
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.client.RestTemplate
import java.util.*

class AuthServiceTest {

    @MockK
    private lateinit var authCredentialRepository: AuthCredentialRepository

    @MockK
    private lateinit var refreshTokenService: RefreshTokenService

    @MockK
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockK
    private lateinit var jwtProperties: JwtProperties

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @MockK
    private lateinit var restTemplate: RestTemplate

    private lateinit var authService: AuthService

    private val testCredential = AuthCredential(
        id = 1L,
        email = "test@test.com",
        password = "encodedPassword",
        role = Role.USER,
        userId = 1L
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        authService = AuthService(
            authCredentialRepository, refreshTokenService,
            jwtTokenProvider, jwtProperties, passwordEncoder, restTemplate
        )
        every { jwtProperties.accessTokenExpiration } returns 3600000L
        every { jwtProperties.refreshTokenExpiration } returns 604800000L
    }

    @Test
    fun `login should return tokens for valid credentials`() {
        every { authCredentialRepository.findByEmail("test@test.com") } returns Optional.of(testCredential)
        every { passwordEncoder.matches("password", "encodedPassword") } returns true
        every { jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER") } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(1L) } returns "refresh-token"
        every { refreshTokenService.saveRefreshToken(1L, "refresh-token", 604800000L) } just runs

        val request = LoginRequest(email = "test@test.com", password = "password")
        val result = authService.login(request)

        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals(1L, result.userId)
        assertEquals("test@test.com", result.email)
    }

    @Test
    fun `login should throw for invalid credentials`() {
        every { authCredentialRepository.findByEmail("test@test.com") } returns Optional.of(testCredential)
        every { passwordEncoder.matches("wrong", "encodedPassword") } returns false

        val request = LoginRequest(email = "test@test.com", password = "wrong")

        val exception = assertThrows<BusinessException> { authService.login(request) }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    @Test
    fun `login should throw for non-existent email`() {
        every { authCredentialRepository.findByEmail("none@test.com") } returns Optional.empty()

        val request = LoginRequest(email = "none@test.com", password = "password")

        val exception = assertThrows<BusinessException> { authService.login(request) }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    @Test
    fun `login should throw for disabled account`() {
        val disabledCredential = testCredential.copy(enabled = false)
        every { authCredentialRepository.findByEmail("test@test.com") } returns Optional.of(disabledCredential)

        val request = LoginRequest(email = "test@test.com", password = "password")

        val exception = assertThrows<BusinessException> { authService.login(request) }
        assertEquals(ErrorCode.UNAUTHORIZED, exception.errorCode)
    }

    @Test
    fun `logout should blacklist token and delete refresh token`() {
        val token = "Bearer valid-token"
        every { jwtTokenProvider.validateToken("valid-token") } returns true
        every { jwtTokenProvider.getUserIdFromToken("valid-token") } returns 1L
        every { jwtTokenProvider.getRemainingExpiration("valid-token") } returns 1000L
        every { refreshTokenService.blacklistAccessToken("valid-token", 1000L) } just runs
        every { refreshTokenService.deleteRefreshToken(1L) } just runs

        authService.logout(token)

        verify { refreshTokenService.blacklistAccessToken("valid-token", 1000L) }
        verify { refreshTokenService.deleteRefreshToken(1L) }
    }

    @Test
    fun `changePassword should throw for wrong current password`() {
        every { authCredentialRepository.findByUserId(1L) } returns Optional.of(testCredential)
        every { passwordEncoder.matches("wrong", "encodedPassword") } returns false

        val request = PasswordChangeRequest(currentPassword = "wrong", newPassword = "newPass123")

        val exception = assertThrows<BusinessException> { authService.changePassword(1L, request) }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    @Test
    fun `changePassword should update password for correct current password`() {
        every { authCredentialRepository.findByUserId(1L) } returns Optional.of(testCredential)
        every { passwordEncoder.matches("current", "encodedPassword") } returns true
        every { passwordEncoder.encode("newPass123") } returns "newEncodedPassword"
        every { authCredentialRepository.save(any()) } returns testCredential

        val request = PasswordChangeRequest(currentPassword = "current", newPassword = "newPass123")
        authService.changePassword(1L, request)

        verify { authCredentialRepository.save(any()) }
    }

    @Test
    fun `validateToken should return claims for valid non-blacklisted token`() {
        every { jwtTokenProvider.validateToken("valid") } returns true
        every { refreshTokenService.isTokenBlacklisted("valid") } returns false
        every { jwtTokenProvider.getUserIdFromToken("valid") } returns 1L
        every { jwtTokenProvider.getEmailFromToken("valid") } returns "test@test.com"
        every { jwtTokenProvider.getRoleFromToken("valid") } returns "USER"

        val result = authService.validateToken("Bearer valid")

        assertEquals(1L, result["userId"])
        assertEquals("test@test.com", result["email"])
        assertEquals("USER", result["role"])
    }

    @Test
    fun `validateToken should throw for blacklisted token`() {
        every { jwtTokenProvider.validateToken("blacklisted") } returns true
        every { refreshTokenService.isTokenBlacklisted("blacklisted") } returns true

        val exception = assertThrows<BusinessException> { authService.validateToken("Bearer blacklisted") }
        assertEquals(ErrorCode.TOKEN_INVALID, exception.errorCode)
    }
}
