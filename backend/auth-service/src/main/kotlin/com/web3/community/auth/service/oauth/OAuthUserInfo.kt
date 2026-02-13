package com.web3.community.auth.service.oauth

import com.web3.community.auth.entity.AuthProvider

data class OAuthUserInfo(
    val provider: AuthProvider,
    val providerId: String,
    val email: String,
    val nickname: String
)
