package com.web3community.auth.domain.service

import com.web3community.auth.domain.model.AuthToken
import com.web3community.auth.domain.model.TokenType
import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.UserId
import com.web3community.user.domain.repository.UserRepository
import com.web3community.auth.domain.repository.AuthTokenRepository
import com.web3community.sharedkernel.events.UserDomainEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

// 인증 도메인 서비스
@Service
@Transactional
class AuthDomainService(
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val jwtUtil: com.web3community.util.security.JwtUtil,
    private val passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder
) {

    fun authenticateUser(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(Email.of(email))
            .orElseThrow { DomainObjectNotFoundException("User", email) }
        
        if (!user.password.value.equals(password) || !passwordEncoder.matches(password, user.password.value)) {
            throw PermissionDeniedException("Invalid credentials")
        }
        
        if (!user.isActive) {
            throw InvalidStateException("Account is deactivated")
        }
        
        // JWT 토큰 생성
        val accessToken = jwtUtil.generateToken(user.id.value.toString())
        val refreshToken = jwtUtil.generateRefreshToken(user.id.value.toString())
        
        // 토큰 저장
        val authToken = AuthToken(
            userId = user.id,
            token = accessToken,
            refreshToken = refreshToken,
            tokenType = TokenType.ACCESS,
            expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getExpirationTime() / 1000),
            ipAddress = null, // 컨트롤러에서 설정
            userAgent = null
        )
        
        val savedToken = authTokenRepository.save(authToken)
        
        // 리프레시 토큰 저장
        val refreshAuth = AuthToken(
            userId = user.id,
            token = refreshToken,
            refreshToken = refreshToken,
            tokenType = TokenType.REFRESH,
            expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationTime() / 1000),
            ipAddress = null,
            userAgent = null
        )
        
        authTokenRepository.save(refreshAuth)
        
        // 이벤트 발행
        user.login()
        userRepository.save(user)
        
        return AuthResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtUtil.getExpirationTime(),
            user = user.toAuthUser()
        )
    }

    fun refreshToken(request: RefreshTokenRequest): TokenResponse {
        val refreshToken = request.refreshToken
        
        // 리프레시 토큰 유효성 확인
        val authUser = jwtUtil.validateToken(refreshToken)
        val userId = authUser?.getUserId()
        
        if (userId == null) {
            throw PermissionDeniedException("Invalid refresh token")
        }
        
        val user = userRepository.findById(UserId.of(userId.toLong()))
            .orElseThrow { DomainObjectNotFoundException("User", userId) }
        
        if (!user.isActive) {
            throw InvalidStateException("Account is deactivated")
        }
        
        // 새 토큰 생성
        val newAccessToken = jwtUtil.generateToken(userId.toString())
        val newRefreshToken = jwtUtil.generateRefreshToken(userId.toString())
        
        // 기존 토큰 무효화
        revokeUserTokens(user.id)
        
        // 새 토큰 저장
        val authToken = AuthToken(
            userId = user.id,
            token = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = TokenType.ACCESS,
            expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getExpirationTime() / 1000)
        )
        
        authTokenRepository.save(authToken)
        
        val refreshAuth = AuthToken(
            userId = user.id,
            token = newRefreshToken,
            refreshToken = newRefreshToken,
            tokenType = TokenType.REFRESH,
            expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationTime() / 1000)
        )
        
        authTokenRepository.save(refreshAuth)
        
        return TokenResponse(
            token = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = jwtUtil.getExpirationTime()
        )
    }

    fun revokeToken(token: String): Boolean {
        try {
            val authUser = jwtUtil.validateToken(token)
            val userId = authUser?.getUserId()
            
            if (userId == null) {
                return false
            }
            
            val user = userRepository.findById(UserId.of(userId.toLong()))
                .orElseThrow { DomainObjectNotFoundException("User", userId) }
            
            // 사용자의 모든 토큰 무효화
            revokeUserTokens(user.id)
            
            // 로그아웃 이벤트 발행
            user.logout()
            userRepository.save(user)
            
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun validateToken(token: String): TokenValidationResult {
        try {
            val authUser = jwtUtil.validateToken(token)
            if (authUser == null) {
                return TokenValidationResult(false, "Invalid token")
            }
            
            val tokenId = authUser.getTokenId()
            val savedToken = authTokenRepository.findByToken(token)
                .orElse(null)
            
            if (savedToken == null || !savedToken.isValid()) {
                return TokenValidationResult(false, "Token not found or expired")
            }
            
            return TokenValidationResult(true, "Valid token", savedToken.userId.value.toString())
        } catch (e: Exception) {
            return TokenValidationResult(false, "Token validation failed: ${e.message}")
        }
    }

    fun getUserSessions(userId: Long): List<UserSession> {
        val userTokens = authTokenRepository.findByUserIdOrderByCreatedAtDesc(UserId.of(userId))
        return userTokens.filter { it.isValid() }.map { token ->
            UserSession(
                tokenId = token.id.value,
                tokenType = token.tokenType.name,
                createdAt = token.createdAt.toString(),
                expiresAt = token.expiresAt.toString(),
                ipAddress = token.ipAddress,
                userAgent = token.userAgent,
                isExpired = token.isExpired(),
                isRevoked = token.isRevoked
            )
        }
    }

    private fun revokeUserTokens(userId: UserId) {
        val userTokens = authTokenRepository.findByUserId(UserId.of(userId))
        userTokens.forEach { it.revoke() }
        authTokenRepository.saveAll(userTokens)
    }
}

// 결과 객체
data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: AuthUser
)

data class TokenResponse(
    val token: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)

data class TokenValidationResult(
    val isValid: Boolean,
    val message: String,
    val userId: String? = null
)

data class UserSession(
    val tokenId: Long,
    val tokenType: String,
    val createdAt: String,
    val expiresAt: String,
    val ipAddress: String?,
    val userAgent: String?,
    val isExpired: Boolean,
    val isRevoked: Boolean
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class AuthUser(
    val id: Long,
    val email: String,
    val username: String,
    val displayName: String,
    val role: String,
    val isActive: Boolean
)

// 확장 함수
fun com.web3community.user.domain.model.User.toAuthUser(): AuthUser {
    return AuthUser(
        id = this.id.value,
        email = this.email.value,
        username = this.username.value,
        displayName = this.displayName,
        role = this.role.name,
        isActive = this.isActive
    )
}