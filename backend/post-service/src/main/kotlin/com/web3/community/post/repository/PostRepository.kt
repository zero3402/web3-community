package com.web3.community.post.repository

import com.web3.community.post.document.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository : MongoRepository<Post, String> {
    fun findByCategoryIdAndPublishedTrue(categoryId: String, pageable: Pageable): Page<Post>
    fun findByAuthorId(authorId: Long, pageable: Pageable): Page<Post>
    fun findByTagsContaining(tag: String, pageable: Pageable): Page<Post>
    fun findByTitleContainingIgnoreCaseAndPublishedTrue(keyword: String, pageable: Pageable): Page<Post>
    fun findByPublishedTrue(pageable: Pageable): Page<Post>
}
