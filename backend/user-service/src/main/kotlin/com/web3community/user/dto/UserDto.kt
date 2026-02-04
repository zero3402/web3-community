package com.web3community.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import com.fasterxml.jackson.annotation.JsonProperty

data class UserRegistrationRequest(
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
    
    @field:Size(max = 500, message = "자기소게는 500자 이하여야 합니다.")
    val bio: String? = null,
    
    @field:Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
    val profileImageUrl: String? = null,
    
    @field:Size(max = 100, message = "위치는 100자 이하여야 합니다.")
    val location: String? = null,
    
    @field:Size(max = 255, message = "웹사이트 URL은 255자 이하여야 합니다.")
    val websiteUrl: String? = null,
    
    val role: UserRole = UserRole.USER
)

data class UserLoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val password: String
)

data class UserUpdateRequest(
    @field:Size(min = 3, max = 50, message = "사용자 이름은 3~50자 사이여야 합니다.")
    val username: String? = null,
    
    @field:Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
    val displayName: String? = null,
    
    @field:Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    val bio: String? = null,
    
    @field:Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
    val profileImageUrl: String? = null,
    
    @field:Size(max = 100, message = "위치는 100자 이하여야 합니다.")
    val location: String? = null,
    
    @field:Size(max = 255, message = "웹사이트 URL은 255자 이하여야 합니다.")
    val websiteUrl: String? = null
)

data class UserPatchRequest(
    @field:Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
    val displayName: String? = null,
    
    @field:Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    val bio: String? = null,
    
    @field:Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
    val profileImageUrl: String? = null,
    
    @field:Size(max = 100, message = "위치는 100자 이하여야 합니다.")
    val location: String? = null,
    
    @field:Size(max = 255, message = "웹사이트 URL은 255자 이하여야 합니다.")
    val websiteUrl: String? = null
)

data class PasswordChangeRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val currentPassword: String,
    
    @field:NotBlank(message = "새 비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 100, message = "새 비밀번호는 8~100자 사이여야 합니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val newPassword: String,
    
    @field:NotBlank(message = "비밀번호 확인은 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val confirmPassword: String
)

data class RoleChangeRequest(
    @field:NotBlank(message = "역할은 필수입니다.")
    val role: UserRole
)

data class LoginResponse(
    val token: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val role: UserRole,
    val isActive: Boolean,
    val isEmailVerified: Boolean,
    val location: String?,
    val websiteUrl: String?,
    val lastLoginAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class UserStats(
    val totalUsers: Long,
    val activeUsers: Long,
    val inactiveUsers: Long,
    val verifiedUsers: Long,
    val unverifiedUsers: Long,
    val newUsersThisMonth: Long,
    val usersByRole: Map<UserRole, Long>
)

enum class UserRole {
    USER,
    MODERATOR,
    ADMIN
}