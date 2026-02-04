package com.web3community.auth.dto

import com.web3community.user.dto.AuthRole

data class AuthRegistrationRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String? = null,
    val role: AuthRole = AuthRole.USER
)

data class AuthLoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserProfile
)

data class TokenRefreshRequest(
    val refreshToken: String
)

data class TokenResponse(
    val token: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)

data class PasswordResetRequest(
    val email: String
)

data class PasswordResetConfirmation(
    val token: String,
    val newPassword: String,
    val confirmPassword: String
)

data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

data class ProfileUpdateRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val location: String? = null,
    val websiteUrl: String? = null
)

data class RoleChangeRequest(
    val role: AuthRole
)

data class UserProfile(
    val id: Long?,
    val username: String,
    val email: String,
    val displayName: String,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val role: String,
    val isActive: Boolean,
    val isEmailVerified: Boolean,
    val permissions: List<String>,
    val lastLoginAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)