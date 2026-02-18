package com.web3.community.auth.service

import com.web3.community.auth.client.UserClient
import com.web3.community.auth.dto.*
import com.web3.community.auth.dto.user.CreateUserRequest
import com.web3.community.auth.dto.user.UserResponse
import com.web3.community.auth.entity.AuthCredential
import com.web3.community.auth.entity.AuthProvider
import com.web3.community.auth.entity.Role
import com.web3.community.auth.repository.AuthCredentialRepository
import com.web3.community.auth.service.oauth.GoogleOAuthClient
import com.web3.community.auth.service.oauth.NaverOAuthClient
import com.web3.community.auth.service.oauth.OAuthUserInfo
import com.web3.community.common.dto.ApiResponse
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
import java.time.LocalDateTime
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
    private lateinit var userClient: UserClient

    @MockK
    private lateinit var googleOAuthClient: GoogleOAuthClient

    @MockK
    private lateinit var naverOAuthClient: NaverOAuthClient

    private lateinit var authService: AuthService

    private val testCredential = AuthCredential(
        id = 1L,
        email = "test@test.com",
        nickname = "tester",
        password = "encodedPassword",
        role = Role.USER,
        userId = 1L,
        provider = AuthProvider.LOCAL
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        authService = AuthService(
            authCredentialRepository, refreshTokenService,
            jwtTokenProvider, jwtProperties, passwordEncoder,
            userClient, googleOAuthClient, naverOAuthClient
        )
        every { jwtProperties.accessTokenExpiration } returns 3600000L
        every { jwtProperties.refreshTokenExpiration } returns 604800000L
    }

    @Test
    fun `login should return tokens for valid credentials`() {
        every { authCredentialRepository.findByEmail("test@test.com") } returns Optional.of(testCredential)
        every { passwordEncoder.matches("password", "encodedPassword") } returns true
        every { jwtTokenProvider.generateAccessToken(1L, "test@test.com", "USER", "tester") } returns "access-token"
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
    fun `login should throw SOCIAL_LOGIN_ONLY for social account`() {
        val socialCredential = testCredential.copy(provider = AuthProvider.GOOGLE, password = null)
        every { authCredentialRepository.findByEmail("test@test.com") } returns Optional.of(socialCredential)

        val request = LoginRequest(email = "test@test.com", password = "password")

        val exception = assertThrows<BusinessException> { authService.login(request) }
        assertEquals(ErrorCode.SOCIAL_LOGIN_ONLY, exception.errorCode)
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
    fun `changePassword should throw SOCIAL_LOGIN_ONLY for social account`() {
        val socialCredential = testCredential.copy(provider = AuthProvider.GOOGLE, password = null)
        every { authCredentialRepository.findByUserId(1L) } returns Optional.of(socialCredential)

        val request = PasswordChangeRequest(currentPassword = "current", newPassword = "newPass123")

        val exception = assertThrows<BusinessException> { authService.changePassword(1L, request) }
        assertEquals(ErrorCode.SOCIAL_LOGIN_ONLY, exception.errorCode)
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

    // Social Login Tests

    @Test
    fun `socialLogin should create new account for first-time Google user`() {
        val oAuthUserInfo = OAuthUserInfo(
            provider = AuthProvider.GOOGLE,
            providerId = "google-123",
            email = "google@gmail.com",
            nickname = "Google User"
        )
        val newCredential = AuthCredential(
            id = 2L, email = "google@gmail.com", nickname = "Google User", password = null,
            role = Role.USER, userId = 2L, provider = AuthProvider.GOOGLE, providerId = "google-123"
        )

        every { googleOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback") } returns oAuthUserInfo
        every { authCredentialRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123") } returns Optional.empty()
        every { userClient.createUserProfile(CreateUserRequest("google@gmail.com", "Google User")) } returns
                ApiResponse.success(UserResponse(2L, "Google User", "google@gmail.com", null, null, "USER", LocalDateTime.now()))
        every { authCredentialRepository.save(any()) } returns newCredential
        every { jwtTokenProvider.generateAccessToken(2L, "google@gmail.com", "USER", "Google User") } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(2L) } returns "refresh-token"
        every { refreshTokenService.saveRefreshToken(2L, "refresh-token", 604800000L) } just runs

        val request = OAuthLoginRequest(provider = "GOOGLE", code = "auth-code", redirectUri = "http://localhost:5173/callback")
        val result = authService.socialLogin(request)

        assertEquals("access-token", result.accessToken)
        assertEquals(2L, result.userId)
        assertEquals("google@gmail.com", result.email)
        verify { userClient.createUserProfile(any()) }
    }

    @Test
    fun `socialLogin should return tokens for existing Google user`() {
        val existingCredential = AuthCredential(
            id = 2L, email = "google@gmail.com", nickname = "Google User", password = null,
            role = Role.USER, userId = 2L, provider = AuthProvider.GOOGLE, providerId = "google-123"
        )
        val oAuthUserInfo = OAuthUserInfo(
            provider = AuthProvider.GOOGLE,
            providerId = "google-123",
            email = "google@gmail.com",
            nickname = "Google User"
        )

        every { googleOAuthClient.getUserInfo("auth-code", "http://localhost:5173/callback") } returns oAuthUserInfo
        every { authCredentialRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123") } returns Optional.of(existingCredential)
        every { jwtTokenProvider.generateAccessToken(2L, "google@gmail.com", "USER", "Google User") } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(2L) } returns "refresh-token"
        every { refreshTokenService.saveRefreshToken(2L, "refresh-token", 604800000L) } just runs

        val request = OAuthLoginRequest(provider = "GOOGLE", code = "auth-code", redirectUri = "http://localhost:5173/callback")
        val result = authService.socialLogin(request)

        assertEquals("access-token", result.accessToken)
        assertEquals(2L, result.userId)
        verify(exactly = 0) { userClient.createUserProfile(any()) }
    }

    @Test
    fun `socialLogin should work with Naver provider`() {
        val oAuthUserInfo = OAuthUserInfo(
            provider = AuthProvider.NAVER,
            providerId = "naver-456",
            email = "naver@naver.com",
            nickname = "Naver User"
        )
        val newCredential = AuthCredential(
            id = 3L, email = "naver@naver.com", nickname = "Naver User", password = null,
            role = Role.USER, userId = 3L, provider = AuthProvider.NAVER, providerId = "naver-456"
        )

        every { naverOAuthClient.getUserInfo("naver-code", "http://localhost:5173/callback") } returns oAuthUserInfo
        every { authCredentialRepository.findByProviderAndProviderId(AuthProvider.NAVER, "naver-456") } returns Optional.empty()
        every { userClient.createUserProfile(CreateUserRequest("naver@naver.com", "Naver User")) } returns
                ApiResponse.success(UserResponse(3L, "Naver User", "naver@naver.com", null, null, "USER", LocalDateTime.now()))
        every { authCredentialRepository.save(any()) } returns newCredential
        every { jwtTokenProvider.generateAccessToken(3L, "naver@naver.com", "USER", "Naver User") } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(3L) } returns "refresh-token"
        every { refreshTokenService.saveRefreshToken(3L, "refresh-token", 604800000L) } just runs

        val request = OAuthLoginRequest(provider = "NAVER", code = "naver-code", redirectUri = "http://localhost:5173/callback")
        val result = authService.socialLogin(request)

        assertEquals("access-token", result.accessToken)
        assertEquals(3L, result.userId)
        assertEquals("naver@naver.com", result.email)
    }

    @Test
    fun `socialLogin should throw for invalid provider`() {
        val request = OAuthLoginRequest(provider = "KAKAO", code = "code", redirectUri = "http://localhost:5173/callback")

        val exception = assertThrows<BusinessException> { authService.socialLogin(request) }
        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `socialLogin should throw for LOCAL provider`() {
        val request = OAuthLoginRequest(provider = "LOCAL", code = "code", redirectUri = "http://localhost:5173/callback")

        val exception = assertThrows<BusinessException> { authService.socialLogin(request) }
        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }
}
