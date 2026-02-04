package com.web3community.analytics.entity

import jakarta.persistence.*

@Entity
@Table(name = "user_analytics")
data class UserAnalytics(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(name = "date", nullable = false)
    val date: java.time.LocalDate,
    
    @Column(name = "session_count")
    val sessionCount: Int = 0,
    
    @Column(name = "page_views")
    val pageViews: Int = 0,
    
    @Column(name = "session_duration")
    val sessionDuration: Long = 0, // in seconds
    
    @Column(name = "last_seen")
    val lastSeen: java.time.LocalDateTime? = null,
    
    @Column(name = "posts_created")
    val postsCreated: Int = 0,
    
    @Column(name = "comments_created")
    val commentsCreated: Int = 0,
    
    @Column(name = "likes_given")
    val likesGiven: Int = 0,
    
    @Column(name = "shares_performed")
    val sharesPerformed: Int = 0,
    
    @Column(name = "searches_performed")
    val searchesPerformed: Int = 0,
    
    @Column(name = "files_uploaded")
    val filesUploaded: Int = 0,
    
    @Column(name = "country_code")
    val countryCode: String? = null,
    
    @Column(name = "device_type")
    val deviceType: DeviceType? = null,
    
    @Column(name = "browser")
    val browser: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)