package com.web3community.notification.service

import com.web3community.notification.dto.NotificationCreateRequest
import com.web3community.notification.dto.NotificationResponse
import com.web3community.notification.entity.Notification
import com.web3community.notification.repository.NotificationRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    fun createNotification(request: NotificationCreateRequest): Mono<NotificationResponse> {
        val notification = Notification(
            userId = request.userId,
            title = request.title,
            message = request.message,
            type = request.type,
            relatedId = request.relatedId
        )

        return notificationRepository.save(notification)
            .doOnSuccess { saved ->
                sendNotificationEvent(saved)
            }
            .map { toResponse(it) }
    }

    fun getUserNotifications(userId: Long): Flux<NotificationResponse> {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .map { toResponse(it) }
    }

    fun getUnreadNotifications(userId: Long): Flux<NotificationResponse> {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false)
            .map { toResponse(it) }
    }

    fun getUnreadCount(userId: Long): Mono<Long> {
        return notificationRepository.countByUserIdAndIsRead(userId, false)
    }

    fun markAsRead(notificationId: String): Mono<NotificationResponse> {
        return notificationRepository.findById(notificationId)
            .switchIfEmpty(Mono.error(IllegalArgumentException("Notification not found")))
            .flatMap { notification ->
                val updatedNotification = notification.copy(isRead = true)
                notificationRepository.save(updatedNotification)
                    .map { toResponse(it) }
            }
    }

    fun markAllAsRead(userId: Long): Mono<String> {
        return notificationRepository.findByUserIdAndIsRead(userId, false)
            .flatMap { notification ->
                val updatedNotification = notification.copy(isRead = true)
                notificationRepository.save(updatedNotification)
            }
            .then(Mono.just("All notifications marked as read"))
    }

    private fun sendNotificationEvent(notification: Notification) {
        try {
            val event = mapOf(
                "type" to "notification.created",
                "notificationId" to (notification.id ?: ""),
                "userId" to notification.userId,
                "title" to notification.title,
                "message" to notification.message,
                "notificationType" to notification.type.name
            )
            kafkaTemplate.send("notifications", event)
        } catch (e: Exception) {
            println("Failed to send notification to Kafka: ${e.message}")
        }
    }

    private fun toResponse(notification: Notification): NotificationResponse {
        return NotificationResponse(
            id = notification.id ?: "",
            userId = notification.userId,
            title = notification.title,
            message = notification.message,
            type = notification.type,
            isRead = notification.isRead,
            relatedId = notification.relatedId,
            createdAt = notification.createdAt.toString()
        )
    }
}