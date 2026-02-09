package com.web3community.auth.domain.model

import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.domain.Password
import com.web3community.sharedkernel.domain.Email
import com.web3community.sharedkernel.events.UserDomainEvent
import jakarta.persistence.*

// 인증 토큰 애그리게이트 루트
@Entity
@Table(name = "auth_tokens")
class AuthToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UserId,
        private set

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "VARCHAR(500)")
    var token: String,
        private set

    @Column(name = "refresh_token", nullable = false, columnDefinition = "VARCHAR(500)")
    var refreshToken: String,
        private set

    @Column(name = "token_type", nullable = false, columnDefinition = "ENUM('ACCESS', 'REFRESH')")
    var tokenType: TokenType = TokenType.ACCESS,
        private set

    @Column(name = "expires_at", nullable = false)
    val expiresAt: java.time.LocalDateTime,
        private set

    @Column(name = "is_revoked", nullable = false)
    var isRevoked: Boolean = false,
        private set

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
        private set

    @Column(name = "ip_address", columnDefinition = "VARCHAR(45)")
    val ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null

) {
    
    // JPA 생성자
    protected constructor() : super()
    
    // 도메인 생성자
    constructor(
        userId: UserId,
        token: String,
        refreshToken: String,
        tokenType: TokenType,
        expiresAt: java.time.LocalDateTime,
        ipAddress: String? = null,
        userAgent: String? = null
    ) : this() {
        this.userId = userId
        this.token = token
        this.refreshToken = refreshToken
        this.tokenType = tokenType
        this.expiresAt = expiresAt
    }
    
    // 비즈니스 메소드
    fun revoke() {
        isRevoked = true
        addDomainEvent(UserDomainEvent.UserLoggedOut(userId.value.toString()))
    }
    
    fun isExpired(): Boolean {
        return java.time.LocalDateTime.now().isAfter(expiresAt)
    }
    
    fun isValid(): Boolean {
        return !isRevoked && !isExpired()
    }
    
    fun isAccessToken(): Boolean {
        return tokenType == TokenType.ACCESS
    }
    
    fun isRefreshToken(): Boolean {
        return tokenType == TokenType.REFRESH
    }
}

// 토큰 타입 Enum
enum class TokenType {
    ACCESS,
    REFRESH
}