package com.web3community.notification.entity

import jakarta.persistence.*

@Entity
@Table(name = "notification_preferences")
data class NotificationPreference(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(nullable = false)
    val type: NotificationPreferenceType,
    
    @Column(name = "is_enabled", nullable = false)
    val isEnabled: Boolean = true,
    
    @Column(name = "email_enabled", nullable = false)
    val emailEnabled: Boolean = true,
    
    @Column(name = "push_enabled", nullable = false)
    val pushEnabled: Boolean = true,
    
    @Column(name = "in_app_enabled", nullable = false)
    val inAppEnabled: Boolean = true,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

enum class NotificationPreferenceType {
    POST_LIKES,
    POST_COMMENTS,
    COMMENT_REPLIES,
    COMMENT_LIKES,
    FOLLOWS,
    MENTIONS,
    POST_PUBLISHED,
    COMMENT_PINNED,
    SYSTEM_ANNOUNCEMENTS,
    EMAIL_DIGESTS,
    SECURITY_ALERTS,
    ACHIEVEMENTS,
    MESSAGES
}