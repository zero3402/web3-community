package com.web3community.comment.entity

import jakarta.persistence.*

@Entity
@Table(name = "comment_threads")
data class CommentThread(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "post_id", nullable = false)
    val postId: Long,
    
    @Column(name = "root_comment_id", nullable = false)
    val rootCommentId: Long,
    
    @Column(name = "comment_count")
    val commentCount: Long = 0,
    
    @Column(name = "participant_count")
    val participantCount: Long = 0,
    
    @Column(name = "last_comment_at")
    val lastCommentAt: java.time.LocalDateTime? = null,
    
    @Column(name = "last_comment_id")
    val lastCommentId: Long? = null,
    
    @Column(name = "is_active")
    val isActive: Boolean = true,
    
    @Column(name = "is_locked")
    val isLocked: Boolean = false,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)