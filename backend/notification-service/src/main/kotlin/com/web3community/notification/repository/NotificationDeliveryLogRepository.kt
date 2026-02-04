package com.web3community.notification.repository

import com.web3community.notification.entity.NotificationDeliveryLog
import com.web3community.notification.entity.NotificationChannel
import com.web3community.notification.entity.DeliveryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationDeliveryLogRepository : JpaRepository<NotificationDeliveryLog, Long> {
    
    fun findByNotificationId(notificationId: Long): List<NotificationDeliveryLog>
    
    fun findByChannel(channel: NotificationChannel, pageable: Pageable): Page<NotificationDeliveryLog>
    
    fun findByDeliveryStatus(deliveryStatus: DeliveryStatus, pageable: Pageable): Page<NotificationDeliveryLog>
    
    fun findByRecipientIdAndChannel(recipientId: Long, channel: NotificationChannel, pageable: Pageable): Page<NotificationDeliveryLog>
    
    fun findByNotificationIdAndChannel(notificationId: Long, channel: NotificationChannel): Optional<NotificationDeliveryLog>
    
    @Query("""
        SELECT ndl.channel, ndl.deliveryStatus, COUNT(ndl) as count 
        FROM NotificationDeliveryLog ndl 
        WHERE ndl.createdAt >= :startDate 
        GROUP BY ndl.channel, ndl.deliveryStatus 
        ORDER BY count DESC
    """)
    fun getDeliveryStatsByDateRange(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("""
        SELECT ndl.channel, ndl.deliveryStatus, COUNT(ndl) as count 
        FROM NotificationDeliveryLog ndl 
        GROUP BY ndl.channel, ndl.deliveryStatus 
        ORDER BY count DESC
    """)
    fun getOverallDeliveryStats(): List<Array<Any>>
    
    @Query("""
        SELECT DATE(ndl.createdAt) as date, ndl.channel, ndl.deliveryStatus, COUNT(ndl) as count 
        FROM NotificationDeliveryLog ndl 
        WHERE ndl.createdAt >= :startDate 
        GROUP BY DATE(ndl.createdAt), ndl.channel, ndl.deliveryStatus 
        ORDER BY date DESC, count DESC
    """)
    fun getDailyDeliveryStats(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("SELECT COUNT(ndl) FROM NotificationDeliveryLog ndl WHERE ndl.notificationId = :notificationId AND ndl.deliveryStatus = 'FAILED'")
    fun countFailedDeliveriesByNotificationId(@Param("notificationId") notificationId: Long): Long
    
    @Query("SELECT COUNT(ndl) FROM NotificationDeliveryLog ndl WHERE ndl.notificationId = :notificationId AND ndl.deliveryStatus = 'DELIVERED'")
    fun countSuccessfulDeliveriesByNotificationId(@Param("notificationId") notificationId: Long): Long
    
    @Query("SELECT ndl FROM NotificationDeliveryLog ndl WHERE ndl.deliveryStatus = 'FAILED' AND ndl.attemptCount < 3 ORDER BY ndl.createdAt ASC")
    fun findRetryableFailedDeliveries(): List<NotificationDeliveryLog>
    
    @Query("SELECT ndl.externalId FROM NotificationDeliveryLog ndl WHERE ndl.notificationId = :notificationId AND ndl.externalId IS NOT NULL")
    fun findExternalIdsByNotificationId(@Param("notificationId") notificationId: Long): List<String>
    
    @Query("SELECT ndl.recipientId FROM NotificationDeliveryLog ndl WHERE ndl.channel = :channel AND ndl.deliveryStatus = 'DELIVERED' GROUP BY ndl.recipientId")
    fun findDeliveredRecipientsByChannel(@Param("channel") channel: NotificationChannel): List<Long>
    
    @Query("""
        SELECT ndl.recipientId, COUNT(ndl) as deliveryCount 
        FROM NotificationDeliveryLog ndl 
        WHERE ndl.deliveryStatus = 'DELIVERED' 
        GROUP BY ndl.recipientId 
        ORDER BY deliveryCount DESC
    """)
    fun getTopDeliveredRecipients(): List<Array<Any>>
    
    @Query("SELECT COUNT(ndl) FROM NotificationDeliveryLog ndl WHERE ndl.attemptCount > 1 AND ndl.deliveryStatus = 'FAILED'")
    fun countMultipleFailedAttempts(): Long
    
    @Query("SELECT ndl FROM NotificationDeliveryLog ndl WHERE ndl.recipientId = :recipientId ORDER BY ndl.createdAt DESC")
    fun findByRecipientIdOrderByCreatedAtDesc(@Param("recipientId") recipientId: Long): List<NotificationDeliveryLog>
    
    fun existsByNotificationIdAndChannel(notificationId: Long, channel: NotificationChannel): Boolean
}