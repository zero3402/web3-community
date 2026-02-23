package com.web3.community.post.dto

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class UpdatePostRequest(
    @field:Size(max = 200, message = "Title must be less than 200 characters")
    val title: String? = null,

    val content: String? = null,

    @field:Positive(message = "Category ID must be positive")
    val categoryId: Long? = null,

    val tags: List<String>? = null
)
