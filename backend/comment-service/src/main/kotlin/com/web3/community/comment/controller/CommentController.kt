package com.web3.community.comment.controller

import com.web3.community.comment.dto.CommentResponse
import com.web3.community.comment.dto.CreateCommentRequest
import com.web3.community.comment.dto.UpdateCommentRequest
import com.web3.community.comment.service.CommentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/comments")
class CommentController(private val commentService: CommentService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestHeader(value = "X-User-Nickname", required = false, defaultValue = "Anonymous") nickname: String,
        @Valid @RequestBody request: CreateCommentRequest
    ): Mono<CommentResponse> {
        return commentService.createComment(userId, nickname, request)
    }

    @GetMapping("/post/{postId}")
    fun getCommentsByPostId(@PathVariable postId: String): Flux<CommentResponse> {
        return commentService.getCommentsByPostId(postId)
    }

    @GetMapping("/post/{postId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamComments(@PathVariable postId: String): Flux<CommentResponse> {
        return commentService.streamComments(postId)
    }

    @PutMapping("/{id}")
    fun updateComment(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: UpdateCommentRequest
    ): Mono<CommentResponse> {
        return commentService.updateComment(id, userId, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long
    ): Mono<Void> {
        return commentService.deleteComment(id, userId)
    }

    @PostMapping("/{id}/like")
    fun toggleLike(
        @PathVariable id: String,
        @RequestHeader("X-User-Id") userId: Long
    ): Mono<CommentResponse> {
        return commentService.toggleLike(id, userId)
    }

    @GetMapping("/post/{postId}/count")
    fun getCommentCount(@PathVariable postId: String): Mono<Long> {
        return commentService.getCommentCount(postId)
    }
}
