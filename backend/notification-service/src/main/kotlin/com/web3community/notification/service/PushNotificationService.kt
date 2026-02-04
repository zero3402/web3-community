package com.web3community.notification.service

import com.web3community.notification.entity.Notification
import com.web3community.notification.entity.NotificationDeliveryLog
import com.web3community.notification.entity.NotificationChannel
import com.web3community.notification.entity.DeliveryStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PushNotificationService(
    private val notificationDeliveryLogRepository: NotificationDeliveryLogRepository
) {

    fun sendPush(notification: Notification) {
        try {
            // This would integrate with a push notification service like Firebase Cloud Messaging
            // For now, we'll simulate the process
            
            val pushPayload = createPushPayload(notification)
            val deviceTokens = getDeviceTokens(notification.recipientId)
            
            deviceTokens.forEach { token ->
                sendPushToToken(token, pushPayload)
            }
            
            // Log successful delivery
            logDelivery(notification, NotificationChannel.PUSH, DeliveryStatus.SENT, deviceTokens.joinToString(","))
            
        } catch (e: Exception) {
            // Log failed delivery
            logDelivery(notification, NotificationChannel.PUSH, DeliveryStatus.FAILED, null, e.message)
            throw e
        }
    }

    private fun createPushPayload(notification: Notification): Map<String, Any> {
        return mapOf(
            "title" to notification.title,
            "body" to notification.message,
            "icon" to (notification.senderAvatar ?: "default_icon.png"),
            "image" to notification.imageUrl,
            "click_action" to notification.actionUrl,
            "data" to mapOf(
                "notificationId" to (notification.id ?: 0),
                "type" to notification.type.name,
                "entityType" to notification.entityType?.name,
                "entityId" to notification.entityId,
                "senderId" to notification.senderId
            ),
            "priority" to when (notification.priority) {
                com.web3community.notification.entity.NotificationPriority.URGENT -> "high"
                com.web3community.notification.entity.NotificationPriority.HIGH -> "high"
                com.web3community.notification.entity.NotificationPriority.NORMAL -> "normal"
                com.web3community.notification.entity.NotificationPriority.LOW -> "normal"
            }
        )
    }

    private fun getDeviceTokens(userId: Long): List<String> {
        // This would fetch device tokens from a user device repository
        // For simulation, return empty list
        return emptyList()
    }

    private fun sendPushToToken(token: String, payload: Map<String, Any>) {
        // This would send actual push notification using FCM or other service
        println("Sending push notification to token: $token")
        println("Payload: $payload")
    }

    private fun logDelivery(
        notification: Notification,
        channel: NotificationChannel,
        status: DeliveryStatus,
        recipientAddress: String?,
        errorMessage: String? = null
    ) {
        val log = NotificationDeliveryLog(
            notificationId = notification.id ?: 0,
            channel = channel,
            recipientId = notification.recipientId,
            recipientAddress = recipientAddress,
            deliveryStatus = status,
            sentAt = if (status == DeliveryStatus.SENT) LocalDateTime.now() else null,
            failedAt = if (status == DeliveryStatus.FAILED) LocalDateTime.now() else null,
            errorMessage = errorMessage
        )
        
        notificationDeliveryLogRepository.save(log)
    }
}