package com.web3community.comment.repository

import com.web3community.comment.entity.CommentThread
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CommentThreadRepository : JpaRepository<CommentThread, Long> {
    
    fun findByPostId(postId: Long): List<CommentThread>
    
    fun findByPostIdAndIsActiveTrue(postId: Long): List<CommentThread>
    
    fun findByRootCommentId(rootCommentId: Long): Optional<CommentThread>
    
    fun findByPostIdAndIsLockedFalse(postId: Long): List<CommentThread>
    
    @Query("SELECT COUNT(ct) FROM CommentThread ct WHERE ct.postId = :postId AND ct.isActive = true")
    fun countActiveThreadsByPostId(@Param("postId") postId: Long): Long
    
    @Query("SELECT ct FROM CommentThread ct WHERE ct.lastCommentAt BETWEEN :startDate AND :endDate ORDER BY ct.lastCommentAt DESC")
    fun findActiveThreadsByDateRange(
        @Param("startDate") startDate: java.time.LocalDateTime,
        @Param("endDate") endDate: java.time.LocalDateTime
    ): List<CommentThread>
    
    @Query("""
        SELECT ct.postId, SUM(ct.commentCount) as totalComments,
        SUM(ct.participantCount) as totalParticipants,
        COUNT(ct) as threadCount
        FROM CommentThread ct
        WHERE ct.isActive = true
        GROUP BY ct.postId
        ORDER BY totalComments DESC
    """)
    fun getPostThreadStats(): List<Array<Any>>
    
    @Query("""
        SELECT ct.rootCommentId, ct.commentCount, ct.participantCount 
        FROM CommentThread ct 
        WHERE ct.isActive = true 
        ORDER BY ct.commentCount DESC
    """)
    fun getMostActiveThreads(): List<Array<Any>>
    
    @Query("SELECT DISTINCT ct.postId FROM CommentThread ct WHERE ct.isActive = true")
    fun findPostsWithActiveThreads(): List<Long>
    
    fun existsByPostIdAndRootCommentId(postId: Long, rootCommentId: Long): Boolean
}