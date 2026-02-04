package com.web3community.user.application.dto

import jakarta.validation.constraints.*

data class RegisterUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @field:Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    val username: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    val password: String,
    
    @field:NotBlank(message = "Display name is required")
    @field:Size(max = 100, message = "Display name must be at most 100 characters")
    val displayName: String,
    
    val role: com.web3community.user.domain.model.UserRole = com.web3community.user.domain.model.UserRole.USER
)

data class LoginUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    val password: String
)

data class UpdateUserProfileRequest(
    @field:NotBlank(message = "Display name is required")
    @field:Size(max = 100, message = "Display name must be at most 100 characters")
    val displayName: String,
    
    @field:Size(max = 500, message = "Bio must be at most 500 characters")
    val bio: String? = null,
    
    @field:Size(max = 500, message = "Profile image URL must be at most 500 characters")
    val profileImageUrl: String? = null,
    
    @field:Size(max = 100, message = "Location must be at most 100 characters")
    val location: String? = null,
    
    @field:Size(max = 500, message = "Website URL must be at most 500 characters")
    @field:Pattern(regexp = "^https?://.*", message = "Website URL must be a valid URL")
    val websiteUrl: String? = null
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "New password must be at least 8 characters")
    @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "New password must contain at least one uppercase letter, one lowercase letter, and one digit")
    val newPassword: String
)

data class UserResponse(
    val id: Long,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val profileImageUrl: String?,
    val location: String?,
    val websiteUrl: String?,
    val role: String,
    val isActive: Boolean,
    val isEmailVerified: Boolean,
    val lastLoginAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class UserProfileResponse(
    val id: Long,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val profileImageUrl: String?,
    val location: String?,
    val websiteUrl: String?,
    val role: String,
    val isActive: Boolean,
    val isEmailVerified: Boolean,
    val lastLoginAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val followersCount: Long,
    val followingCount: Long
)

data class LoginResponse(
    val user: UserResponse,
    val token: String
)

data class FollowResponse(
    val message: String,
    val isFollowing: Boolean
)

data class UserListResponse(
    val users: List<UserResponse>,
    val total: Long,
    val page: Int,
    val size: Int
)