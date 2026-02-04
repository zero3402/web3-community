package com.web3community.comment.controller

import com.web3community.comment.dto.*
import com.web3community.comment.service.CommentService
import com.web3community.util.annotation.CurrentUser
import com.web3community.user.dto.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/comments")
@CrossOrigin(origins = ["*"])
class CommentController(
    private val commentService: CommentService
) {

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun createComment(
        @Valid @RequestBody request: CommentCreateRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.createComment(request, currentUser.getUserId()))
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun updateComment(
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentUpdateRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.updateComment(commentId, request, currentUser.getUserId()))
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun deleteComment(
        @PathVariable commentId: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<String> {
        return ResponseEntity.ok(commentService.deleteComment(commentId, currentUser.getUserId()))
    }

    @GetMapping("/{commentId}")
    fun getComment(
        @PathVariable commentId: Long,
        @CurrentUser currentUser: CustomUserDetails? = null
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.getCommentById(commentId, currentUser?.getUserId()))
    }

    @GetMapping("/search")
    fun searchComments(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RequestParam postId: String? = null,
        @RequestParam authorId: String? = null,
        @RequestParam parentId: String? = null,
        @RequestParam threadId: String? = null,
        @RequestParam level: Int? = null,
        @RequestParam isEdited: Boolean? = null,
        @RequestParam isDeleted: Boolean? = null,
        @RequestParam isPinned: Boolean? = null,
        @RequestParam dateFrom: String? = null,
        @RequestParam dateTo: String? = null,
        @RequestParam(defaultValue = "true") includeReplies: Boolean,
        @CurrentUser currentUser: CustomUserDetails? = null
    ): ResponseEntity<CommentListResponse> {
        
        val searchRequest = CommentSearchRequest(
            postId = postId,
            authorId = authorId,
            parentId = parentId,
            threadId = threadId,
            level = level,
            isEdited = isEdited,
            isDeleted = isDeleted,
            isPinned = isPinned,
            dateFrom = dateFrom,
            dateTo = dateTo,
            sortBy = CommentSortBy.valueOf(sortBy.uppercase()),
            sortDirection = SortDirection.valueOf(sortDirection.uppercase()),
            includeReplies = includeReplies
        )
        
        val sort = Sort.by(
            if (sortDirection.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC,
            sortBy.lowercase()
        )
        val pageable: Pageable = PageRequest.of(page, size, sort)
        
        return ResponseEntity.ok(commentService.getComments(searchRequest, pageable, currentUser?.getUserId()))
    }

    @GetMapping("/post/{postId}")
    fun getPostComments(
        @PathVariable postId: Long,
        @CurrentUser currentUser: CustomUserDetails? = null
    ): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getPostComments(postId, currentUser?.getUserId()))
    }

    @GetMapping("/post/{postId}/flat")
    fun getPostCommentsFlat(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentUser currentUser: CustomUserDetails? = null
    ): ResponseEntity<CommentListResponse> {
        return ResponseEntity.ok(commentService.getPostCommentsFlat(postId, page, size, currentUser?.getUserId()))
    }

    @GetMapping("/{commentId}/replies")
    fun getCommentReplies(
        @PathVariable commentId: Long,
        @CurrentUser currentUser: CustomUserDetails? = null
    ): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getCommentReplies(commentId, currentUser?.getUserId()))
    }

    @GetMapping("/thread/{threadId}")
    fun getCommentThread(
        @PathVariable threadId: Long,
        @CurrentUser currentUser: CustomUserDetails? = null
    ): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getCommentThread(threadId, currentUser?.getUserId()))
    }

    @PostMapping("/{commentId}/react")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun addReaction(
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentReactionRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<CommentReactionResponse> {
        return ResponseEntity.ok(commentService.addReaction(commentId, request, currentUser.getUserId()))
    }

    @DeleteMapping("/{commentId}/react")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun removeReaction(
        @PathVariable commentId: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<String> {
        return ResponseEntity.ok(commentService.removeReaction(commentId, currentUser.getUserId()))
    }

    @PostMapping("/{commentId}/pin")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    fun pinComment(@PathVariable commentId: Long): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.pinComment(commentId))
    }

    @DeleteMapping("/{commentId}/pin")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    fun unpinComment(@PathVariable commentId: Long): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.unpinComment(commentId))
    }

    @PostMapping("/{commentId}/report")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun reportComment(@PathVariable commentId: Long): ResponseEntity<String> {
        return ResponseEntity.ok(commentService.reportComment(commentId))
    }

    @GetMapping("/post/{postId}/stats")
    fun getCommentStats(@PathVariable postId: Long): ResponseEntity<CommentStatsResponse> {
        return ResponseEntity.ok(commentService.getCommentStats(postId))
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("type", required = false) type: String? = null,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<Map<String, Any>> {
        // This would integrate with a file storage service
        return ResponseEntity.ok(mapOf(
            "message" to "File uploaded successfully",
            "filename" to file.originalFilename,
            "size" to file.size,
            "contentType" to file.contentType
        ))
    }
}