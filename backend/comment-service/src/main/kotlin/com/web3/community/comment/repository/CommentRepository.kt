package com.web3.community.comment.repository

import com.web3.community.comment.document.Comment
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CommentRepository : ReactiveMongoRepository<Comment, String> {
    fun findByPostIdOrderByCreatedAtAsc(postId: String): Flux<Comment>
    fun findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId: String): Flux<Comment>
    fun findByParentIdOrderByCreatedAtAsc(parentId: String): Flux<Comment>
    fun countByPostIdAndDeletedFalse(postId: String): Mono<Long>
}
