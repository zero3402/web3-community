package com.web3community.notification.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class NotificationCreateRequest(
    @field:NotNull(message = "Recipient ID is required")
    val recipientId: Long,
    
    @field:NotNull(message = "Type is required")
    val type: NotificationType,
    
    @field:NotBlank(message = "Title is required")
    val title: String,
    
    @field:NotBlank(message = "Message is required")
    val message: String,
    
    val entityType: EntityType? = null,
    val entityId: Long? = null,
    val senderId: Long? = null,
    val senderName: String? = null,
    val senderAvatar: String? = null,
    val actionUrl: String? = null,
    val actionText: String? = null,
    val imageUrl: String? = null,
    val metadata: Map<String, Any>? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val expiresAt: String? = null
)

data class NotificationUpdateRequest(
    val isRead: Boolean? = null,
    val priority: NotificationPriority? = null,
    val expiresAt: String? = null
)

data class NotificationResponse(
    val id: Long,
    val recipientId: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val entityType: EntityType? = null,
    val entityId: Long? = null,
    val senderId: Long? = null,
    val senderName: String? = null,
    val senderAvatar: String? = null,
    val actionUrl: String? = null,
    val actionText: String? = null,
    val imageUrl: String? = null,
    val metadata: Map<String, Any>? = null,
    val priority: NotificationPriority,
    val isRead: Boolean,
    val isSent: Boolean,
    val isEmailSent: Boolean,
    val isPushSent: Boolean,
    val readAt: String? = null,
    val sentAt: String? = null,
    val expiresAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val isExpired: Boolean = false
)

data class NotificationSearchRequest(
    val recipientId: Long? = null,
    val type: NotificationType? = null,
    val entityType: EntityType? = null,
    val entityId: Long? = null,
    val senderId: Long? = null,
    val priority: NotificationPriority? = null,
    val isRead: Boolean? = null,
    val isSent: Boolean? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val sortBy: NotificationSortBy = NotificationSortBy.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC
)

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val pagination: PaginationResponse
)

data class NotificationStatsResponse(
    val totalNotifications: Long,
    val unreadNotifications: Long,
    val sentNotifications: Long,
    val failedNotifications: Long,
    val emailNotifications: Long,
    val pushNotifications: Long,
    val recentActivity: List<NotificationActivity>,
    val notificationTypes: Map<String, Long>,
    val dailyStats: List<DailyNotificationStats>
)

data class NotificationActivity(
    val type: String,
    val count: Long,
    val percentage: Double
)

data class DailyNotificationStats(
    val date: String,
    val total: Long,
    val sent: Long,
    val failed: Long
)

data class BulkNotificationRequest(
    @field:NotNull(message = "Recipients are required")
    val recipientIds: List<Long>,
    
    @field:NotNull(message = "Type is required")
    val type: NotificationType,
    
    @field:NotBlank(message = "Title is required")
    val title: String,
    
    @field:NotBlank(message = "Message is required")
    val message: String,
    
    val entityType: EntityType? = null,
    val entityId: Long? = null,
    val senderId: Long? = null,
    val senderName: String? = null,
    val senderAvatar: String? = null,
    val actionUrl: String? = null,
    val actionText: String? = null,
    val imageUrl: String? = null,
    val metadata: Map<String, Any>? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val scheduleAt: String? = null,
    val expiresAt: String? = null
)

data class BulkNotificationResponse(
    val totalRequests: Int,
    val successful: Int,
    val failed: Int,
    val failedIds: List<Long>,
    val errors: List<String>
)

data class NotificationPreferenceRequest(
    @field:NotNull(message = "Type is required")
    val type: NotificationPreferenceType,
    
    @field:NotNull(message = "Enabled status is required")
    val isEnabled: Boolean,
    
    val emailEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val inAppEnabled: Boolean = true
)

data class NotificationPreferenceResponse(
    val id: Long,
    val userId: Long,
    val type: NotificationPreferenceType,
    val isEnabled: Boolean,
    val emailEnabled: Boolean,
    val pushEnabled: Boolean,
    val inAppEnabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class WebSocketNotificationMessage(
    val type: String = "notification",
    val notification: NotificationResponse,
    val timestamp: String,
    val recipientId: Long
)

data class PaginationResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

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

enum class NotificationSortBy {
    CREATED_AT,
    UPDATED_AT,
    PRIORITY,
    READ_AT,
    SENT_AT
}

enum class SortDirection {
    ASC,
    DESC
}

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