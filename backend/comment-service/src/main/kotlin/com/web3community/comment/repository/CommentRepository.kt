package com.web3community.comment.repository

import com.web3community.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    
    fun findByPostIdOrderByCreatedAtAsc(postId: Long, pageable: Pageable): Page<Comment>
    
    fun findByPostIdAndParentIdOrderByCreatedAtAsc(postId: Long, parentId: Long?, pageable: Pageable): Page<Comment>
    
    fun findByThreadIdOrderByCreatedAtAsc(threadId: Long, pageable: Pageable): Page<Comment>
    
    fun findByAuthorIdOrderByCreatedAtDesc(authorId: Long, pageable: Pageable): Page<Comment>
    
    fun findByParentIdOrderByCreatedAtAsc(parentId: Long): List<Comment>
    
    fun findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId: Long): List<Comment>
    
    fun findByPostIdAndParentIdAndIsDeletedFalseOrderByCreatedAtAsc(postId: Long, parentId: Long?): List<Comment>
    
    fun findByThreadIdAndLevelLessThanOrderByCreatedAtAsc(threadId: Long, maxLevel: Int): List<Comment>
    
    fun findByPostIdAndIsPinnedTrueOrderByCreatedAtAsc(postId: Long): List<Comment>
    
    fun findByPostIdAndIsReportedTrueOrderByCreatedAtDesc(postId: Long): List<Comment>
    
    @Query("""
        SELECT c FROM Comment c 
        WHERE (:postId IS NULL OR c.postId = :postId)
        AND (:authorId IS NULL OR c.authorId = :authorId)
        AND (:parentId IS NULL OR c.parentId = :parentId)
        AND (:threadId IS NULL OR c.threadId = :threadId)
        AND (:level IS NULL OR c.level = :level)
        AND (:isEdited IS NULL OR c.isEdited = :isEdited)
        AND (:isDeleted IS NULL OR c.isDeleted = :isDeleted)
        AND (:isPinned IS NULL OR c.isPinned = :isPinned)
        AND (:dateFrom IS NULL OR c.createdAt >= :dateFrom)
        AND (:dateTo IS NULL OR c.createdAt <= :dateTo)
        ORDER BY c.createdAt DESC
    """)
    fun findByMultipleCriteria(
        @Param("postId") postId: Long?,
        @Param("authorId") authorId: Long?,
        @Param("parentId") parentId: Long?,
        @Param("threadId") threadId: Long?,
        @Param("level") level: Int?,
        @Param("isEdited") isEdited: Boolean?,
        @Param("isDeleted") isDeleted: Boolean?,
        @Param("isPinned") isPinned: Boolean?,
        @Param("dateFrom") dateFrom: LocalDateTime?,
        @Param("dateTo") dateTo: LocalDateTime?,
        pageable: Pageable
    ): Page<Comment>
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.postId = :postId AND c.isDeleted = false")
    fun countByPostIdAndIsDeletedFalse(@Param("postId") postId: Long): Long
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.postId = :postId AND c.parentId IS NULL AND c.isDeleted = false")
    fun countRootCommentsByPostIdAndIsDeletedFalse(@Param("postId") postId: Long): Long
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.authorId = :authorId AND c.isDeleted = false")
    fun countByAuthorIdAndIsDeletedFalse(@Param("authorId") authorId: Long): Long
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.threadId = :threadId AND c.isDeleted = false")
    fun countByThreadIdAndIsDeletedFalse(@Param("threadId") threadId: Long): Long
    
    @Query("""
        SELECT c.authorId, COUNT(c) as commentCount 
        FROM Comment c 
        WHERE c.isDeleted = false 
        GROUP BY c.authorId 
        ORDER BY commentCount DESC
    """)
    fun findTopCommentAuthors(pageable: Pageable): List<Array<Any>>
    
    @Query("""
        SELECT c.postId, COUNT(c) as commentCount 
        FROM Comment c 
        WHERE c.isDeleted = false 
        GROUP BY c.postId 
        ORDER BY commentCount DESC
    """)
    fun findMostCommentedPosts(pageable: Pageable): List<Array<Any>>
    
    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :commentId")
    fun incrementReplyCount(@Param("commentId") commentId: Long): Int
    
    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount - 1 WHERE c.id = :commentId")
    fun decrementReplyCount(@Param("commentId") commentId: Long): Int
    
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = :count WHERE c.id = :commentId")
    fun updateLikeCount(@Param("commentId") commentId: Long, @Param("count") count: Long): Int
    
    @Modifying
    @Query("UPDATE Comment c SET c.dislikeCount = :count WHERE c.id = :commentId")
    fun updateDislikeCount(@Param("commentId") commentId: Long, @Param("count") count: Long): Int
    
    @Modifying
    @Query("UPDATE Comment c SET c.isPinned = :isPinned WHERE c.id = :commentId")
    fun updatePinStatus(@Param("commentId") commentId: Long, @Param("isPinned") isPinned: Boolean): Int
    
    @Modifying
    @Query("UPDATE Comment c SET c.isReported = :isReported WHERE c.id = :commentId")
    fun updateReportStatus(@Param("commentId") commentId: Long, @Param("isReported") isReported: Boolean): Int
    
    fun existsByPostIdAndAuthorIdAndContent(postId: Long, authorId: Long, content: String): Boolean
}