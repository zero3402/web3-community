package com.web3community.notification.service

import com.web3community.notification.dto.WebSocketNotificationMessage
import com.web3community.notification.entity.Notification
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WebSocketService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper
) {

    fun sendNotification(userId: Long, notification: Notification) {
        try {
            val message = WebSocketNotificationMessage(
                type = "notification",
                notification = toNotificationResponse(notification),
                timestamp = LocalDateTime.now().toString(),
                recipientId = userId
            )
            
            // Send to user-specific queue
            messagingTemplate.convertAndSend("/queue/notifications/$userId", message)
            
            // Also send to user topic for real-time updates
            messagingTemplate.convertAndSend("/topic/user/$userId/notifications", message)
            
        } catch (e: Exception) {
            // Log error but don't fail the notification
            println("Failed to send WebSocket notification: ${e.message}")
        }
    }

    fun sendNotificationCount(userId: Long, unreadCount: Long) {
        try {
            val message = mapOf(
                "type" to "notification_count",
                "unreadCount" to unreadCount,
                "timestamp" to LocalDateTime.now().toString(),
                "recipientId" to userId
            )
            
            messagingTemplate.convertAndSend("/queue/notifications/$userId", message)
            
        } catch (e: Exception) {
            println("Failed to send notification count: ${e.message}")
        }
    }

    fun sendSystemNotification(message: Map<String, Any>) {
        try {
            messagingTemplate.convertAndSend("/topic/system/notifications", message)
        } catch (e: Exception) {
            println("Failed to send system notification: ${e.message}")
        }
    }

    private fun toNotificationResponse(notification: com.web3community.notification.entity.Notification): com.web3community.notification.dto.NotificationResponse {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        val metadata = notification.metadata?.let { 
            try {
                objectMapper.readValue(it, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap<String, Any>()
            }
        }
        
        return com.web3community.notification.dto.NotificationResponse(
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