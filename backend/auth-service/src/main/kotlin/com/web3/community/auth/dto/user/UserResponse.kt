package com.web3.community.auth.dto.user

import java.time.LocalDateTime

data class UserResponse(
        val id: Long,
        val nickname: String,
        val email: String,
        val bio: String?,
        val profileImageUrl: String?,
        val role: String,
        val createdAt: LocalDateTime
)
