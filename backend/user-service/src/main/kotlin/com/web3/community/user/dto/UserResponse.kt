package com.web3.community.user.dto

import com.web3.community.user.entity.User
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val nickname: String,
    val email: String,
    val bio: String?,
    val profileImageUrl: String?,
    val role: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                nickname = user.nickname,
                email = user.email,
                bio = user.bio,
                profileImageUrl = user.profileImageUrl,
                role = user.role,
                createdAt = user.createdAt
            )
        }
    }
}
