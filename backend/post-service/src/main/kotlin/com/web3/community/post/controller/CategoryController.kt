package com.web3.community.post.controller

import com.web3.community.common.dto.ApiResponse
import com.web3.community.post.dto.CategoryRequest
import com.web3.community.post.dto.CategoryResponse
import com.web3.community.post.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts/categories")
class CategoryController(private val categoryService: CategoryService) {

    @GetMapping
    fun getAllCategories(): ApiResponse<List<CategoryResponse>> {
        return ApiResponse.success(categoryService.getAllCategories())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(@Valid @RequestBody request: CategoryRequest): ApiResponse<CategoryResponse> {
        return ApiResponse.success(categoryService.createCategory(request))
    }

    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: String,
        @Valid @RequestBody request: CategoryRequest
    ): ApiResponse<CategoryResponse> {
        return ApiResponse.success(categoryService.updateCategory(id, request))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(@PathVariable id: String) {
        categoryService.deleteCategory(id)
    }
}
