package com.web3.community.auth.dto

import jakarta.validation.constraints.NotBlank

data class TokenRefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)
