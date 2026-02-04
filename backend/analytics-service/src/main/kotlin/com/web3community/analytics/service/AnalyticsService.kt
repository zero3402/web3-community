package com.web3community.analytics.service

import com.web3community.analytics.dto.*
import com.web3community.analytics.entity.*
import com.web3community.analytics.repository.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class AnalyticsService(
    private val analyticsEventRepository: AnalyticsEventRepository,
    private val analyticsSummaryRepository: AnalyticsSummaryRepository,
    private val userAnalyticsRepository: UserAnalyticsRepository
) {

    fun trackEvent(request: AnalyticsEventRequest): AnalyticsResponse {
        val event = AnalyticsEvent(
            userId = request.userId,
            sessionId = request.sessionId,
            eventType = request.eventType,
            eventName = request.eventName,
            entityType = request.entityType,
            entityId = request.entityId,
            sourceIp = request.sourceIp,
            userAgent = request.userAgent,
            referrer = request.referrer,
            countryCode = request.countryCode,
            deviceType = request.deviceType,
            browser = request.browser,
            platform = request.platform,
            pageUrl = request.pageUrl,
            pageTitle = request.pageTitle,
            properties = request.properties?.let { 
                com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(it)
            },
            value = request.value
        )
        
        val savedEvent = analyticsEventRepository.save(event)
        
        // Update user analytics if user is present
        request.userId?.let { userId ->
            updateUserAnalytics(userId, request)
        }
        
        return AnalyticsResponse(
            message = "Event tracked successfully",
            eventId = savedEvent.id
        )
    }

    fun trackBulkEvents(requests: List<AnalyticsEventRequest>): AnalyticsResponse {
        val events = requests.map { request ->
            AnalyticsEvent(
                userId = request.userId,
                sessionId = request.sessionId,
                eventType = request.eventType,
                eventName = request.eventName,
                entityType = request.entityType,
                entityId = request.entityId,
                sourceIp = request.sourceIp,
                userAgent = request.userAgent,
                referrer = request.referrer,
                countryCode = request.countryCode,
                deviceType = request.deviceType,
                browser = request.browser,
                platform = request.platform,
                pageUrl = request.pageUrl,
                pageTitle = request.pageTitle,
                properties = request.properties?.let { 
                    com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(it)
                },
                value = request.value
            )
        }
        
        analyticsEventRepository.saveAll(events)
        
        // Update user analytics for user-specific events
        requests.filter { it.userId != null }.forEach { request ->
            request.userId?.let { userId ->
                updateUserAnalytics(userId, request)
            }
        }
        
        return AnalyticsResponse(
            message = "Bulk events tracked successfully",
            eventId = null
        )
    }

    @Cacheable(value = ["dashboard"], key = "'overview'")
    fun getDashboardAnalytics(): AnalyticsDashboardResponse {
        val totalUsers = getTotalUsers()
        val activeUsers = getActiveUsers()
        val totalPosts = getTotalPosts()
        val totalComments = getTotalComments()
        val totalViews = getTotalViews()
        
        val userGrowth = getUserGrowth(30)
        val postEngagement = getPostEngagement(30)
        val topPages = getTopPages(10)
        val topUsers = getTopUsers(10)
        val deviceBreakdown = getDeviceBreakdown()
        val countryBreakdown = getCountryBreakdown()
        
        return AnalyticsDashboardResponse(
            totalUsers = totalUsers,
            activeUsers = activeUsers,
            totalPosts = totalPosts,
            totalComments = totalComments,
            totalViews = totalViews,
            userGrowth = userGrowth,
            postEngagement = postEngagement,
            topPages = topPages,
            topUsers = topUsers,
            deviceBreakdown = deviceBreakdown,
            countryBreakdown = countryBreakdown
        )
    }

    fun getUserAnalytics(userId: Long): UserAnalyticsResponse {
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        
        val userStats = userAnalyticsRepository.findByUserIdAndDateBetween(userId, thirtyDaysAgo, today)
        
        val totalSessions = userStats.sumOf { it.sessionCount }
        val totalPages = userStats.sumOf { it.pageViews }
        val totalDuration = userStats.sumOf { it.sessionDuration }
        val totalPosts = userStats.sumOf { it.postsCreated }
        val totalComments = userStats.sumOf { it.commentsCreated }
        val totalLikes = userStats.sumOf { it.likesGiven }
        val lastSeen = userStats.maxOfOrNull { it.lastSeen }
        
        val activityHistory = userStats.sortedBy { it.date }.map { stat ->
            DailyMetric(
                date = stat.date.toString(),
                value = stat.pageViews.toLong()
            )
        }
        
        return UserAnalyticsResponse(
            userId = userId,
            sessionCount = totalSessions,
            pageViews = totalPages,
            sessionDuration = totalDuration,
            postsCreated = totalPosts,
            commentsCreated = totalComments,
            likesGiven = totalLikes,
            lastSeen = lastSeen?.toString(),
            activityHistory = activityHistory
        )
    }

    fun searchEvents(request: EventSearchRequest): EventListResponse {
        val pageable = PageRequest.of(request.page, request.size)
        
        val events = when {
            request.userId != null -> {
                analyticsEventRepository.findByUserIdOrderByTimestampDesc(request.userId, pageable)
            }
            request.eventType != null -> {
                analyticsEventRepository.findByEventTypeOrderByTimestampDesc(request.eventType, pageable)
            }
            else -> {
                analyticsEventRepository.findAllByOrderByTimestampDesc(pageable)
            }
        }
        
        val eventMaps = events.content.map { event ->
            mapOf(
                "id" to (event.id ?: 0),
                "userId" to event.userId,
                "eventType" to event.eventType.name,
                "eventName" to event.eventName,
                "entityType" to event.entityType,
                "entityId" to event.entityId,
                "deviceType" to event.deviceType?.name,
                "browser" to event.browser,
                "pageUrl" to event.pageUrl,
                "pageTitle" to event.pageTitle,
                "value" to event.value,
                "timestamp" to event.timestamp.toString(),
                "properties" to event.getPropertiesMap()
            )
        }
        
        return EventListResponse(
            events = eventMaps,
            total = events.totalElements,
            page = events.number,
            size = events.size
        )
    }

    private fun updateUserAnalytics(userId: Long, request: AnalyticsEventRequest) {
        val today = LocalDate.now()
        val userAnalytics = userAnalyticsRepository.findByUserIdAndDate(userId, today)
            .orElseGet {
                UserAnalytics(
                    userId = userId,
                    date = today
                )
            }
        
        val updatedAnalytics = userAnalytics.copy(
            sessionCount = if (request.eventType == EventType.USER_LOGIN) userAnalytics.sessionCount + 1 else userAnalytics.sessionCount,
            pageViews = if (request.eventType == EventType.PAGE_VIEW) userAnalytics.pageViews + 1 else userAnalytics.pageViews,
            postsCreated = if (request.eventType == EventType.POST_CREATE) userAnalytics.postsCreated + 1 else userAnalytics.postsCreated,
            commentsCreated = if (request.eventType == EventType.COMMENT_CREATE) userAnalytics.commentsCreated + 1 else userAnalytics.commentsCreated,
            likesGiven = if (request.eventType == EventType.POST_LIKE || request.eventType == EventType.COMMENT_LIKE) userAnalytics.likesGiven + 1 else userAnalytics.likesGiven,
            lastSeen = LocalDateTime.now(),
            countryCode = request.countryCode ?: userAnalytics.countryCode,
            deviceType = request.deviceType ?: userAnalytics.deviceType,
            browser = request.browser ?: userAnalytics.browser
        )
        
        userAnalyticsRepository.save(updatedAnalytics)
    }

    private fun getTotalUsers(): Long {
        return userAnalyticsRepository.countDistinctUsers()
    }

    private fun getActiveUsers(): Long {
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        return userAnalyticsRepository.countActiveUsersSince(thirtyDaysAgo)
    }

    private fun getTotalPosts(): Long {
        return analyticsEventRepository.countByEventType(EventType.POST_CREATE)
    }

    private fun getTotalComments(): Long {
        return analyticsEventRepository.countByEventType(EventType.COMMENT_CREATE)
    }

    private fun getTotalViews(): Long {
        return analyticsEventRepository.countByEventType(EventType.PAGE_VIEW)
    }

    private fun getUserGrowth(days: Int): List<DailyMetric> {
        val startDate = LocalDate.now().minusDays(days.toLong())
        // This would be implemented with proper date grouping query
        return emptyList() // Placeholder
    }

    private fun getPostEngagement(days: Int): List<DailyMetric> {
        val startDate = LocalDate.now().minusDays(days.toLong())
        // This would be implemented with proper date grouping query
        return emptyList() // Placeholder
    }

    private fun getTopPages(limit: Int): List<PageMetric> {
        // This would be implemented with proper grouping query
        return emptyList() // Placeholder
    }

    private fun getTopUsers(limit: Int): List<UserMetric> {
        // This would be implemented with proper grouping query
        return emptyList() // Placeholder
    }

    private fun getDeviceBreakdown(): Map<String, Long> {
        return analyticsEventRepository.countByDeviceType()
            .mapKeys { it.key?.name ?: "unknown" }
    }

    private fun getCountryBreakdown(): Map<String, Long> {
        return analyticsEventRepository.countByCountryCode()
            .mapKeys { it.key ?: "unknown" }
    }
}