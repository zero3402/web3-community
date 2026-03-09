package com.web3.community.comment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class CreateCommentRequest(
    @field:Positive(message = "Post ID must be positive")
    val postId: Long,

    val parentId: Long? = null,

    @field:NotBlank(message = "Content is required")
    val content: String
)
