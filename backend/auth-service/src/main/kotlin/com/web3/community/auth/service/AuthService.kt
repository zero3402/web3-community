package com.web3.community.auth.service

import com.web3.community.auth.client.UserClient
import com.web3.community.auth.dto.*
import com.web3.community.auth.dto.user.CreateUserRequest
import com.web3.community.auth.dto.user.UserResponse
import com.web3.community.auth.entity.AuthCredential
import com.web3.community.auth.entity.Role
import com.web3.community.auth.repository.AuthCredentialRepository
import com.web3.community.common.dto.ApiResponse
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.common.jwt.JwtProperties
import com.web3.community.common.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

@Service
class AuthService(
        private val authCredentialRepository: AuthCredentialRepository,
        private val refreshTokenService: RefreshTokenService,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jwtProperties: JwtProperties,
        private val passwordEncoder: PasswordEncoder,
        @Qualifier("com.web3.community.auth.client.UserClient") private val userClient: UserClient
) {

    @Transactional
    fun register(request: RegisterRequest): LoginResponse {
        if (authCredentialRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }

        val userProfile = createUserProfile(request.email, request.nickname)

        val credential = AuthCredential(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = Role.USER,
            userId = userProfile.id
        )
        authCredentialRepository.save(credential)

        return generateLoginResponse(credential)
    }

    fun login(request: LoginRequest): LoginResponse {
        val credential = authCredentialRepository.findByEmail(request.email)
            .orElseThrow { BusinessException(ErrorCode.INVALID_CREDENTIALS) }

        if (!credential.enabled) {
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }

        if (!passwordEncoder.matches(request.password, credential.password)) {
            throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        }

        return generateLoginResponse(credential)
    }

    fun refresh(request: TokenRefreshRequest): LoginResponse {
        val refreshToken = request.refreshToken

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw BusinessException(ErrorCode.TOKEN_INVALID)
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        val storedToken = refreshTokenService.getRefreshToken(userId)

        if (storedToken == null || storedToken != refreshToken) {
            throw BusinessException(ErrorCode.TOKEN_INVALID)
        }

        val credential = authCredentialRepository.findByUserId(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        refreshTokenService.deleteRefreshToken(userId)

        return generateLoginResponse(credential)
    }

    fun logout(token: String) {
        val actualToken = token.removePrefix("Bearer ")
        if (jwtTokenProvider.validateToken(actualToken)) {
            val userId = jwtTokenProvider.getUserIdFromToken(actualToken)
            val remainingMs = jwtTokenProvider.getRemainingExpiration(actualToken)
            refreshTokenService.blacklistAccessToken(actualToken, remainingMs)
            refreshTokenService.deleteRefreshToken(userId)
        }
    }

    @Transactional
    fun changePassword(userId: Long, request: PasswordChangeRequest) {
        val credential = authCredentialRepository.findByUserId(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        if (!passwordEncoder.matches(request.currentPassword, credential.password)) {
            throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        }

        credential.password = passwordEncoder.encode(request.newPassword)
        credential.updatedAt = LocalDateTime.now()
        authCredentialRepository.save(credential)
    }

    fun validateToken(token: String): Map<String, Any> {
        val actualToken = token.removePrefix("Bearer ")

        if (!jwtTokenProvider.validateToken(actualToken)) {
            throw BusinessException(ErrorCode.TOKEN_INVALID)
        }

        if (refreshTokenService.isTokenBlacklisted(actualToken)) {
            throw BusinessException(ErrorCode.TOKEN_INVALID)
        }

        return mapOf(
            "userId" to jwtTokenProvider.getUserIdFromToken(actualToken),
            "email" to jwtTokenProvider.getEmailFromToken(actualToken),
            "role" to jwtTokenProvider.getRoleFromToken(actualToken)
        )
    }

    private fun generateLoginResponse(credential: AuthCredential): LoginResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(
            credential.userId, credential.email, credential.role.name
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(credential.userId)

        refreshTokenService.saveRefreshToken(
            credential.userId, refreshToken, jwtProperties.refreshTokenExpiration
        )

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtProperties.accessTokenExpiration,
            userId = credential.userId,
            email = credential.email,
            role = credential.role.name
        )
    }

    private fun createUserProfile(email: String, nickname: String): UserResponse {
        val apiResponse = userClient.createUserProfile(CreateUserRequest(email, nickname))

        if (!apiResponse.success) {
            throw BusinessException(ErrorCode.INTERNAL_ERROR)
        }

        return apiResponse.data ?: throw BusinessException(ErrorCode.INTERNAL_ERROR)
    }
}
