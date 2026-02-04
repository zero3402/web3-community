package com.web3community.analytics.dto

import com.web3community.analytics.entity.EventType
import com.web3community.analytics.entity.DeviceType
import jakarta.validation.constraints.NotNull

data class AnalyticsEventRequest(
    @field:NotNull(message = "Event type is required")
    val eventType: EventType,
    
    @field:NotNull(message = "Event name is required")
    val eventName: String,
    
    val userId: Long? = null,
    val sessionId: String? = null,
    val entityType: String? = null,
    val entityId: Long? = null,
    val sourceIp: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
    val countryCode: String? = null,
    val deviceType: DeviceType? = null,
    val browser: String? = null,
    val platform: String? = null,
    val pageUrl: String? = null,
    val pageTitle: String? = null,
    val properties: Map<String, Any>? = null,
    val value: Double? = null
)

data class AnalyticsResponse(
    val message: String,
    val eventId: Long? = null
)

data class AnalyticsDashboardResponse(
    val totalUsers: Long,
    val activeUsers: Long,
    val totalPosts: Long,
    val totalComments: Long,
    val totalViews: Long,
    val userGrowth: List<DailyMetric>,
    val postEngagement: List<DailyMetric>,
    val topPages: List<PageMetric>,
    val topUsers: List<UserMetric>,
    val deviceBreakdown: Map<String, Long>,
    val countryBreakdown: Map<String, Long>
)

data class DailyMetric(
    val date: String,
    val value: Long
)

data class PageMetric(
    val page: String,
    val views: Long,
    val uniqueUsers: Long
)

data class UserMetric(
    val userId: Long,
    val username: String? = null,
    val activity: Long,
    val engagement: Long
)

data class UserAnalyticsResponse(
    val userId: Long,
    val sessionCount: Int,
    val pageViews: Int,
    val sessionDuration: Long,
    val postsCreated: Int,
    val commentsCreated: Int,
    val likesGiven: Int,
    val lastSeen: String? = null,
    val activityHistory: List<DailyMetric>
)

data class EventSearchRequest(
    val userId: Long? = null,
    val eventType: EventType? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val deviceType: DeviceType? = null,
    val countryCode: String? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class EventListResponse(
    val events: List<Map<String, Any>>,
    val total: Long,
    val page: Int,
    val size: Int
)