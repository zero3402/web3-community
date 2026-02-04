package com.web3community.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CategoryCreateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    val name: String,
    
    @field:NotBlank(message = "Slug is required")
    @field:Size(min = 2, max = 50, message = "Slug must be between 2 and 50 characters")
    val slug: String,
    
    val description: String? = null,
    val parentId: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

data class CategoryUpdateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    val name: String,
    
    @field:NotBlank(message = "Slug is required")
    @field:Size(min = 2, max = 50, message = "Slug must be between 2 and 50 characters")
    val slug: String,
    
    val description: String? = null,
    val parentId: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String? = null,
    val parentId: Long? = null,
    val parentName: String? = null,
    val displayOrder: Int,
    val isActive: Boolean,
    val postCount: Long,
    val createdAt: String,
    val updatedAt: String,
    val children: List<CategoryResponse> = emptyList()
)

data class CategoryListResponse(
    val categories: List<CategoryResponse>,
    val pagination: PaginationResponse? = null
)

data class TagCreateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters")
    val name: String,
    
    @field:NotBlank(message = "Slug is required")
    @field:Size(min = 2, max = 30, message = "Slug must be between 2 and 30 characters")
    val slug: String,
    
    val description: String? = null
)

data class TagUpdateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters")
    val name: String,
    
    @field:NotBlank(message = "Slug is required")
    @field:Size(min = 2, max = 30, message = "Slug must be between 2 and 30 characters")
    val slug: String,
    
    val description: String? = null
)

data class TagResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String? = null,
    val postCount: Long,
    val createdAt: String,
    val updatedAt: String
)

data class TagListResponse(
    val tags: List<TagResponse>,
    val pagination: PaginationResponse? = null
)