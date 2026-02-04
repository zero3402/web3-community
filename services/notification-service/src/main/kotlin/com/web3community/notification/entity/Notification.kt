package com.web3community.notification.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@Document(collection = "notifications")
data class Notification(
    @Id
    val id: String? = null,
    
    val userId: Long,
    
    val title: String,
    
    val message: String,
    
    val type: NotificationType,
    
    val isRead: Boolean = false,
    
    val relatedId: String? = null,
    
    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    
    constructor() : this(0, "", "", NotificationType.POST_CREATED)
}

enum class NotificationType {
    POST_CREATED,
    COMMENT_ADDED,
    LIKE_RECEIVED,
    FOLLOW_REQUEST,
    SYSTEM_ANNOUNCEMENT
}