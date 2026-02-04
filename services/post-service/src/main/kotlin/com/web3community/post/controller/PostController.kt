package com.web3community.post.controller

import com.web3community.post.dto.PostCreateRequest
import com.web3community.post.dto.PostUpdateRequest
import com.web3community.post.dto.PostResponse
import com.web3community.post.service.PostService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = ["*"])
class PostController(private val postService: PostService) {

    @PostMapping
    fun createPost(@Valid @RequestBody request: PostCreateRequest): Mono<ResponseEntity<PostResponse>> {
        return postService.createPost(request)
            .map { post -> ResponseEntity.status(HttpStatus.CREATED).body(post) }
            .onErrorReturn(ResponseEntity.badRequest().build())
    }

    @GetMapping
    fun getAllPosts(): Flux<PostResponse> {
        return postService.getAllPosts()
    }

    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: String): Mono<ResponseEntity<PostResponse>> {
        return postService.getPostById(id)
            .map { post -> ResponseEntity.ok(post) }
            .onErrorReturn(ResponseEntity.notFound().build())
    }

    @GetMapping("/author/{authorId}")
    fun getPostsByAuthor(@PathVariable authorId: Long): Flux<PostResponse> {
        return postService.getPostsByAuthor(authorId)
    }

    @GetMapping("/category/{category}")
    fun getPostsByCategory(@PathVariable category: String): Flux<PostResponse> {
        return postService.getPostsByCategory(category)
    }

    @GetMapping("/search")
    fun searchPosts(@RequestParam query: String): Flux<PostResponse> {
        return postService.searchPosts(query)
    }

    @GetMapping("/search/tag")
    fun searchPostsByTag(@RequestParam tag: String): Flux<PostResponse> {
        return postService.searchPostsByTag(tag)
    }

    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: String,
        @Valid @RequestBody request: PostUpdateRequest
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.updatePost(id, request)
            .map { post -> ResponseEntity.ok(post) }
            .onErrorReturn(ResponseEntity.notFound().build())
    }

    @PostMapping("/{id}/like")
    fun likePost(@PathVariable id: String): Mono<ResponseEntity<PostResponse>> {
        return postService.likePost(id)
            .map { post -> ResponseEntity.ok(post) }
            .onErrorReturn(ResponseEntity.notFound().build())
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: String): Mono<ResponseEntity<String>> {
        return postService.deletePost(id)
            .map { message -> ResponseEntity.ok(message) }
            .onErrorReturn(ResponseEntity.notFound().build())
    }
}