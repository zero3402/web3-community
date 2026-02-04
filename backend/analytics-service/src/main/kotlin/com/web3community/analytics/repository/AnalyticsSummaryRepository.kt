package com.web3community.analytics.repository

import com.web3community.analytics.entity.AnalyticsSummary
import com.web3community.analytics.entity.MetricType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AnalyticsSummaryRepository : JpaRepository<AnalyticsSummary, Long> {
    
    fun findByDateAndMetricType(date: LocalDate, metricType: MetricType): Optional<AnalyticsSummary>
    
    fun findByMetricTypeOrderByDateDesc(metricType: MetricType): List<AnalyticsSummary>
    
    fun findByDateBetween(startDate: LocalDate, endDate: LocalDate): List<AnalyticsSummary>
    
    fun findByDateBetweenAndMetricType(startDate: LocalDate, endDate: LocalDate, metricType: MetricType): List<AnalyticsSummary>
    
    @Query("SELECT SUM(total_count) FROM analytics_summary WHERE metric_type = :metricType AND date >= :startDate AND date <= :endDate")
    fun sumTotalCountByMetricTypeAndDateRange(
        @Param("metricType") metricType: MetricType, 
        @Param("startDate") startDate: LocalDate, 
        @Param("endDate") endDate: LocalDate
    ): Long?
    
    @Query("SELECT date, SUM(total_count) FROM analytics_summary WHERE metric_type = :metricType AND date >= :startDate GROUP BY date ORDER BY date")
    fun getDailySummariesByMetricType(@Param("metricType") metricType: MetricType, @Param("startDate") startDate: LocalDate): List<Array<Any>>
}