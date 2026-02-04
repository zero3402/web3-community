package com.web3community.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val content: String,
    
    @Column(name = "post_id", nullable = false)
    val postId: Long,
    
    @Column(name = "author_id", nullable = false)
    val authorId: Long,
    
    @Column(name = "parent_id")
    val parentId: Long? = null,
    
    @Column(name = "thread_id", nullable = false)
    val threadId: Long,
    
    @Column(name = "level", nullable = false)
    val level: Int = 0,
    
    @Column(name = "like_count")
    val likeCount: Long = 0,
    
    @Column(name = "dislike_count")
    val dislikeCount: Long = 0,
    
    @Column(name = "reply_count")
    val replyCount: Long = 0,
    
    @Column(name = "is_edited")
    val isEdited: Boolean = false,
    
    @Column(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @Column(name = "is_pinned")
    val isPinned: Boolean = false,
    
    @Column(name = "is_reported")
    val isReported: Boolean = false,
    
    @Column(name = "edited_content")
    val editedContent: String? = null,
    
    @Column(name = "edited_at")
    val editedAt: LocalDateTime? = null,
    
    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "created_by")
    val createdBy: Long? = null,
    
    @Column(name = "updated_by")
    val updatedBy: Long? = null,
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    val parent: Comment? = null,
    
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val replies: MutableList<Comment> = mutableListOf(),
    
    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val reactions: MutableList<CommentReaction> = mutableListOf(),
    
    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val attachments: MutableList<CommentAttachment> = mutableListOf()
    
) {
    // Helper methods for tree structure
    fun isRoot(): Boolean = parentId == null
    fun isReply(): Boolean = parentId != null
    fun canReply(): Boolean = level < 5 // Maximum nesting level
}