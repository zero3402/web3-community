package com.web3community.notification.domain.model

import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.domain.PostId
import com.web3community.sharedkernel.domain.CommentId
import com.web3community.sharedkernel.events.NotificationDomainEvent
import jakarta.persistence.*

// 알림 애그리게이트 루트
@Entity
@Table(name = "notifications")
class Notification(
    @Column(name = "recipient_id", nullable = false)
    var recipientId: UserId,
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "ENUM('POST_LIKE', 'POST_COMMENT', 'COMMENT_REPLY', 'COMMENT_LIKE', 'FOLLOW', 'MENTION', 'POST_PUBLISHED', 'COMMENT_PINNED', 'SYSTEM_ANNOUNCEMENT', 'ACCOUNT_VERIFICATION', 'PASSWORD_CHANGE', 'SECURITY_ALERT', 'CONTENT_APPROVED', 'CONTENT_REJECTED', 'ACHIEVEMENT_UNLOCKED', 'WEEKLY_DIGEST', 'TRENDING_POST', 'NEW_MESSAGE')")
    var type: NotificationType,
        private set

    @Column(name = "title", nullable = false, columnDefinition = "VARCHAR(255)")
    var title: String,
        set(value) {
            require(value.isNotBlank()) { "Title cannot be blank" }
            require(value.length <= 255) { "Title must be at most 255 characters" }
            field = value
            addDomainEvent(NotificationDomainEvent.NotificationSent(id.value.toString(), recipientId.value.toString(), type.name))
        }

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    var message: String,
        set(value) {
            require(value.isNotBlank()) { "Message cannot be blank" }
            field = value
            addDomainEvent(NotificationDomainEvent.NotificationSent(id.value.toString(), recipientId.value.toString(), type.name))
        }

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", columnDefinition = "ENUM('POST', 'COMMENT', 'USER', 'SYSTEM', 'MESSAGE', 'ACHIEVEMENT')")
    var entityType: EntityType? = null,
        private set

    @Column(name = "entity_id")
    var entityId: Long? = null,
        private set

    @Column(name = "sender_id")
    var senderId: UserId? = null,
        private set

    @Column(name = "sender_name", columnDefinition = "VARCHAR(100)")
    var senderName: String? = null,
        private set

    @Column(name = "sender_avatar", columnDefinition = "VARCHAR(500)")
    var senderAvatar: String? = null,
        private set

    @Column(name = "action_url", columnDefinition = "VARCHAR(500)")
    var actionUrl: String? = null,
        private set

    @Column(name = "action_text", columnDefinition = "VARCHAR(100)")
    var actionText: String? = null,
        private set

    @Column(name = "image_url", columnDefinition = "VARCHAR(500)")
    var imageUrl: String? = null,
        private set

    @Column(name = "metadata", columnDefinition = "JSON")
    var metadata: String? = null,
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, columnDefinition = "ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT')")
    var priority: NotificationPriority = NotificationPriority.NORMAL,
        private set

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
        private set

    @Column(name = "is_sent", nullable = false)
    var isSent: Boolean = false,
        private set

    @Column(name = "is_email_sent", nullable = false)
    var isEmailSent: Boolean = false,
        private set

    @Column(name = "is_push_sent", nullable = false)
    var isPushSent: Boolean = false,
        private set

    @Column(name = "read_at")
    var readAt: java.time.LocalDateTime? = null,
        private set

    @Column(name = "sent_at")
    var sentAt: java.time.LocalDateTime? = null,
        private set

    @Column(name = "expires_at")
    var expiresAt: java.time.LocalDateTime? = null,
        private set

    // JPA 생성자
    protected constructor() : super()

    // 도메인 생성자
    constructor(
        recipientId: UserId,
        type: NotificationType,
        title: String,
        message: String,
        entityType: EntityType? = null,
        entityId: Long? = null,
        senderId: UserId? = null,
        senderName: String? = null,
        senderAvatar: String? = null,
        actionUrl: String? = null,
        actionText: String? = null,
        imageUrl: String? = null,
        metadata: Map<String, Any>? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        expiresAt: java.time.LocalDateTime? = null
    ) : super() {
        this.recipientId = recipientId
        this.type = type
        this.title = title
        this.message = message
        this.entityType = entityType
        this.entityId = entityId
        this.senderId = senderId
        this.senderName = senderName
        this.senderAvatar = senderAvatar
        this.actionUrl = actionUrl
        this.actionText = actionText
        this.imageUrl = imageUrl
        this.metadata = metadata?.let { com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(it) }
        this.priority = priority
        this.expiresAt = expiresAt
    }

    // 비즈니스 메소드
    fun markAsRead() {
        if (!this.isRead) {
            this.isRead = true
            this.readAt = java.time.LocalDateTime.now()
            this.addDomainEvent(NotificationDomainEvent.NotificationRead(id.value.toString(), recipientId.value.toString()))
        }
    }

    fun markAsSent() {
        if (!this.isSent) {
            this.isSent = true
            this.sentAt = java.time.LocalDateTime.now()
        }
    }

    fun markAsEmailSent() {
        if (!this.isEmailSent) {
            this.isEmailSent = true
        }
    }

    fun markAsPushSent() {
        if (!this.isPushSent) {
            this.isPushSent = true
        }
    }

    fun delete() {
        this.addDomainEvent(NotificationDomainEvent.NotificationDeleted(id.value.toString(), recipientId.value.toString()))
    }

    fun isExpired(): Boolean {
        return this.expiresAt?.let { it.isBefore(java.time.LocalDateTime.now()) } == true
    }

    fun shouldSendEmail(): Boolean {
        return !this.isEmailSent && this.priority != NotificationPriority.LOW
    }

    fun shouldSendPush(): Boolean {
        return !this.isPushSent && this.priority != NotificationPriority.LOW
    }

    fun getMetadataMap(): Map<String, Any> {
        return this.metadata?.let { 
            try {
                com.fasterxml.jackson.databind.ObjectMapper().readValue(it, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    }

    override fun toString(): String {
        return "Notification(id=${id}, recipientId=${recipientId.value}, type=${type.name}, isRead=$isRead)"
    }
}

// 알림 타입 Enum
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

// 엔티티 타입 Enum
enum class EntityType {
    POST,
    COMMENT,
    USER,
    SYSTEM,
    MESSAGE,
    ACHIEVEMENT
}

// 알림 우선순위 Enum
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}