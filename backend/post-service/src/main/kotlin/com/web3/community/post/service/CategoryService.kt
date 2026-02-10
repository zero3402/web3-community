package com.web3.community.post.service

import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.post.document.Category
import com.web3.community.post.dto.CategoryRequest
import com.web3.community.post.dto.CategoryResponse
import com.web3.community.post.repository.CategoryRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {

    fun getAllCategories(): List<CategoryResponse> {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
            .map { CategoryResponse.from(it) }
    }

    fun createCategory(request: CategoryRequest): CategoryResponse {
        val category = Category(
            name = request.name,
            description = request.description,
            displayOrder = request.displayOrder
        )
        return CategoryResponse.from(categoryRepository.save(category))
    }

    fun updateCategory(id: String, request: CategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }

        category.name = request.name
        request.description?.let { category.description = it }
        category.displayOrder = request.displayOrder

        return CategoryResponse.from(categoryRepository.save(category))
    }

    fun deleteCategory(id: String) {
        val category = categoryRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }
        category.active = false
        categoryRepository.save(category)
    }

    fun getCategoryById(id: String): Category {
        return categoryRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.CATEGORY_NOT_FOUND) }
    }
}
