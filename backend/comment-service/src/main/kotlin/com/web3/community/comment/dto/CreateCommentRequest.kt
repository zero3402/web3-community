package com.web3.community.comment.dto

import jakarta.validation.constraints.NotBlank

data class CreateCommentRequest(
    @field:NotBlank(message = "Post ID is required")
    val postId: String,

    val parentId: String? = null,

    @field:NotBlank(message = "Content is required")
    val content: String
)
