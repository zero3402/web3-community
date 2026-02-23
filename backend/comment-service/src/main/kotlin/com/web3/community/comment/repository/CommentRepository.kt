package com.web3.community.comment.repository

import com.web3.community.comment.entity.Comment
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CommentRepository : ReactiveCrudRepository<Comment, Long> {
    fun findByPostIdOrderByCreatedAtAsc(postId: Long): Flux<Comment>
    fun findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId: Long): Flux<Comment>
    fun findByParentIdOrderByCreatedAtAsc(parentId: Long): Flux<Comment>
    fun countByPostIdAndDeletedFalse(postId: Long): Mono<Long>
}
