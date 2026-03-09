package com.web3.community.auth.dto

import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:NotBlank
    val provider: String,

    @field:NotBlank
    val code: String,

    @field:NotBlank
    val redirectUri: String
)
