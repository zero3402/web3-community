package com.web3community.notification.repository

import com.web3community.notification.entity.Notification
import com.web3community.notification.entity.NotificationType
import com.web3community.notification.entity.EntityType
import com.web3community.notification.entity.NotificationPriority
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    
    fun findByRecipientIdOrderByCreatedAtDesc(recipientId: Long, pageable: Pageable): Page<Notification>
    
    fun findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId: Long, pageable: Pageable): Page<Notification>
    
    fun findByRecipientIdAndIsSentFalseOrderByCreatedAtAsc(recipientId: Long): List<Notification>
    
    fun findByTypeAndRecipientIdOrderByCreatedAtDesc(type: NotificationType, recipientId: Long, pageable: Pageable): Page<Notification>
    
    fun findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType: EntityType, entityId: Long, pageable: Pageable): Page<Notification>
    
    fun findBySenderIdAndRecipientIdOrderByCreatedAtDesc(senderId: Long, recipientId: Long, pageable: Pageable): Page<Notification>
    
    fun findByPriorityAndIsSentFalseOrderByCreatedAtAsc(priority: NotificationPriority): List<Notification>
    
    @Query("""
        SELECT n FROM Notification n 
        WHERE (:recipientId IS NULL OR n.recipientId = :recipientId)
        AND (:type IS NULL OR n.type = :type)
        AND (:entityType IS NULL OR n.entityType = :entityType)
        AND (:entityId IS NULL OR n.entityId = :entityId)
        AND (:senderId IS NULL OR n.senderId = :senderId)
        AND (:priority IS NULL OR n.priority = :priority)
        AND (:isRead IS NULL OR n.isRead = :isRead)
        AND (:isSent IS NULL OR n.isSent = :isSent)
        AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom)
        AND (:dateTo IS NULL OR n.createdAt <= :dateTo)
        ORDER BY n.createdAt DESC
    """)
    fun findByMultipleCriteria(
        @Param("recipientId") recipientId: Long?,
        @Param("type") type: NotificationType?,
        @Param("entityType") entityType: EntityType?,
        @Param("entityId") entityId: Long?,
        @Param("senderId") senderId: Long?,
        @Param("priority") priority: NotificationPriority?,
        @Param("isRead") isRead: Boolean?,
        @Param("isSent") isSent: Boolean?,
        @Param("dateFrom") dateFrom: LocalDateTime?,
        @Param("dateTo") dateTo: LocalDateTime?,
        pageable: Pageable
    ): Page<Notification>
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = false")
    fun countUnreadByRecipientId(@Param("recipientId") recipientId: Long): Long
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId")
    fun countByRecipientId(@Param("recipientId") recipientId: Long): Long
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isSent = false")
    fun countUnsentNotifications(): Long
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isEmailSent = false AND n.priority != 'LOW'")
    fun countUnsentEmailNotifications(): Long
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isPushSent = false AND n.priority != 'LOW'")
    fun countUnsentPushNotifications(): Long
    
    @Query("""
        SELECT n.type, COUNT(n) as count 
        FROM Notification n 
        WHERE n.recipientId = :recipientId 
        GROUP BY n.type 
        ORDER BY count DESC
    """)
    fun getNotificationStatsByRecipientId(@Param("recipientId") recipientId: Long): List<Array<Any>>
    
    @Query("""
        SELECT n.type, COUNT(n) as count 
        FROM Notification n 
        WHERE n.createdAt >= :startDate AND n.createdAt <= :endDate
        GROUP BY n.type 
        ORDER BY count DESC
    """)
    fun getNotificationStatsByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>
    
    @Query("""
        SELECT DATE(n.createdAt) as date, COUNT(n) as count 
        FROM Notification n 
        WHERE n.createdAt >= :startDate 
        GROUP BY DATE(n.createdAt) 
        ORDER BY date DESC
    """)
    fun getDailyNotificationStats(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("SELECT n FROM Notification n WHERE n.expiresAt < :currentTime AND n.isRead = false")
    fun findExpiredNotifications(@Param("currentTime") currentTime: LocalDateTime): List<Notification>
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id = :notificationId")
    fun markAsRead(@Param("notificationId") notificationId: Long, @Param("readAt") readAt: LocalDateTime): Int
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipientId = :recipientId AND n.isRead = false")
    fun markAllAsReadByRecipientId(@Param("recipientId") recipientId: Long, @Param("readAt") readAt: LocalDateTime): Int
    
    @Modifying
    @Query("UPDATE Notification n SET n.isSent = true, n.sentAt = :sentAt WHERE n.id = :notificationId")
    fun markAsSent(@Param("notificationId") notificationId: Long, @Param("sentAt") sentAt: LocalDateTime): Int
    
    @Modifying
    @Query("UPDATE Notification n SET n.isEmailSent = true WHERE n.id = :notificationId")
    fun markAsEmailSent(@Param("notificationId") notificationId: Long): Int
    
    @Modifying
    @Query("UPDATE Notification n SET n.isPushSent = true WHERE n.id = :notificationId")
    fun markAsPushSent(@Param("notificationId") notificationId: Long): Int
    
    @Query("SELECT n FROM Notification n WHERE n.priority = :priority AND n.isSent = false ORDER BY n.createdAt ASC")
    fun findNotificationsByPriority(@Param("priority") priority: NotificationPriority): List<Notification>
    
    fun findByPriorityAndIsSentFalseAndExpiresAtAfterOrderByCreatedAtAsc(
        priority: NotificationPriority, 
        currentTime: LocalDateTime
    ): List<Notification>
    
    @Query("""
        SELECT n.recipientId, COUNT(n) as unreadCount 
        FROM Notification n 
        WHERE n.isRead = false 
        GROUP BY n.recipientId 
        ORDER BY unreadCount DESC
    """)
    fun getTopUsersWithUnreadNotifications(): List<Array<Any>>
    
    @Query("""
        SELECT n.senderId, COUNT(n) as sentCount 
        FROM Notification n 
        WHERE n.senderId IS NOT NULL 
        GROUP BY n.senderId 
        ORDER BY sentCount DESC
    """)
    fun getTopNotificationSenders(): List<Array<Any>>
}