package com.web3community.post.infrastructure.controller

import com.web3community.post.application.service.PostApplicationService
import com.web3community.post.application.dto.*
import com.web3community.util.annotation.CurrentUser
import com.web3community.user.dto.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/posts")
@CrossOrigin(origins = ["*"])
class PostController(
    private val postApplicationService: PostApplicationService
) {

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun createPost(
        @Valid @RequestBody request: CreatePostRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<PostResponse> {
        val post = postApplicationService.createPost(request, currentUser.getUserId())
        return ResponseEntity.created(post.id.toLocation()).body(post)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun updatePost(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePostRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postApplicationService.updatePost(id, request, currentUser.getUserId()))
    }

    @GetMapping("/{id}")
    fun getPost(@PathVariable id: Long): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postApplicationService.getPostById(id))
    }

    @GetMapping("/author/{authorId}")
    fun getPostsByAuthor(
        @PathVariable authorId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PostListResponse> {
        return ResponseEntity.ok(postApplicationService.getPostsByAuthor(authorId, page, size))
    }

    @GetMapping("/category/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PostListResponse> {
        return ResponseEntity.ok(postApplicationService.getPostsByCategory(categoryId, page, size))
    }

    @GetMapping("/search")
    fun searchPosts(
        @Valid @RequestBody request: PostSearchRequest
    ): ResponseEntity<PostListResponse> {
        return ResponseEntity.ok(postApplicationService.searchPosts(request.query, request.page, request.size))
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun publishPost(
        @PathVariable id: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postApplicationService.publishPost(id, currentUser.getUserId()))
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun archivePost(
        @PathVariable id: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postApplicationService.archivePost(id, currentUser.getUserId()))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun deletePost(
        @PathVariable id: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<Map<String, String>> {
        postApplicationService.deletePost(id, currentUser.getUserId())
        return ResponseEntity.ok(mapOf("message" to "Post deleted successfully"))
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun likePost(
        @PathVariable id: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postApplicationService.likePost(id, currentUser.getUserId()))
    }

    @DeleteMapping("/{id}/like")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun unlikePost(
        @PathVariable id: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postApplicationService.unlikePost(id, currentUser.getUserId()))
    }

    @PostMapping("/{id}/share")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun sharePost(
        @PathVariable id: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postApplicationService.sharePost(id, currentUser.getUserId()))
    }

    @GetMapping("/featured")
    fun getFeaturedPosts(
        @RequestParam(defaultValue = "5") limit: Int
    ): ResponseEntity<List<PostResponse>> {
        return ResponseEntity.ok(postApplicationService.getFeaturedPosts(limit))
    }

    @GetMapping("/pinned")
    fun getPinnedPosts(
        @RequestParam(defaultValue = "5") limit: Int
    ): ResponseEntity<List<PostResponse>> {
        return ResponseEntity.ok(postApplicationService.getPinnedPosts(limit))
    }
}

// 확장 함수
fun Long.toLocation(): java.net.URI {
    return java.net.URI.create("/api/v1/posts/$this")
}