package com.web3community.notification.repository

import com.web3community.notification.entity.NotificationPreference
import com.web3community.notification.entity.NotificationPreferenceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    
    fun findByUserIdAndType(userId: Long, type: NotificationPreferenceType): Optional<NotificationPreference>
    
    fun findByUserId(userId: Long): List<NotificationPreference>
    
    fun findByUserIdAndIsEnabledTrue(userId: Long): List<NotificationPreference>
    
    fun findByTypeAndIsEnabledTrue(type: NotificationPreferenceType): List<NotificationPreference>
    
    @Query("SELECT np FROM NotificationPreference np WHERE np.userId = :userId AND np.emailEnabled = true")
    fun findByUserIdAndEmailEnabled(@Param("userId") userId: Long): List<NotificationPreference>
    
    @Query("SELECT np FROM NotificationPreference np WHERE np.userId = :userId AND np.pushEnabled = true")
    fun findByUserIdAndPushEnabled(@Param("userId") userId: Long): List<NotificationPreference>
    
    @Query("SELECT np FROM NotificationPreference np WHERE np.userId = :userId AND np.inAppEnabled = true")
    fun findByUserIdAndInAppEnabled(@Param("userId") userId: Long): List<NotificationPreference>
    
    @Query("SELECT np.type FROM NotificationPreference np WHERE np.userId = :userId AND np.isEnabled = true")
    fun findEnabledNotificationTypesByUserId(@Param("userId") userId: Long): List<NotificationPreferenceType>
    
    @Query("""
        SELECT np.type, COUNT(np) as userCount 
        FROM NotificationPreference np 
        WHERE np.isEnabled = true 
        GROUP BY np.type 
        ORDER BY userCount DESC
    """)
    fun getNotificationTypeStats(): List<Array<Any>>
    
    @Query("SELECT COUNT(np) FROM NotificationPreference np WHERE np.userId = :userId AND np.isEnabled = true")
    fun countEnabledPreferencesByUserId(@Param("userId") userId: Long): Long
    
    fun existsByUserIdAndType(userId: Long, type: NotificationPreferenceType): Boolean
}