package com.web3.community.comment.controller

import com.web3.community.comment.dto.CommentResponse
import com.web3.community.comment.dto.CreateCommentRequest
import com.web3.community.comment.dto.UpdateCommentRequest
import com.web3.community.comment.service.CommentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api/comments")
class CommentController(private val commentService: CommentService) {

    @Operation(summary = "댓글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestHeader(value = "X-User-Nickname", required = false, defaultValue = "Anonymous") nickname: String,
        @Valid @RequestBody request: CreateCommentRequest
    ): Mono<CommentResponse> {
        return commentService.createComment(userId, nickname, request)
    }

    @Operation(summary = "게시글별 댓글 조회")
    @GetMapping("/post/{postId}")
    fun getCommentsByPostId(@PathVariable postId: String): Flux<CommentResponse> {
        return commentService.getCommentsByPostId(postId)
    }

    @Operation(summary = "댓글 실시간 스트리밍 (SSE)")
    @GetMapping("/post/{postId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamComments(@PathVariable postId: String): Flux<CommentResponse> {
        return commentService.streamComments(postId)
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{id}")
    fun updateComment(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: UpdateCommentRequest
    ): Mono<CommentResponse> {
        return commentService.updateComment(id, userId, request)
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long
    ): Mono<Void> {
        return commentService.deleteComment(id, userId)
    }

    @Operation(summary = "좋아요 토글")
    @PostMapping("/{id}/like")
    fun toggleLike(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long
    ): Mono<CommentResponse> {
        return commentService.toggleLike(id, userId)
    }

    @Operation(summary = "댓글 수 조회")
    @GetMapping("/post/{postId}/count")
    fun getCommentCount(@PathVariable postId: String): Mono<Long> {
        return commentService.getCommentCount(postId)
    }
}
