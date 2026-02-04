package com.web3community.post.application.dto

import jakarta.validation.constraints.*

data class CreatePostRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    val title: String,
    
    @field:NotBlank(message = "Content is required")
    val content: String,
    
    @field:NotNull(message = "Category is required")
    val categoryId: Long,
    
    @field:Size(max = 500, message = "Featured image URL must be at most 500 characters")
    val featuredImageUrl: String? = null,
    
    @field:Size(max = 500, message = "Excerpt must be at most 500 characters")
    val excerpt: String? = null,
    
    val tags: List<String>? = null,
    val attachments: List<AttachmentRequest>? = null
)

data class UpdatePostRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    val title: String,
    
    @field:NotBlank(message = "Content is required")
    val content: String,
    
    @field:NotNull(message = "Category is required")
    val categoryId: Long,
    
    @field:Size(max = 500, message = "Featured image URL must be at most 500 characters")
    val featuredImageUrl: String? = null,
    
    @field:Size(max = 500, message = "Excerpt must be at most 500 characters")
    val excerpt: String? = null,
    
    val tags: List<String>? = null,
    val attachments: List<AttachmentRequest>? = null
)

data class AttachmentRequest(
    @field:NotBlank(message = "Filename is required")
    val filename: String,
    
    @field:NotBlank(message = "Original filename is required")
    val originalFilename: String,
    
    @field:NotBlank(message = "MIME type is required")
    val mimeType: String,
    
    @field:NotNull(message = "File size is required")
    @field:Positive(message = "File size must be positive")
    val fileSize: Long,
    
    @field:NotBlank(message = "File path is required")
    val filePath: String,
    
    val thumbnailPath: String? = null,
    val type: String = "FILE",
    val displayOrder: Int = 0
)

data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val authorId: Long,
    val categoryId: Long,
    val featuredImageUrl: String?,
    val excerpt: String?,
    val status: String,
    val isPinned: Boolean,
    val isFeatured: Boolean,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long,
    val tags: List<TagResponse>,
    val attachments: List<AttachmentResponse>,
    val createdAt: String,
    val updatedAt: String
)

data class TagResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String?,
    val postCount: Long
)

data class AttachmentResponse(
    val id: Long,
    val filename: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val filePath: String,
    val thumbnailPath: String?,
    val type: String,
    val displayOrder: Int,
    val createdAt: String
)

data class PostListResponse(
    val posts: List<PostResponse>,
    val total: Long,
    val page: Int,
    val size: Int
)

data class PostSearchRequest(
    @field:Size(min = 1, max = 100, message = "Search query must be between 1 and 100 characters")
    val query: String,
    
    val categoryId: Long? = null,
    val authorId: Long? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val page: Int = 0,
    val size: Int = 20
)