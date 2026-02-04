package com.web3community.post.service

import com.web3community.post.dto.PostCreateRequest
import com.web3community.post.dto.PostUpdateRequest
import com.web3community.post.dto.PostResponse
import com.web3community.post.entity.Post
import com.web3community.post.repository.PostRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class PostService(private val postRepository: PostRepository) {

    fun createPost(request: PostCreateRequest): Mono<PostResponse> {
        val post = Post(
            title = request.title,
            content = request.content,
            authorId = request.authorId,
            authorName = request.authorName,
            category = request.category,
            tags = request.tags
        )

        return postRepository.save(post)
            .map { toResponse(it) }
    }

    fun getAllPosts(): Flux<PostResponse> {
        return postRepository.findByIsActiveOrderByCreatedAtDesc(true)
            .map { toResponse(it) }
    }

    fun getPostById(id: String): Mono<PostResponse> {
        return postRepository.findByIdAndIsActive(id, true)
            .switchIfEmpty(Mono.error(IllegalArgumentException("Post not found")))
            .flatMap { post ->
                val updatedPost = post.copy(viewCount = post.viewCount + 1, updatedAt = LocalDateTime.now())
                postRepository.save(updatedPost)
                    .map { toResponse(it) }
            }
    }

    fun getPostsByAuthor(authorId: Long): Flux<PostResponse> {
        return postRepository.findByAuthorIdAndIsActiveOrderByCreatedAtDesc(authorId, true)
            .map { toResponse(it) }
    }

    fun getPostsByCategory(category: String): Flux<PostResponse> {
        return postRepository.findByCategoryAndIsActiveOrderByCreatedAtDesc(category, true)
            .map { toResponse(it) }
    }

    fun searchPosts(query: String): Flux<PostResponse> {
        return postRepository.findByTitleContainingIgnoreCaseAndIsActive(query)
            .mergeWith(postRepository.findByContentContainingIgnoreCaseAndIsActive(query))
            .distinct { it.id }
            .map { toResponse(it) }
    }

    fun searchPostsByTag(tag: String): Flux<PostResponse> {
        return postRepository.findByTagAndIsActive(tag)
            .map { toResponse(it) }
    }

    fun updatePost(id: String, request: PostUpdateRequest): Mono<PostResponse> {
        return postRepository.findByIdAndIsActive(id, true)
            .switchIfEmpty(Mono.error(IllegalArgumentException("Post not found")))
            .flatMap { post ->
                val updatedPost = post.copy(
                    title = request.title ?: post.title,
                    content = request.content ?: post.content,
                    category = request.category ?: post.category,
                    tags = request.tags ?: post.tags,
                    updatedAt = LocalDateTime.now()
                )
                postRepository.save(updatedPost)
                    .map { toResponse(it) }
            }
    }

    fun deletePost(id: String): Mono<String> {
        return postRepository.findByIdAndIsActive(id, true)
            .switchIfEmpty(Mono.error(IllegalArgumentException("Post not found")))
            .flatMap { post ->
                val deletedPost = post.copy(isActive = false, updatedAt = LocalDateTime.now())
                postRepository.save(deletedPost)
                    .map { "Post deleted successfully" }
            }
    }

    fun likePost(id: String): Mono<PostResponse> {
        return postRepository.findByIdAndIsActive(id, true)
            .switchIfEmpty(Mono.error(IllegalArgumentException("Post not found")))
            .flatMap { post ->
                val updatedPost = post.copy(likeCount = post.likeCount + 1, updatedAt = LocalDateTime.now())
                postRepository.save(updatedPost)
                    .map { toResponse(it) }
            }
    }

    private fun toResponse(post: Post): PostResponse {
        return PostResponse(
            id = post.id ?: "",
            title = post.title,
            content = post.content,
            authorId = post.authorId,
            authorName = post.authorName,
            category = post.category,
            tags = post.tags,
            likeCount = post.likeCount,
            viewCount = post.viewCount,
            isActive = post.isActive,
            createdAt = post.createdAt.toString(),
            updatedAt = post.updatedAt.toString()
        )
    }
}