package com.web3community.notification.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "recipient_id", nullable = false)
    val recipientId: Long,
    
    @Column(nullable = false)
    val type: NotificationType,
    
    @Column(nullable = false)
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val message: String,
    
    @Column(name = "entity_type")
    val entityType: EntityType? = null,
    
    @Column(name = "entity_id")
    val entityId: Long? = null,
    
    @Column(name = "sender_id")
    val senderId: Long? = null,
    
    @Column(name = "sender_name")
    val senderName: String? = null,
    
    @Column(name = "sender_avatar")
    val senderAvatar: String? = null,
    
    @Column(name = "action_url")
    val actionUrl: String? = null,
    
    @Column(name = "action_text")
    val actionText: String? = null,
    
    @Column(name = "image_url")
    val imageUrl: String? = null,
    
    @Column(name = "metadata", columnDefinition = "JSON")
    val metadata: String? = null,
    
    @Column(nullable = false)
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    
    @Column(name = "is_read", nullable = false)
    val isRead: Boolean = false,
    
    @Column(name = "is_sent", nullable = false)
    val isSent: Boolean = false,
    
    @Column(name = "is_email_sent", nullable = false)
    val isEmailSent: Boolean = false,
    
    @Column(name = "is_push_sent", nullable = false)
    val isPushSent: Boolean = false,
    
    @Column(name = "read_at")
    val readAt: LocalDateTime? = null,
    
    @Column(name = "sent_at")
    val sentAt: LocalDateTime? = null,
    
    @Column(name = "expires_at")
    val expiresAt: LocalDateTime? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
    
) {
    fun isExpired(): Boolean = expiresAt?.isBefore(LocalDateTime.now()) == true
    
    fun shouldSendEmail(): Boolean = !isEmailSent && priority != NotificationPriority.LOW
    
    fun shouldSendPush(): Boolean = !isPushSent && priority != NotificationPriority.LOW
}

enum class NotificationType {
    POST_LIKE,
    POST_COMMENT,
    COMMENT_REPLY,
    COMMENT_LIKE,
    FOLLOW,
    MENTION,
    POST_PUBLISHED,
    COMMENT_PINNED,
    SYSTEM_ANNOUNCEMENT,
    ACCOUNT_VERIFICATION,
    PASSWORD_CHANGE,
    SECURITY_ALERT,
    CONTENT_APPROVED,
    CONTENT_REJECTED,
    ACHIEVEMENT_UNLOCKED,
    WEEKLY_DIGEST,
    TRENDING_POST,
    NEW_MESSAGE
}

enum class EntityType {
    POST,
    COMMENT,
    USER,
    SYSTEM,
    MESSAGE,
    ACHIEVEMENT
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}