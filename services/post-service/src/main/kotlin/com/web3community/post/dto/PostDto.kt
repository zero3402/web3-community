package com.web3community.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다.")
    val title: String,
    
    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(min = 1, max = 5000, message = "내용은 1~5000자 사이여야 합니다.")
    val content: String,
    
    @field:Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
    val category: String? = null,
    
    val tags: List<String> = emptyList(),
    
    val authorId: Long,
    val authorName: String
)

data class PostUpdateRequest(
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다.")
    val title: String? = null,
    
    @field:Size(min = 1, max = 5000, message = "내용은 1~5000자 사이여야 합니다.")
    val content: String? = null,
    
    @field:Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
    val category: String? = null,
    
    val tags: List<String>? = null
)

data class PostResponse(
    val id: String,
    val title: String,
    val content: String,
    val authorId: Long,
    val authorName: String,
    val category: String?,
    val tags: List<String>,
    val likeCount: Long,
    val viewCount: Long,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)