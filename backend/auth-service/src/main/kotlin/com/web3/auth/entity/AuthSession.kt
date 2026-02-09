package com.web3community.auth.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("auth_sessions")
data class AuthSession(
    @Id val id: Long? = null,
    val userId: Long,
    val token: String,
    val refreshToken: String,
    val expiresAt: LocalDateTime,
    val isActive: Boolean = true,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)