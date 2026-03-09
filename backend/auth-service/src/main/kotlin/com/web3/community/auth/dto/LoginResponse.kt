package com.web3.community.auth.dto

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val userId: Long,
    val email: String,
    val role: String
)
