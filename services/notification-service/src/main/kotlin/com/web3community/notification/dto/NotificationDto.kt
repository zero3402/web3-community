package com.web3community.notification.dto

import com.web3community.notification.entity.NotificationType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NotificationCreateRequest(
    @field:NotBlank(message = "사용자 ID는 필수입니다.")
    val userId: Long,
    
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    val title: String,
    
    @field:NotBlank(message = "메시지는 필수입니다.")
    @field:Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.")
    val message: String,
    
    val type: NotificationType,
    
    val relatedId: String? = null
)

data class NotificationResponse(
    val id: String,
    val userId: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean,
    val relatedId: String?,
    val createdAt: String
)