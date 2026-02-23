package com.web3.community.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreatePostRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title must be less than 200 characters")
    val title: String,

    @field:NotBlank(message = "Content is required")
    val content: String,

    @field:Positive(message = "Category ID must be positive")
    val categoryId: Long,

    val tags: List<String> = emptyList()
)
