package com.web3community.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import com.fasterxml.jackson.annotation.JsonProperty

data class AuthRegistrationRequest(
    @field:NotBlank(message = "사용자 이름은 필수입니다.")
    @field:Size(min = 3, max = 50, message = "사용자 이름은 3~50자 사이여야 합니다.")
    val username: String,
    
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8~100자 사이여야 합니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val password: String,
    
    @field:Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
    val displayName: String? = null,
    
    val role: com.web3community.auth.dto.AuthRole = com.web3community.auth.dto.AuthRole.USER
)

data class AuthLoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val password: String
)

data class TokenRefreshRequest(
    @field:NotBlank(message = "리프래시 토큰은 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val refreshToken: String
)

data class TokenResponse(
    val token: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long
    val refreshTokenExpiresIn: Long? = null
)

data class AuthResponse(
    val token: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserProfile
)

data class UserProfile(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val role: String,
    val isActive: Boolean,
    val permissions: List<String>,
    val lastLoginAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class RoleChangeRequest(
    @field:NotBlank(message = "역할은 필수입니다.")
    val role: com.web3community.auth.dto.AuthRole
    val reason: String? = null
)

data class PasswordResetRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String
)

data class PasswordResetConfirmation(
    @field:NotBlank(message = "토큰은 필수입니다.")
    @field:Size(min = 6, max = 100, message = "토큰은 6~100자 사이여야 합니다.")
    val token: String
)

data class PasswordResetRequest(
    @field:NotBlank(message = "새 비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 100, message = "새 비밀번호는 8~100자 사이여야 합니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val newPassword: String,
    @field:NotBlank(message = "비밀번호 확인은 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val confirmPassword: String
)

enum class AuthRole {
    USER,
    MODERATOR,
    ADMIN
}