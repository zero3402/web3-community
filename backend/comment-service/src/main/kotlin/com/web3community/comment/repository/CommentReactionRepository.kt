package com.web3community.comment.repository

import com.web3community.comment.entity.CommentReaction
import com.web3community.comment.entity.ReactionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CommentReactionRepository : JpaRepository<CommentReaction, Long> {
    
    fun findByCommentIdAndUserId(commentId: Long, userId: Long): Optional<CommentReaction>
    
    fun findByCommentId(commentId: Long): List<CommentReaction>
    
    fun findByUserId(userId: Long): List<CommentReaction>
    
    fun findByCommentIdAndReactionType(commentId: Long, reactionType: ReactionType): List<CommentReaction>
    
    @Query("SELECT COUNT(cr) FROM CommentReaction cr WHERE cr.comment.id = :commentId AND cr.reactionType = :reactionType")
    fun countByCommentIdAndReactionType(@Param("commentId") commentId: Long, @Param("reactionType") reactionType: ReactionType): Long
    
    @Query("SELECT cr.reactionType, COUNT(cr) FROM CommentReaction cr WHERE cr.comment.id = :commentId GROUP BY cr.reactionType")
    fun countByCommentIdGroupByReactionType(@Param("commentId") commentId: Long): List<Array<Any>>
    
    @Query("SELECT COUNT(cr) FROM CommentReaction cr WHERE cr.userId = :userId")
    fun countByUserId(@Param("userId") userId: Long): Long
    
    @Query("""
        SELECT cr.comment.id, COUNT(cr) as reactionCount 
        FROM CommentReaction cr 
        GROUP BY cr.comment.id 
        ORDER BY reactionCount DESC
    """)
    fun findMostReactedComments(): List<Array<Any>>
    
    @Query("SELECT DISTINCT cr.userId FROM CommentReaction cr WHERE cr.comment.id = :commentId")
    fun findUserIdsByCommentId(@Param("commentId") commentId: Long): List<Long>
    
    @Query("SELECT cr.reactionType, COUNT(cr) FROM CommentReaction cr WHERE cr.userId = :userId GROUP BY cr.reactionType")
    fun getUserReactionStats(@Param("userId") userId: Long): List<Array<Any>>
    
    fun existsByCommentIdAndUserId(commentId: Long, userId: Long): Boolean
}