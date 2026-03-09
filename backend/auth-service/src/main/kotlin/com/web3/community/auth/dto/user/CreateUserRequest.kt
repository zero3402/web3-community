package com.web3.community.auth.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Invalid email format")
        val email: String,

        @field:NotBlank(message = "Nickname is required")
        @field:Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
        val nickname: String
)