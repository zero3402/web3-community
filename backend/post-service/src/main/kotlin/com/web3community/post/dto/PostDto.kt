package com.web3community.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다.")
    val title: String,
    
    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(min = 1, max = 10000, message = "내용은 1~10000자 사이여야 합니다.")
    val content: String,
    
    @field:Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
    val category: String? = null,
    
    val tags: List<String> = emptyList(),
    
    @field:Size(max = 255, message = "썸네일 이미지 URL은 255자 이하여야 합니다.")
    val thumbnailImageUrl: String? = null,
    
    val authorId: Long,
    val authorName: String,
    
    val isFeatured: Boolean = false,
    
    val status: PostStatus = PostStatus.PUBLISHED,
    
    val allowComments: Boolean = true
)

data class PostUpdateRequest(
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다.")
    val title: String? = null,
    
    @field:Size(min = 1, max = 10000, message = "내용은 1~10000자 사이여야 합니다.")
    val content: String? = null,
    
    @field:Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
    val category: String? = null,
    
    val tags: List<String>? = null,
    
    @field:Size(max = 255, message = "썸네일 이미지 URL은 255자 이하여야 합니다.")
    val thumbnailImageUrl: String? = null,
    
    val isFeatured: Boolean? = null,
    
    val status: PostStatus? = null,
    
    val allowComments: Boolean? = null
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
    val dislikeCount: Long,
    val commentCount: Long,
    val viewCount: Long,
    val isFeatured: Boolean,
    val status: PostStatus,
    val allowComments: Boolean,
    val thumbnailImageUrl: String?,
    val createdAt: String,
    val updatedAt: String,
    val publishedAt: String?
)

data class PostSearchRequest(
    @field:NotBlank(message = "검색어는 필수입니다.")
    val query: String,
    
    val searchType: PostSearchType = PostSearchType.ALL,
    
    val category: String? = null,
    
    val tags: List<String> = emptyList(),
    
    val authorId: Long? = null,
    
    val status: PostStatus = PostStatus.PUBLISHED,
    
    val page: Int = 0,
    
    val size: Int = 20,
    
    val sortBy: PostSortBy = PostSortBy.CREATED_AT,
    
    val sortDirection: String = "desc"
)

data class PostSearchResponse(
    val posts: List<PostResponse>,
    val totalCount: Long,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

enum class PostStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
    DELETED
}

enum class PostSearchType {
    TITLE,
    CONTENT,
    TAGS,
    AUTHOR,
    ALL
}

enum class PostSortBy {
    CREATED_AT,
    UPDATED_AT,
    PUBLISHED_AT,
    TITLE,
    VIEW_COUNT,
    LIKE_COUNT,
    COMMENT_COUNT,
    AUTHOR_NAME,
    CATEGORY
}