package com.web3.community.comment.repository

import com.web3.community.comment.entity.CommentLike
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface CommentLikeRepository : ReactiveCrudRepository<CommentLike, Long> {
    fun findByCommentIdAndUserId(commentId: Long, userId: Long): Mono<CommentLike>
    fun deleteByCommentIdAndUserId(commentId: Long, userId: Long): Mono<Void>
}
