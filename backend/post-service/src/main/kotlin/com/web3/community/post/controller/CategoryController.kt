package com.web3.community.post.controller

import com.web3.community.common.dto.ApiResponse
import com.web3.community.post.dto.CategoryRequest
import com.web3.community.post.dto.CategoryResponse
import com.web3.community.post.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Category", description = "카테고리 API")
@RestController
@RequestMapping("/api/posts/categories")
class CategoryController(private val categoryService: CategoryService) {

    @Operation(summary = "카테고리 목록 조회")
    @GetMapping
    fun getAllCategories(): ApiResponse<List<CategoryResponse>> {
        return ApiResponse.success(categoryService.getAllCategories())
    }

    @Operation(summary = "카테고리 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(@Valid @RequestBody request: CategoryRequest): ApiResponse<CategoryResponse> {
        return ApiResponse.success(categoryService.createCategory(request))
    }

    @Operation(summary = "카테고리 수정")
    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: String,
        @Valid @RequestBody request: CategoryRequest
    ): ApiResponse<CategoryResponse> {
        return ApiResponse.success(categoryService.updateCategory(id, request))
    }

    @Operation(summary = "카테고리 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(@PathVariable id: String) {
        categoryService.deleteCategory(id)
    }
}
