package com.web3community.notification.service

import com.web3community.notification.dto.*
import com.web3community.notification.entity.*
import com.web3community.notification.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
    private val notificationTemplateRepository: NotificationTemplateRepository,
    private val notificationDeliveryLogRepository: NotificationDeliveryLogRepository,
    private val emailNotificationService: EmailNotificationService,
    private val pushNotificationService: PushNotificationService,
    private val webSocketService: WebSocketService,
    private val objectMapper: ObjectMapper
) {

    @CacheEvict(value = ["notifications", "notification-stats"], allEntries = true)
    fun createNotification(request: NotificationCreateRequest): NotificationResponse {
        val notification = Notification(
            recipientId = request.recipientId,
            type = request.type,
            title = request.title.trim(),
            message = request.message.trim(),
            entityType = request.entityType,
            entityId = request.entityId,
            senderId = request.senderId,
            senderName = request.senderName,
            senderAvatar = request.senderAvatar,
            actionUrl = request.actionUrl,
            actionText = request.actionText,
            imageUrl = request.imageUrl,
            metadata = request.metadata?.let { objectMapper.writeValueAsString(it) },
            priority = request.priority,
            expiresAt = request.expiresAt?.let { LocalDateTime.parse(it) }
        )
        
        val savedNotification = notificationRepository.save(notification)
        
        // Process notification asynchronously
        processNotification(savedNotification)
        
        return toNotificationResponse(savedNotification)
    }

    @CacheEvict(value = ["notifications", "notification-stats"], allEntries = true)
    fun createBulkNotifications(request: BulkNotificationRequest): BulkNotificationResponse {
        val results = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        var successful = 0
        var failed = 0
        
        request.recipientIds.forEach { recipientId ->
            try {
                val notificationRequest = NotificationCreateRequest(
                    recipientId = recipientId,
                    type = request.type,
                    title = request.title,
                    message = request.message,
                    entityType = request.entityType,
                    entityId = request.entityId,
                    senderId = request.senderId,
                    senderName = request.senderName,
                    senderAvatar = request.senderAvatar,
                    actionUrl = request.actionUrl,
                    actionText = request.actionText,
                    imageUrl = request.imageUrl,
                    metadata = request.metadata,
                    priority = request.priority,
                    expiresAt = request.expiresAt
                )
                
                val notification = createNotification(notificationRequest)
                results.add(notification.id)
                successful++
            } catch (e: Exception) {
                errors.add("Failed to create notification for user $recipientId: ${e.message}")
                failed++
            }
        }
        
        return BulkNotificationResponse(
            totalRequests = request.recipientIds.size,
            successful = successful,
            failed = failed,
            failedIds = emptyList(), // Would need to track failed recipient IDs separately
            errors = errors
        )
    }

    @CacheEvict(value = ["notifications"], key = "#notificationId")
    fun updateNotification(notificationId: Long, request: NotificationUpdateRequest, userId: Long): NotificationResponse {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notification not found") }
            
        if (notification.recipientId != userId) {
            throw IllegalArgumentException("Not authorized to update this notification")
        }
        
        val updatedNotification = notification.copy(
            isRead = request.isRead ?: notification.isRead,
            readAt = if (request.isRead == true) LocalDateTime.now() else notification.readAt,
            priority = request.priority ?: notification.priority,
            expiresAt = request.expiresAt?.let { LocalDateTime.parse(it) } ?: notification.expiresAt,
            updatedAt = LocalDateTime.now()
        )
        
        val savedNotification = notificationRepository.save(updatedNotification)
        return toNotificationResponse(savedNotification)
    }

    fun getNotificationById(notificationId: Long, userId: Long): NotificationResponse {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notification not found") }
            
        if (notification.recipientId != userId) {
            throw IllegalArgumentException("Not authorized to view this notification")
        }
        
        return toNotificationResponse(notification)
    }

    @Cacheable(value = ["notifications"], key = "#recipientId + '-' + #page + '-' + #size + '-unread-' + #unreadOnly")
    fun getUserNotifications(
        recipientId: Long,
        page: Int = 0,
        size: Int = 20,
        unreadOnly: Boolean = false
    ): NotificationListResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pageResult = if (unreadOnly) {
            notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId, pageable)
        } else {
            notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
        }
        
        val notifications = pageResult.content.map { toNotificationResponse(it) }
        
        return NotificationListResponse(
            notifications = notifications,
            pagination = PaginationResponse(
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext(),
                hasPrevious = pageResult.hasPrevious()
            )
        )
    }

    fun searchNotifications(request: NotificationSearchRequest, pageable: Pageable): NotificationListResponse {
        val page = notificationRepository.findByMultipleCriteria(
            recipientId = request.recipientId,
            type = request.type,
            entityType = request.entityType,
            entityId = request.entityId,
            senderId = request.senderId,
            priority = request.priority,
            isRead = request.isRead,
            isSent = request.isSent,
            dateFrom = request.dateFrom?.let { LocalDateTime.parse(it) },
            dateTo = request.dateTo?.let { LocalDateTime.parse(it) },
            pageable = pageable
        )
        
        val notifications = page.content.map { toNotificationResponse(it) }
        
        return NotificationListResponse(
            notifications = notifications,
            pagination = PaginationResponse(
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious()
            )
        )
    }

    fun markAsRead(notificationId: Long, userId: Long): NotificationResponse {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notification not found") }
            
        if (notification.recipientId != userId) {
            throw IllegalArgumentException("Not authorized to update this notification")
        }
        
        if (!notification.isRead) {
            notificationRepository.markAsRead(notificationId, LocalDateTime.now())
        }
        
        return getNotificationById(notificationId, userId)
    }

    fun markAllAsRead(userId: Long): String {
        val updatedCount = notificationRepository.markAllAsReadByRecipientId(userId, LocalDateTime.now())
        return "Marked $updatedCount notifications as read"
    }

    fun deleteNotification(notificationId: Long, userId: Long): String {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notification not found") }
            
        if (notification.recipientId != userId) {
            throw IllegalArgumentException("Not authorized to delete this notification")
        }
        
        notificationRepository.delete(notification)
        return "Notification deleted successfully"
    }

    fun getNotificationStats(userId: Long): NotificationStatsResponse {
        val totalNotifications = notificationRepository.countByRecipientId(userId)
        val unreadNotifications = notificationRepository.countUnreadByRecipientId(userId)
        
        val recentActivity = notificationRepository.getNotificationStatsByRecipientId(userId)
            .map { row ->
                NotificationActivity(
                    type = row[0].toString(),
                    count = (row[1] as Number).toLong(),
                    percentage = if (totalNotifications > 0) (row[1] as Number).toDouble() / totalNotifications * 100 else 0.0
                )
            }
        
        val dailyStats = notificationRepository.getDailyNotificationStats(LocalDateTime.now().minusDays(30))
            .map { row ->
                DailyNotificationStats(
                    date = row[0].toString(),
                    total = (row[1] as Number).toLong(),
                    sent = (row[1] as Number).toLong(), // Simplified
                    failed = 0L // Simplified
                )
            }
        
        return NotificationStatsResponse(
            totalNotifications = totalNotifications,
            unreadNotifications = unreadNotifications,
            sentNotifications = 0L, // Would need to calculate
            failedNotifications = 0L, // Would need to calculate
            emailNotifications = 0L, // Would need to calculate
            pushNotifications = 0L, // Would need to calculate
            recentActivity = recentActivity,
            notificationTypes = recentActivity.associate { it.type to it.count },
            dailyStats = dailyStats
        )
    }

    @Async
    private fun processNotification(notification: Notification) {
        // Check user preferences
        val preferences = getNotificationPreferences(notification.recipientId, notification.type)
        
        if (preferences?.inAppEnabled == true) {
            // Send via WebSocket
            webSocketService.sendNotification(notification.recipientId, notification)
        }
        
        if (preferences?.emailEnabled == true && notification.shouldSendEmail()) {
            // Send email
            emailNotificationService.sendEmail(notification)
        }
        
        if (preferences?.pushEnabled == true && notification.shouldSendPush()) {
            // Send push notification
            pushNotificationService.sendPush(notification)
        }
        
        // Mark as sent
        notificationRepository.markAsSent(notification.id ?: 0, LocalDateTime.now())
    }

    private fun getNotificationPreferences(userId: Long, type: NotificationType): NotificationPreference? {
        val preferenceType = when (type) {
            NotificationType.POST_LIKE -> NotificationPreferenceType.POST_LIKES
            NotificationType.POST_COMMENT -> NotificationPreferenceType.POST_COMMENTS
            NotificationType.COMMENT_REPLY -> NotificationPreferenceType.COMMENT_REPLIES
            NotificationType.COMMENT_LIKE -> NotificationPreferenceType.COMMENT_LIKES
            NotificationType.FOLLOW -> NotificationPreferenceType.FOLLOWS
            NotificationType.MENTION -> NotificationPreferenceType.MENTIONS
            NotificationType.POST_PUBLISHED -> NotificationPreferenceType.POST_PUBLISHED
            NotificationType.COMMENT_PINNED -> NotificationPreferenceType.COMMENT_PINNED
            NotificationType.SYSTEM_ANNOUNCEMENT -> NotificationPreferenceType.SYSTEM_ANNOUNCEMENTS
            NotificationType.NEW_MESSAGE -> NotificationPreferenceType.MESSAGES
            else -> null
        }
        
        return preferenceType?.let { 
            notificationPreferenceRepository.findByUserIdAndType(userId, it).orElse(null)
        }
    }

    private fun toNotificationResponse(notification: Notification): NotificationResponse {
        val metadata = notification.metadata?.let { 
            try {
                objectMapper.readValue(it, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap<String, Any>()
            }
        }
        
        return NotificationResponse(
            id = notification.id ?: 0,
            recipientId = notification.recipientId,
            type = notification.type,
            title = notification.title,
            message = notification.message,
            entityType = notification.entityType,
            entityId = notification.entityId,
            senderId = notification.senderId,
            senderName = notification.senderName,
            senderAvatar = notification.senderAvatar,
            actionUrl = notification.actionUrl,
            actionText = notification.actionText,
            imageUrl = notification.imageUrl,
            metadata = metadata,
            priority = notification.priority,
            isRead = notification.isRead,
            isSent = notification.isSent,
            isEmailSent = notification.isEmailSent,
            isPushSent = notification.isPushSent,
            readAt = notification.readAt?.toString(),
            sentAt = notification.sentAt?.toString(),
            expiresAt = notification.expiresAt?.toString(),
            createdAt = notification.createdAt.toString(),
            updatedAt = notification.updatedAt.toString(),
            isExpired = notification.isExpired()
        )
    }
}