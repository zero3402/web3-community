package com.web3community.comment.entity

import jakarta.persistence.*

@Entity
@Table(name = "comment_reactions")
data class CommentReaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    val comment: Comment,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(nullable = false)
    val reactionType: ReactionType,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

enum class ReactionType {
    LIKE,
    DISLIKE,
    LAUGH,
    LOVE,
    ANGRY,
    SAD
}