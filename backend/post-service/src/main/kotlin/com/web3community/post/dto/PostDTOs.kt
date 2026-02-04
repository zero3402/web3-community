package com.web3community.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostCreateRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    val title: String,
    
    @field:NotBlank(message = "Content is required")
    val content: String,
    
    @field:NotBlank(message = "Category is required")
    val categoryId: String,
    
    val featuredImageUrl: String? = null,
    val excerpt: String? = null,
    val status: PostStatus = PostStatus.DRAFT,
    val isPinned: Boolean = false,
    val isFeatured: Boolean = false,
    val tags: List<String> = emptyList(),
    val attachments: List<PostAttachmentRequest> = emptyList()
)

data class PostUpdateRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    val title: String,
    
    @field:NotBlank(message = "Content is required")
    val content: String,
    
    @field:NotBlank(message = "Category is required")
    val categoryId: String,
    
    val featuredImageUrl: String? = null,
    val excerpt: String? = null,
    val status: PostStatus = PostStatus.DRAFT,
    val isPinned: Boolean = false,
    val isFeatured: Boolean = false,
    val tags: List<String> = emptyList(),
    val attachments: List<PostAttachmentRequest> = emptyList()
)

data class PostAttachmentRequest(
    val filename: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val filePath: String,
    val thumbnailPath: String? = null,
    val type: AttachmentType = AttachmentType.FILE,
    val displayOrder: Int = 0
)

data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val authorId: Long,
    val authorName: String? = null,
    val categoryId: Long,
    val categoryName: String? = null,
    val categorySlug: String? = null,
    val featuredImageUrl: String? = null,
    val excerpt: String? = null,
    val status: String,
    val isPinned: Boolean,
    val isFeatured: Boolean,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long,
    val publishedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val tags: List<TagResponse> = emptyList(),
    val attachments: List<AttachmentResponse> = emptyList(),
    val metrics: Map<String, Long> = emptyMap()
)

data class TagResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String? = null,
    val postCount: Long
)

data class AttachmentResponse(
    val id: Long,
    val filename: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val filePath: String,
    val thumbnailPath: String? = null,
    val type: String,
    val displayOrder: Int,
    val createdAt: String
)

data class PostSearchRequest(
    val query: String? = null,
    val categoryId: String? = null,
    val authorId: String? = null,
    val status: PostStatus? = null,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean? = null,
    val isFeatured: Boolean? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val sortBy: PostSortBy = PostSortBy.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC
)

data class PostListResponse(
    val posts: List<PostResponse>,
    val pagination: PaginationResponse
)

data class PaginationResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

enum class PostStatus {
    DRAFT, PUBLISHED, ARCHIVED, DELETED
}

enum class PostSortBy {
    CREATED_AT, UPDATED_AT, PUBLISHED_AT, TITLE, VIEW_COUNT, LIKE_COUNT, COMMENT_COUNT
}

enum class SortDirection {
    ASC, DESC
}

enum class AttachmentType {
    IMAGE, VIDEO, DOCUMENT, FILE, THUMBNAIL
}