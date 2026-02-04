package com.web3community.analytics.domain.model

import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.domain.PostId
import com.web3community.sharedkernel.domain.CommentId
import com.web3community.sharedkernel.events.AnalyticsDomainEvent
import jakarta.persistence.*

// 애널리틱스 이벤트 애그리게이트 루트
@Entity
@Table(name = "analytics_events")
class AnalyticsEvent(
    @Column(name = "user_id")
    var userId: UserId? = null,
        private set

    @Column(name = "session_id")
    var sessionId: String? = null,
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, columnDefinition = "ENUM('PAGE_VIEW', 'POST_VIEW', 'POST_CREATE', 'POST_UPDATE', 'POST_DELETE', 'POST_LIKE', 'POST_SHARE', 'COMMENT_CREATE', 'COMMENT_UPDATE', 'COMMENT_DELETE', 'COMMENT_LIKE', 'USER_LOGIN', 'USER_LOGOUT', 'USER_REGISTER', 'USER_FOLLOW', 'USER_UNFOLLOW', 'SEARCH_QUERY', 'FILE_UPLOAD', 'NOTIFICATION_CLICK', 'PROFILE_VIEW', 'SETTINGS_UPDATE', 'ERROR_OCCURRED', 'PERFORMANCE_METRIC')")
    val eventType: EventType,

    @Column(name = "event_name", nullable = false, columnDefinition = "VARCHAR(100)")
    val eventName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", columnDefinition = "ENUM('POST', 'COMMENT', 'USER', 'SYSTEM', 'MESSAGE', 'ACHIEVEMENT')")
    var entityType: String? = null,
        private set

    @Column(name = "entity_id")
    var entityId: Long? = null,
        private set

    @Column(name = "source_ip", columnDefinition = "VARCHAR(45)")
    var sourceIp: String? = null,
        private set

    @Column(name = "user_agent", columnDefinition = "TEXT")
    var userAgent: String? = null,
        private set

    @Column(name = "referrer", columnDefinition = "TEXT")
    var referrer: String? = null,
        private set

    @Column(name = "country_code", columnDefinition = "VARCHAR(2)")
    var countryCode: String? = null,
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", columnDefinition = "ENUM('DESKTOP', 'MOBILE', 'TABLET', 'SMART_TV', 'WEARABLE', 'UNKNOWN')")
    var deviceType: DeviceType? = null,
        private set

    @Column(name = "browser", columnDefinition = "VARCHAR(50)")
    var browser: String? = null,
        private set

    @Column(name = "platform", columnDefinition = "VARCHAR(50)")
    var platform: String? = null,
        private set

    @Column(name = "page_url", columnDefinition = "VARCHAR(500)")
    var pageUrl: String? = null,
        private set

    @Column(name = "page_title", columnDefinition = "VARCHAR(255)")
    var pageTitle: String? = null,
        private set

    @Column(name = "metadata", columnDefinition = "JSON")
    var metadata: String? = null,
        private set

    @Column(name = "value")
    var value: Double? = null,
        private set

    @Column(name = "timestamp", nullable = false)
    val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()

) {
    
    // JPA 생성자
    protected constructor() : super()

    // 도메인 생성자
    constructor(
        eventType: EventType,
        eventName: String,
        userId: UserId? = null,
        sessionId: String? = null,
        entityType: String? = null,
        entityId: Long? = null,
        sourceIp: String? = null,
        userAgent: String? = null,
        referrer: String? = null,
        countryCode: String? = null,
        deviceType: DeviceType? = null,
        browser: String? = null,
        platform: String? = null,
        pageUrl: String? = null,
        pageTitle: String? = null,
        metadata: Map<String, Any>? = null,
        value: Double? = null
    ) : this() {
        this.userId = userId
        this.sessionId = sessionId
        this.eventType = eventType
        this.eventName = eventName
        this.entityType = eventType
        this.entityId = entityId
        this.sourceIp = sourceIp
        this.userAgent = userAgent
        this.referrer = referrer
        this.countryCode = countryCode
        this.deviceType = deviceType
        this.browser = browser
        this.platform = platform
        this.pageUrl = pageUrl
        this.pageTitle = pageTitle
        this.metadata = metadata?.let { com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(it) }
        this.value = value
        
        // 도메인 이벤트 발행
        this.addDomainEvent(AnalyticsDomainEvent.PageViewed(this.sessionId ?: ""))
    }

    // 비즈니스 메소드
    fun trackPageView(
        userId: UserId? = null,
        pageUrl: String,
        pageTitle: String? = null,
        deviceInfo: Map<String, String>? = null
    ): AnalyticsEvent {
        return AnalyticsEvent(
            eventType = EventType.PAGE_VIEW,
            eventName = "page_view",
            userId = userId,
            sessionId = this.sessionId,
            entityType = "PAGE",
            pageUrl = pageUrl,
            pageTitle = pageTitle,
            deviceType = DeviceType.valueOf(deviceInfo?.get("device_type") ?: "UNKNOWN"),
            browser = deviceInfo?.get("browser"),
            platform = deviceInfo?.get("platform"),
            userAgent = deviceInfo?.get("user_agent")
        )
    }

    fun trackPostAction(
        action: PostAction,
        userId: UserId? = null,
        postId: PostId,
        metadata: Map<String, Any>? = null
    ): AnalyticsEvent {
        val eventType = when (action) {
            PostAction.VIEW -> EventType.POST_VIEW
            PostAction.CREATE -> EventType.POST_CREATE
            PostAction.UPDATE -> EventType.POST_UPDATE
            PostAction.DELETE -> EventType.POST_DELETE
            PostAction.LIKE -> EventType.POST_LIKE
            PostAction.SHARE -> EventType.POST_SHARE
        }
        
        return AnalyticsEvent(
            eventType = eventType,
            eventName = action.name.lowercase(),
            userId = userId,
            sessionId = this.sessionId,
            entityType = "POST",
            entityId = postId.value,
            metadata = metadata
        )
    }

    fun trackUserAction(
        action: UserAction,
        userId: UserId,
        metadata: Map<String, Any>? = null
    ): AnalyticsEvent {
        val eventType = when (action) {
            UserAction.LOGIN -> EventType.USER_LOGIN
            UserAction.LOGOUT -> EventType.USER_LOGOUT
            UserAction.REGISTER -> EventType.USER_REGISTER
            UserAction.FOLLOW -> EventType.USER_FOLLOW
            UserAction.UNFOLLOW -> EventType.USER_UNFOLLOW
        }
        
        return AnalyticsEvent(
            eventType = eventType,
            eventName = action.name.lowercase(),
            userId = userId,
            sessionId = this.sessionId,
            entityType = "USER",
            entityId = userId.value,
            metadata = metadata
        )
    }

    fun getMetadataMap(): Map<String, Any> {
        return this.metadata?.let { 
            try {
                com.fasterxml.jackson.databind.ObjectMapper().readValue(it, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    }

    override fun toString(): String {
        return "AnalyticsEvent(eventType=${eventType}, userId=${userId?.value}, sessionId=$sessionId, timestamp=$timestamp)"
    }
}

// 애너리틱스 이벤트 타입 Enum
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

// 디바이스 타입 Enum
enum class DeviceType {
    DESKTOP,
    MOBILE,
    TABLET,
    SMART_TV,
    WEARABLE,
    UNKNOWN
}

// 포스트 액션 Enum
enum class PostAction {
    VIEW,
    CREATE,
    UPDATE,
    DELETE,
    LIKE,
    SHARE
}

// 사용자 액션 Enum
enum class UserAction {
    LOGIN,
    LOGOUT,
    REGISTER,
    FOLLOW,
    UNFOLLOW
}