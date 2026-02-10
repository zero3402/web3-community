package com.web3.community.user.dto

import jakarta.validation.constraints.Size

data class UpdateUserRequest(
    @field:Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    val nickname: String? = null,

    @field:Size(max = 500, message = "Bio must be less than 500 characters")
    val bio: String? = null,

    val profileImageUrl: String? = null
)
