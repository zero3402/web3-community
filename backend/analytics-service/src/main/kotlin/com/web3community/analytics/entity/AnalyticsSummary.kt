package com.web3community.analytics.entity

import jakarta.persistence.*

@Entity
@Table(name = "analytics_summaries")
data class AnalyticsSummary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "date", nullable = false)
    val date: java.time.LocalDate,
    
    @Column(name = "metric_type", nullable = false)
    val metricType: MetricType,
    
    @Column(name = "entity_type")
    val entityType: String? = null,
    
    @Column(name = "entity_id")
    val entityId: Long? = null,
    
    @Column(name = "total_count", nullable = false)
    val totalCount: Long = 0,
    
    @Column(name = "unique_users")
    val uniqueUsers: Long = 0,
    
    @Column(name = "total_value")
    val totalValue: Double = 0.0,
    
    @Column(name = "avg_value")
    val avgValue: Double = 0.0,
    
    @Column(name = "min_value")
    val minValue: Double? = null,
    
    @Column(name = "max_value")
    val maxValue: Double? = null,
    
    @Column(name = "breakdown", columnDefinition = "JSON")
    val breakdown: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    fun getBreakdownMap(): Map<String, Any> {
        return breakdown?.let {
            try {
                com.fasterxml.jackson.databind.ObjectMapper().readValue(it, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    }
}

enum class MetricType {
    PAGE_VIEWS,
    UNIQUE_VISITORS,
    SESSIONS,
    POST_VIEWS,
    POST_CREATES,
    POST_LIKES,
    POST_SHARES,
    COMMENTS,
    COMMENT_LIKES,
    USER_REGISTRATIONS,
    USER_LOGINS,
    ACTIVE_USERS,
    SEARCH_QUERIES,
    FILE_UPLOADS,
    NOTIFICATIONS_SENT,
    ERRORS_OCCURRED,
    AVG_SESSION_DURATION,
    BOUNCE_RATE,
    CONVERSION_RATE
}