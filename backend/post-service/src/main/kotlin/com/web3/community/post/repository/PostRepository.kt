package com.web3.community.post.repository

import com.web3.community.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long> {
    fun findByCategoryIdAndPublishedTrue(categoryId: Long, pageable: Pageable): Page<Post>
    fun findByAuthorId(authorId: Long, pageable: Pageable): Page<Post>
    fun findByTitleContainingIgnoreCaseAndPublishedTrue(keyword: String, pageable: Pageable): Page<Post>
    fun findByPublishedTrue(pageable: Pageable): Page<Post>

    @Query(
        value = "SELECT DISTINCT p FROM Post p JOIN p.tags t WHERE t = :tag",
        countQuery = "SELECT COUNT(DISTINCT p) FROM Post p JOIN p.tags t WHERE t = :tag"
    )
    fun findByTagsContaining(@Param("tag") tag: String, pageable: Pageable): Page<Post>
}
