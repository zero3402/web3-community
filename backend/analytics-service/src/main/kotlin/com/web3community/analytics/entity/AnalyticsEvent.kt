package com.web3community.analytics.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "analytics_events")
data class AnalyticsEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "user_id")
    val userId: Long? = null,
    
    @Column(name = "session_id")
    val sessionId: String? = null,
    
    @Column(nullable = false)
    val eventType: EventType,
    
    @Column(nullable = false)
    val eventName: String,
    
    @Column(name = "entity_type")
    val entityType: String? = null,
    
    @Column(name = "entity_id")
    val entityId: Long? = null,
    
    @Column(name = "source_ip")
    val sourceIp: String? = null,
    
    @Column(name = "user_agent")
    val userAgent: String? = null,
    
    @Column(name = "referrer")
    val referrer: String? = null,
    
    @Column(name = "country_code")
    val countryCode: String? = null,
    
    @Column(name = "device_type")
    val deviceType: DeviceType? = null,
    
    @Column(name = "browser")
    val browser: String? = null,
    
    @Column(name = "platform")
    val platform: String? = null,
    
    @Column(name = "page_url")
    val pageUrl: String? = null,
    
    @Column(name = "page_title")
    val pageTitle: String? = null,
    
    @Column(name = "properties", columnDefinition = "JSON")
    val properties: String? = null,
    
    @Column(name = "value")
    val value: Double? = null,
    
    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
    
) {
    fun getPropertiesMap(): Map<String, Any> {
        return properties?.let {
            try {
                com.fasterxml.jackson.databind.ObjectMapper().readValue(it, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    }
}

enum class EventType {
    PAGE_VIEW,
    POST_VIEW,
    POST_CREATE,
    POST_UPDATE,
    POST_DELETE,
    POST_LIKE,
    POST_SHARE,
    COMMENT_CREATE,
    COMMENT_UPDATE,
    COMMENT_DELETE,
    COMMENT_LIKE,
    USER_LOGIN,
    USER_LOGOUT,
    USER_REGISTER,
    USER_FOLLOW,
    USER_UNFOLLOW,
    SEARCH_QUERY,
    FILE_UPLOAD,
    NOTIFICATION_CLICK,
    PROFILE_VIEW,
    SETTINGS_UPDATE,
    ERROR_OCCURRED,
    PERFORMANCE_METRIC
}

enum class DeviceType {
    DESKTOP,
    MOBILE,
    TABLET,
    SMART_TV,
    WEARABLE,
    UNKNOWN
}