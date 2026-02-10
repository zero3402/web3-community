package com.web3.community.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreatePostRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title must be less than 200 characters")
    val title: String,

    @field:NotBlank(message = "Content is required")
    val content: String,

    @field:NotBlank(message = "Category ID is required")
    val categoryId: String,

    val tags: List<String> = emptyList()
)
