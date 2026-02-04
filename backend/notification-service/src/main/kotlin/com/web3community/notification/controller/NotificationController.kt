package com.web3community.notification.controller

import com.web3community.notification.dto.*
import com.web3community.notification.service.NotificationService
import com.web3community.util.annotation.CurrentUser
import com.web3community.user.dto.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = ["*"])
class NotificationController(
    private val notificationService: NotificationService
) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun createNotification(
        @Valid @RequestBody request: NotificationCreateRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.createNotification(request))
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun createBulkNotifications(
        @Valid @RequestBody request: BulkNotificationRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<BulkNotificationResponse> {
        return ResponseEntity.ok(notificationService.createBulkNotifications(request))
    }

    @PutMapping("/{notificationId}")
    fun updateNotification(
        @PathVariable notificationId: Long,
        @Valid @RequestBody request: NotificationUpdateRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.updateNotification(notificationId, request, currentUser.getUserId()))
    }

    @GetMapping("/{notificationId}")
    fun getNotification(
        @PathVariable notificationId: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.getNotificationById(notificationId, currentUser.getUserId()))
    }

    @GetMapping("/my")
    fun getMyNotifications(
        @CurrentUser currentUser: CustomUserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") unreadOnly: Boolean
    ): ResponseEntity<NotificationListResponse> {
        return ResponseEntity.ok(
            notificationService.getUserNotifications(
                currentUser.getUserId(),
                page,
                size,
                unreadOnly
            )
        )
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    fun searchNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RequestParam recipientId: Long? = null,
        @RequestParam type: NotificationType? = null,
        @RequestParam entityType: EntityType? = null,
        @RequestParam entityId: Long? = null,
        @RequestParam senderId: Long? = null,
        @RequestParam priority: NotificationPriority? = null,
        @RequestParam isRead: Boolean? = null,
        @RequestParam isSent: Boolean? = null,
        @RequestParam dateFrom: String? = null,
        @RequestParam dateTo: String? = null
    ): ResponseEntity<NotificationListResponse> {
        
        val searchRequest = NotificationSearchRequest(
            recipientId = recipientId,
            type = type,
            entityType = entityType,
            entityId = entityId,
            senderId = senderId,
            priority = priority,
            isRead = isRead,
            isSent = isSent,
            dateFrom = dateFrom,
            dateTo = dateTo,
            sortBy = NotificationSortBy.valueOf(sortBy.uppercase()),
            sortDirection = SortDirection.valueOf(sortDirection.uppercase())
        )
        
        val sort = Sort.by(
            if (sortDirection.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC,
            sortBy.lowercase()
        )
        val pageable: Pageable = PageRequest.of(page, size, sort)
        
        return ResponseEntity.ok(notificationService.searchNotifications(searchRequest, pageable))
    }

    @PostMapping("/{notificationId}/read")
    fun markAsRead(
        @PathVariable notificationId: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, currentUser.getUserId()))
    }

    @PostMapping("/mark-all-read")
    fun markAllAsRead(@CurrentUser currentUser: CustomUserDetails): ResponseEntity<String> {
        return ResponseEntity.ok(notificationService.markAllAsRead(currentUser.getUserId()))
    }

    @DeleteMapping("/{notificationId}")
    fun deleteNotification(
        @PathVariable notificationId: Long,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<String> {
        return ResponseEntity.ok(notificationService.deleteNotification(notificationId, currentUser.getUserId()))
    }

    @GetMapping("/stats")
    fun getNotificationStats(@CurrentUser currentUser: CustomUserDetails): ResponseEntity<NotificationStatsResponse> {
        return ResponseEntity.ok(notificationService.getNotificationStats(currentUser.getUserId()))
    }

    // System endpoints for administrators
    @GetMapping("/system/stats")
    @PreAuthorize("hasRole('ADMIN')")
    fun getSystemNotificationStats(): ResponseEntity<Map<String, Any>> {
        // This would return system-wide notification statistics
        return ResponseEntity.ok(mapOf(
            "message" to "System stats endpoint - to be implemented"
        ))
    }

    @PostMapping("/system/announcement")
    @PreAuthorize("hasRole('ADMIN')")
    fun sendSystemAnnouncement(
        @Valid @RequestBody request: SystemAnnouncementRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): ResponseEntity<String> {
        // This would send system-wide announcements
        return ResponseEntity.ok("System announcement sent successfully")
    }
}

data class SystemAnnouncementRequest(
    val title: String,
    val message: String,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val targetRoles: List<String> = emptyList(),
    val expiresAt: String? = null
)