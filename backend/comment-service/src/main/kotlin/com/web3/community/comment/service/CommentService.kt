package com.web3.community.comment.service

import com.web3.community.comment.entity.Comment
import com.web3.community.comment.entity.CommentLike
import com.web3.community.comment.dto.*
import com.web3.community.comment.repository.CommentLikeRepository
import com.web3.community.comment.repository.CommentRepository
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val commentEventService: CommentEventService
) {
    private val commentSinks = ConcurrentHashMap<Long, Sinks.Many<CommentResponse>>()

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

    fun getCommentsByPostId(postId: Long): Flux<CommentResponse> {
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

    fun streamComments(postId: Long): Flux<CommentResponse> {
        val sink = commentSinks.computeIfAbsent(postId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
        return sink.asFlux()
            .doFinally {
                if (sink.currentSubscriberCount() == 0) {
                    commentSinks.remove(postId)
                }
            }
    }

    fun updateComment(id: Long, userId: Long, request: UpdateCommentRequest): Mono<CommentResponse> {
        return commentRepository.findById(id)
            .switchIfEmpty(Mono.error(BusinessException(ErrorCode.COMMENT_NOT_FOUND)))
            .flatMap { comment ->
                if (comment.authorId != userId) {
                    return@flatMap Mono.error<Comment>(BusinessException(ErrorCode.FORBIDDEN))
                }
                comment.content = request.content
                comment.updatedAt = LocalDateTime.now()
                commentRepository.save(comment)
            }
            .map { CommentResponse.from(it) }
    }

    fun deleteComment(id: Long, userId: Long): Mono<Void> {
        return commentRepository.findById(id)
            .switchIfEmpty(Mono.error(BusinessException(ErrorCode.COMMENT_NOT_FOUND)))
            .flatMap { comment ->
                if (comment.authorId != userId) {
                    return@flatMap Mono.error<Comment>(BusinessException(ErrorCode.FORBIDDEN))
                }
                comment.deleted = true
                comment.content = ""
                comment.updatedAt = LocalDateTime.now()
                commentRepository.save(comment)
            }
            .then()
    }

    fun toggleLike(id: Long, userId: Long): Mono<CommentResponse> {
        return commentRepository.findById(id)
            .switchIfEmpty(Mono.error(BusinessException(ErrorCode.COMMENT_NOT_FOUND)))
            .flatMap { comment ->
                commentLikeRepository.findByCommentIdAndUserId(id, userId)
                    .flatMap { _ ->
                        commentLikeRepository.deleteByCommentIdAndUserId(id, userId)
                            .then(Mono.fromCallable { comment.apply { likeCount-- } })
                    }
                    .switchIfEmpty(
                        Mono.defer {
                            commentLikeRepository.save(CommentLike(commentId = id, userId = userId))
                                .then(Mono.fromCallable { comment.apply { likeCount++ } })
                        }
                    )
            }
            .flatMap { comment ->
                comment.updatedAt = LocalDateTime.now()
                commentRepository.save(comment)
            }
            .map { CommentResponse.from(it) }
    }

    fun getCommentCount(postId: Long): Mono<Long> {
        return commentRepository.countByPostIdAndDeletedFalse(postId)
    }
}
