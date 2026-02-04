package com.web3community.post.repository

import com.web3community.post.entity.Post
import com.web3community.post.dto.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PostRepository : JpaRepository<Post, Long> {
    
    fun findByStatusOrderByCreatedAtDesc(status: PostStatus, pageable: Pageable): Page<Post>
    
    fun findByAuthorIdOrderByCreatedAtDesc(authorId: Long, pageable: Pageable): Page<Post>
    
    fun findByCategoryIdOrderByCreatedAtDesc(categoryId: Long, pageable: Pageable): Page<Post>
    
    fun findByIsPinnedTrueOrderByCreatedAtDesc(pageable: Pageable): Page<Post>
    
    fun findByIsFeaturedTrueOrderByCreatedAtDesc(pageable: Pageable): Page<Post>
    
    @Query("""
        SELECT p FROM Post p 
        WHERE (:status IS NULL OR p.status = :status)
        AND (:authorId IS NULL OR p.authorId = :authorId)
        AND (:categoryId IS NULL OR p.categoryId = :categoryId)
        AND (:isPinned IS NULL OR p.isPinned = :isPinned)
        AND (:isFeatured IS NULL OR p.isFeatured = :isFeatured)
        AND (:query IS NULL OR p.title LIKE %:query% OR p.content LIKE %:query% OR p.excerpt LIKE %:query%)
        AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom)
        AND (:dateTo IS NULL OR p.createdAt <= :dateTo)
        ORDER BY p.createdAt DESC
    """)
    fun findByMultipleCriteria(
        @Param("status") status: PostStatus?,
        @Param("authorId") authorId: Long?,
        @Param("categoryId") categoryId: Long?,
        @Param("isPinned") isPinned: Boolean?,
        @Param("isFeatured") isFeatured: Boolean?,
        @Param("query") query: String?,
        @Param("dateFrom") dateFrom: LocalDateTime?,
        @Param("dateTo") dateTo: LocalDateTime?,
        pageable: Pageable
    ): Page<Post>
    
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.tag.name IN :tagNames")
    fun findByTagNames(@Param("tagNames") tagNames: List<String>, pageable: Pageable): Page<Post>
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.authorId = :authorId AND p.status = :status")
    fun countByAuthorIdAndStatus(@Param("authorId") authorId: Long, @Param("status") status: PostStatus): Long
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.categoryId = :categoryId AND p.status = :status")
    fun countByCategoryIdAndStatus(@Param("categoryId") categoryId: Long, @Param("status") status: PostStatus): Long
    
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:query% OR p.content LIKE %:query% ORDER BY p.createdAt DESC")
    fun searchByContent(@Param("query") query: String, pageable: Pageable): Page<Post>
    
    @Query("SELECT p FROM Post p WHERE p.publishedAt BETWEEN :startDate AND :endDate ORDER BY p.publishedAt DESC")
    fun findByPublishedAtBetween(@Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime, pageable: Pageable): Page<Post>
    
    fun existsByTitle(title: String): Boolean
    
    fun findByTitleContainingIgnoreCase(title: String, pageable: Pageable): Page<Post>
}