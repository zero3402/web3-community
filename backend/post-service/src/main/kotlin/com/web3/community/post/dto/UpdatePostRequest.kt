package com.web3.community.post.dto

import jakarta.validation.constraints.Size

data class UpdatePostRequest(
    @field:Size(max = 200, message = "Title must be less than 200 characters")
    val title: String? = null,

    val content: String? = null,

    val categoryId: String? = null,

    val tags: List<String>? = null
)
