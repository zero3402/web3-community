package com.web3community.notification.repository

import com.web3community.notification.entity.NotificationTemplate
import com.web3community.notification.entity.NotificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationTemplateRepository : JpaRepository<NotificationTemplate, Long> {
    
    fun findByTypeAndIsActiveTrue(type: NotificationType): Optional<NotificationTemplate>
    
    fun findByType(type: NotificationType): List<NotificationTemplate>
    
    fun findByIsActiveTrue(): List<NotificationTemplate>
    
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.type = :type AND nt.isActive = true")
    fun findActiveTemplateByType(@Param("type") type: NotificationType): Optional<NotificationTemplate>
    
    @Query("""
        SELECT nt.type, COUNT(nt) as templateCount 
        FROM NotificationTemplate nt 
        WHERE nt.isActive = true 
        GROUP BY nt.type 
        ORDER BY templateCount DESC
    """)
    fun getTemplateTypeStats(): List<Array<Any>>
    
    @Query("SELECT COUNT(nt) FROM NotificationTemplate nt WHERE nt.isActive = true")
    fun countActiveTemplates(): Long
    
    @Query("SELECT nt.type FROM NotificationTemplate nt WHERE nt.isActive = true")
    fun findActiveNotificationTypes(): List<NotificationType>
}