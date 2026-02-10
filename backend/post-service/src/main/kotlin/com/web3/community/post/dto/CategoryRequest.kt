package com.web3.community.post.dto

import com.web3.community.post.document.Category
import jakarta.validation.constraints.NotBlank

data class CategoryRequest(
    @field:NotBlank(message = "Category name is required")
    val name: String,
    val description: String? = null,
    val displayOrder: Int = 0
)

data class CategoryResponse(
    val id: String,
    val name: String,
    val description: String?,
    val displayOrder: Int
) {
    companion object {
        fun from(category: Category): CategoryResponse {
            return CategoryResponse(
                id = category.id!!,
                name = category.name,
                description = category.description,
                displayOrder = category.displayOrder
            )
        }
    }
}
