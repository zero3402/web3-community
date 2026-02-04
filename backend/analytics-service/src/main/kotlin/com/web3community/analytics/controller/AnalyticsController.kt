package com.web3community.analytics.controller

import com.web3community.analytics.dto.*
import com.web3community.analytics.service.AnalyticsService
import com.web3community.util.annotation.CurrentUser
import com.web3community.user.dto.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = ["*"])
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    @PostMapping("/events")
    fun trackEvent(@Valid @RequestBody request: AnalyticsEventRequest): ResponseEntity<AnalyticsResponse> {
        return ResponseEntity.ok(analyticsService.trackEvent(request))
    }

    @PostMapping("/events/bulk")
    fun trackBulkEvents(@Valid @RequestBody requests: List<AnalyticsEventRequest>): ResponseEntity<AnalyticsResponse> {
        return ResponseEntity.ok(analyticsService.trackBulkEvents(requests))
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun getDashboard(): ResponseEntity<AnalyticsDashboardResponse> {
        return ResponseEntity.ok(analyticsService.getDashboardAnalytics())
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @authService.isCurrentUser(#userId, authentication))")
    fun getUserAnalytics(
        @PathVariable userId: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<UserAnalyticsResponse> {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(userId))
    }

    @GetMapping("/my")
    fun getMyAnalytics(@CurrentUser currentUser: CustomUserDetails): ResponseEntity<UserAnalyticsResponse> {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(currentUser.getUserId()))
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun searchEvents(
        @RequestParam userId: Long? = null,
        @RequestParam eventType: String? = null,
        @RequestParam dateFrom: String? = null,
        @RequestParam dateTo: String? = null,
        @RequestParam deviceType: String? = null,
        @RequestParam countryCode: String? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<EventListResponse> {
        
        val request = EventSearchRequest(
            userId = userId,
            eventType = eventType?.let { com.web3community.analytics.entity.EventType.valueOf(it.uppercase()) },
            dateFrom = dateFrom,
            dateTo = dateTo,
            deviceType = deviceType?.let { com.web3community.analytics.entity.DeviceType.valueOf(it.uppercase()) },
            countryCode = countryCode,
            page = page,
            size = size
        )
        
        return ResponseEntity.ok(analyticsService.searchEvents(request))
    }

    // Additional endpoints for admin analytics
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAnalyticsSummary(
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<Map<String, Any>> {
        // This would return comprehensive analytics summary
        return ResponseEntity.ok(mapOf(
            "message" to "Analytics summary endpoint - to be implemented"
        ))
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    fun exportAnalytics(
        @RequestParam format: String = "csv",
        @RequestParam startDate: String? = null,
        @RequestParam endDate: String? = null
    ): ResponseEntity<Map<String, Any>> {
        // This would export analytics data
        return ResponseEntity.ok(mapOf(
            "message" to "Export endpoint - to be implemented"
        ))
    }
}