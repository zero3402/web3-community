package com.web3community.notification.entity

import jakarta.persistence.*

@Entity
@Table(name = "notification_delivery_logs")
data class NotificationDeliveryLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "notification_id", nullable = false)
    val notificationId: Long,
    
    @Column(nullable = false)
    val channel: NotificationChannel,
    
    @Column(name = "recipient_id", nullable = false)
    val recipientId: Long,
    
    @Column(name = "recipient_address")
    val recipientAddress: String? = null,
    
    @Column(name = "delivery_status", nullable = false)
    val deliveryStatus: DeliveryStatus,
    
    @Column(name = "attempt_count", nullable = false)
    val attemptCount: Int = 1,
    
    @Column(name = "sent_at")
    val sentAt: java.time.LocalDateTime? = null,
    
    @Column(name = "delivered_at")
    val deliveredAt: java.time.LocalDateTime? = null,
    
    @Column(name = "failed_at")
    val failedAt: java.time.LocalDateTime? = null,
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,
    
    @Column(name = "error_code")
    val errorCode: String? = null,
    
    @Column(name = "external_id")
    val externalId: String? = null,
    
    @Column(name = "response_data", columnDefinition = "JSON")
    val responseData: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

enum class NotificationChannel {
    IN_APP,
    EMAIL,
    PUSH,
    SMS,
    WEBHOOK
}

enum class DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    BOUNCED,
    REJECTED,
    OPENED,
    CLICKED
}