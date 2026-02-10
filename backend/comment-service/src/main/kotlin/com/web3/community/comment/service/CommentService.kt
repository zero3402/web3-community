package com.web3.community.comment.service

import com.web3.community.comment.document.Comment
import com.web3.community.comment.dto.*
import com.web3.community.comment.repository.CommentRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.LocalDateTime

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentEventService: CommentEventService
) {
    private val commentSinks = mutableMapOf<String, Sinks.Many<CommentResponse>>()

    fun createComment(userId: Long, nickname: String, request: CreateCommentRequest): Mono<CommentResponse> {
        val depth = if (request.parentId != null) {
            commentRepository.findById(request.parentId)
                .map { it.depth + 1 }
                .defaultIfEmpty(0)
        } else {
            Mono.just(0)
        }

        return depth.flatMap { d ->
            val comment = Comment(
                postId = request.postId,
                parentId = request.parentId,
                depth = d,
                authorId = userId,
                authorNickname = nickname,
                content = request.content
            )
            commentRepository.save(comment)
        }.map { saved ->
            val response = CommentResponse.from(saved)

            val event = CommentEvent(
                eventType = "CREATED",
                commentId = saved.id!!,
                postId = saved.postId,
                authorId = saved.authorId,
                authorNickname = saved.authorNickname,
                content = saved.content,
                parentId = saved.parentId
            )
            commentEventService.publishCommentEvent(event)

            commentSinks[saved.postId]?.tryEmitNext(response)

            response
        }
    }

    fun getCommentsByPostId(postId: String): Flux<CommentResponse> {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
            .map { CommentResponse.from(it) }
            .collectList()
            .flatMapMany { comments ->
                val commentMap = comments.associateBy { it.id }.toMutableMap()
                val rootComments = mutableListOf<CommentResponse>()

                comments.forEach { comment ->
                    if (comment.parentId == null) {
                        rootComments.add(comment)
                    } else {
                        commentMap[comment.parentId]?.children?.add(comment)
                    }
                }

                Flux.fromIterable(rootComments)
            }
    }

    fun streamComments(postId: String): Flux<CommentResponse> {
        val sink = commentSinks.getOrPut(postId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
        return sink.asFlux()
    }

    fun updateComment(id: String, userId: Long, request: UpdateCommentRequest): Mono<CommentResponse> {
        return commentRepository.findById(id)
            .switchIfEmpty(Mono.error(RuntimeException("Comment not found")))
            .flatMap { comment ->
                if (comment.authorId != userId) {
                    return@flatMap Mono.error<Comment>(RuntimeException("Forbidden"))
                }
                comment.content = request.content
                comment.updatedAt = LocalDateTime.now()
                commentRepository.save(comment)
            }
            .map { CommentResponse.from(it) }
    }

    fun deleteComment(id: String, userId: Long): Mono<Void> {
        return commentRepository.findById(id)
            .switchIfEmpty(Mono.error(RuntimeException("Comment not found")))
            .flatMap { comment ->
                if (comment.authorId != userId) {
                    return@flatMap Mono.error<Comment>(RuntimeException("Forbidden"))
                }
                comment.deleted = true
                comment.content = ""
                comment.updatedAt = LocalDateTime.now()
                commentRepository.save(comment)
            }
            .then()
    }

    fun toggleLike(id: String, userId: Long): Mono<CommentResponse> {
        return commentRepository.findById(id)
            .switchIfEmpty(Mono.error(RuntimeException("Comment not found")))
            .flatMap { comment ->
                if (comment.likedUserIds.contains(userId)) {
                    comment.likedUserIds.remove(userId)
                    comment.likeCount--
                } else {
                    comment.likedUserIds.add(userId)
                    comment.likeCount++
                }
                commentRepository.save(comment)
            }
            .map { CommentResponse.from(it) }
    }

    fun getCommentCount(postId: String): Mono<Long> {
        return commentRepository.countByPostIdAndDeletedFalse(postId)
    }
}
