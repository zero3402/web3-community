package com.web3.community.comment.dto

import jakarta.validation.constraints.NotBlank

data class UpdateCommentRequest(
    @field:NotBlank(message = "Content is required")
    val content: String
)
