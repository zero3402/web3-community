package com.web3.community.post.controller

import com.web3.community.common.dto.ApiResponse
import com.web3.community.common.dto.PageResponse
import com.web3.community.post.dto.*
import com.web3.community.post.service.PostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Post", description = "게시글 API")
@RestController
@RequestMapping("/api/posts")
class PostController(private val postService: PostService) {

    @Operation(summary = "게시글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestHeader("X-User-Email") email: String,
        @RequestHeader(value = "X-User-Nickname", required = false, defaultValue = "Anonymous") nickname: String,
        @Valid @RequestBody request: CreatePostRequest
    ): ApiResponse<PostResponse> {
        return ApiResponse.success(postService.createPost(userId, nickname, request))
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    fun getPosts(
        @RequestParam(required = false) categoryId: String?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<PageResponse<PostSummaryResponse>> {
        return ApiResponse.success(postService.getPosts(categoryId, tag, keyword, pageable))
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: String): ApiResponse<PostResponse> {
        return ApiResponse.success(postService.getPostById(id))
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ): ApiResponse<PostResponse> {
        return ApiResponse.success(postService.updatePost(id, userId, request))
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long,
        @RequestHeader("X-User-Role") role: String
    ) {
        postService.deletePost(id, userId, role)
    }

    @Operation(summary = "좋아요 토글")
    @PostMapping("/{id}/like")
    fun toggleLike(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long
    ): ApiResponse<PostResponse> {
        return ApiResponse.success(postService.toggleLike(id, userId))
    }
}
