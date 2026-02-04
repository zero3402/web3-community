package com.web3community.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserRegistrationRequest(
    @field:NotBlank(message = "사용자 이름은 필수입니다.")
    @field:Size(min = 3, max = 50, message = "사용자 이름은 3~50자 사이여야 합니다.")
    val username: String,
    
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8~100자 사이여야 합니다.")
    val password: String,
    
    @field:Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
    val displayName: String? = null,
    
    val role: UserRole = UserRole.USER
)

data class UserLoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: String
)

enum class UserRole {
    USER,
    ADMIN
}