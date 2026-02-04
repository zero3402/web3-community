package com.web3community.analytics.repository

import com.web3community.analytics.entity.UserAnalytics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface UserAnalyticsRepository : JpaRepository<UserAnalytics, Long> {
    
    fun findByUserIdAndDate(userId: Long, date: LocalDate): Optional<UserAnalytics>
    
    fun findByUserIdAndDateBetween(userId: Long, startDate: LocalDate, endDate: LocalDate): List<UserAnalytics>
    
    fun findByDate(date: LocalDate): List<UserAnalytics>
    
    fun findByDateBetween(startDate: LocalDate, endDate: LocalDate): List<UserAnalytics>
    
    @Query("SELECT COUNT(DISTINCT user_id) FROM user_analytics")
    fun countDistinctUsers(): Long
    
    @Query("SELECT COUNT(DISTINCT user_id) FROM user_analytics WHERE date >= :since")
    fun countActiveUsersSince(@Param("since") since: LocalDate): Long
    
    @Query("SELECT user_id, SUM(page_views) as totalViews FROM user_analytics WHERE date >= :since GROUP BY user_id ORDER BY totalViews DESC LIMIT :limit")
    fun getMostActiveUsers(@Param("since") since: LocalDate, @Param("limit") limit: Int): List<Array<Any>>
    
    @Query("SELECT date, SUM(page_views) as totalViews, COUNT(DISTINCT user_id) as uniqueUsers FROM user_analytics WHERE date >= :since GROUP BY date ORDER BY date")
    fun getDailyStats(@Param("since") since: LocalDate): List<Array<Any>>
    
    @Query("SELECT device_type, COUNT(DISTINCT user_id) FROM user_analytics GROUP BY device_type")
    fun getUsersByDeviceType(): Map<String, Long>
    
    @Query("SELECT country_code, COUNT(DISTINCT user_id) FROM user_analytics GROUP BY country_code")
    fun getUsersByCountry(): Map<String, Long>
}