package com.web3community.post.repository

import com.web3community.post.entity.Post
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PostRepository : ReactiveMongoRepository<Post, String> {
    fun findByAuthorId(authorId: Long): Flux<Post>
    fun findByAuthorIdAndIsActiveOrderByCreatedAtDesc(authorId: Long, isActive: Boolean): Flux<Post>
    fun findByCategoryAndIsActiveOrderByCreatedAtDesc(category: String, isActive: Boolean): Flux<Post>
    fun findByIsActiveOrderByCreatedAtDesc(isActive: Boolean): Flux<Post>
    fun findByIdAndIsActive(id: String, isActive: Boolean): Mono<Post>
    
    @Query("{ 'title': { '$regex': ?0, '$options': 'i' }, 'isActive': true }")
    fun findByTitleContainingIgnoreCaseAndIsActive(title: String): Flux<Post>
    
    @Query("{ 'content': { '$regex': ?0, '$options': 'i' }, 'isActive': true }")
    fun findByContentContainingIgnoreCaseAndIsActive(content: String): Flux<Post>
    
    @Query("{ 'tags': ?0, 'isActive': true }")
    fun findByTagAndIsActive(tag: String): Flux<Post>
}