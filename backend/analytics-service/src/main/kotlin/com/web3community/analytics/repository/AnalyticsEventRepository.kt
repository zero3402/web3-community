package com.web3community.analytics.repository

import com.web3community.analytics.entity.AnalyticsEvent
import com.web3community.analytics.entity.EventType
import com.web3community.analytics.entity.DeviceType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AnalyticsEventRepository : JpaRepository<AnalyticsEvent, Long> {
    
    fun findByUserIdOrderByTimestampDesc(userId: Long, pageable: Pageable): Page<AnalyticsEvent>
    
    fun findByEventTypeOrderByTimestampDesc(eventType: EventType, pageable: Pageable): Page<AnalyticsEvent>
    
    fun findAllByOrderByTimestampDesc(pageable: Pageable): Page<AnalyticsEvent>
    
    fun findByEventType(eventType: EventType): List<AnalyticsEvent>
    
    fun findByUserIdAndTimestampBetween(userId: Long, startTime: LocalDateTime, endTime: LocalDateTime): List<AnalyticsEvent>
    
    fun findByEventTypeAndTimestampBetween(eventType: EventType, startTime: LocalDateTime, endTime: LocalDateTime): List<AnalyticsEvent>
    
    fun findByTimestampBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<AnalyticsEvent>
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE event_type = :eventType")
    fun countByEventType(@Param("eventType") eventType: EventType): Long
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE event_type = :eventType AND timestamp >= :since")
    fun countByEventTypeSince(@Param("eventType") eventType: EventType, @Param("since") since: LocalDateTime): Long
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE timestamp >= :since")
    fun countEventsSince(@Param("since") since: LocalDateTime): Long
    
    @Query("SELECT device_type, COUNT(*) FROM analytics_events GROUP BY device_type")
    fun countByDeviceType(): Map<DeviceType?, Long>
    
    @Query("SELECT country_code, COUNT(*) FROM analytics_events GROUP BY country_code")
    fun countByCountryCode(): Map<String?, Long>
    
    @Query("SELECT DATE(timestamp), COUNT(*) FROM analytics_events WHERE timestamp >= :since GROUP BY DATE(timestamp) ORDER BY DATE(timestamp)")
    fun getDailyEventCounts(@Param("since") since: LocalDateTime): List<Array<Any>>
    
    @Query("SELECT user_id, COUNT(*) FROM analytics_events WHERE user_id IS NOT NULL GROUP BY user_id ORDER BY COUNT(*) DESC")
    fun getTopUsers(): List<Array<Any>>
    
    @Query("SELECT page_url, COUNT(*) FROM analytics_events WHERE page_url IS NOT NULL GROUP BY page_url ORDER BY COUNT(*) DESC")
    fun getTopPages(): List<Array<Any>>
    
    @Query("""
        SELECT event_type, COUNT(*) as count 
        FROM analytics_events 
        WHERE timestamp >= :startDate AND timestamp <= :endDate
        GROUP BY event_type 
        ORDER BY count DESC
    """)
    fun getEventTypeStats(@Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime): List<Array<Any>>
    
    @Query("SELECT DISTINCT user_id FROM analytics_events WHERE user_id IS NOT NULL")
    fun findDistinctUserIds(): List<Long>
    
    @Query("SELECT COUNT(DISTINCT user_id) FROM analytics_events WHERE timestamp >= :since")
    fun countDistinctUsersSince(@Param("since") since: LocalDateTime): Long
}