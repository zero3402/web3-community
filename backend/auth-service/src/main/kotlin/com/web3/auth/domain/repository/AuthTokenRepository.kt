package com.web3community.auth.domain.repository

import com.web3community.auth.domain.model.AuthToken
import com.web3community.auth.domain.model.TokenType
import com.web3community.sharedkernel.domain.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AuthTokenRepository : JpaRepository<AuthToken, Long> {
    
    fun findByToken(token: String): Optional<AuthToken>
    
    fun findByUserIdAndTokenType(userId: UserId, tokenType: TokenType): List<AuthToken>
    
    fun findByUserIdOrderByCreatedAtDesc(userId: UserId): List<AuthToken>
    
    fun findByTokenIdAndIsRevokedFalse(tokenId: Long): Optional<AuthToken>
    
    fun findByTokenAndIsRevokedFalse(token: String): Optional<AuthToken>
    
    fun findByExpiresAtBefore(expiresAt: java.time.LocalDateTime): List<AuthToken>
    
    fun countByUserIdAndTokenTypeAndIsRevokedFalse(userId: UserId, tokenType: TokenType): Long
    
    fun deleteExpiredTokens(expiresAt: java.time.LocalDateTime): Int
    
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(at) FROM AuthToken at 
        WHERE at.userId = :userId 
        AND at.tokenType = :tokenType 
        AND at.isRevoked = false
    """)
    fun countValidTokensByUserIdAndTokenType(
        @org.springframework.data.repository.query.Param("userId") userId: UserId,
        @org.springframework.data.repository.query.Param("tokenType") tokenType: TokenType
    ): Long
}