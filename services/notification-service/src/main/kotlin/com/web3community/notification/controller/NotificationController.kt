package com.web3community.notification.controller

import com.web3community.notification.dto.NotificationCreateRequest
import com.web3community.notification.dto.NotificationResponse
import com.web3community.notification.service.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = ["*"])
class NotificationController(private val notificationService: NotificationService) {

    @PostMapping
    fun createNotification(@Valid @RequestBody request: NotificationCreateRequest): Mono<ResponseEntity<NotificationResponse>> {
        return notificationService.createNotification(request)
            .map { notification -> ResponseEntity.status(HttpStatus.CREATED).body(notification) }
            .onErrorResume { e ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @GetMapping("/user/{userId}")
    fun getUserNotifications(@PathVariable userId: Long): Flux<NotificationResponse> {
        return notificationService.getUserNotifications(userId)
    }

    @GetMapping("/user/{userId}/unread")
    fun getUnreadNotifications(@PathVariable userId: Long): Flux<NotificationResponse> {
        return notificationService.getUnreadNotifications(userId)
    }

    @GetMapping("/user/{userId}/unread/count")
    fun getUnreadCount(@PathVariable userId: Long): Mono<Map<String, Long>> {
        return notificationService.getUnreadCount(userId)
            .map { count -> mapOf("unreadCount" to count) }
    }

    @GetMapping(value = ["/user/{userId}/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNotificationsStream(@PathVariable userId: Long): Flux<NotificationResponse> {
        return notificationService.getUserNotifications(userId)
            .distinctUntilChanged { it.id }
    }

    @PutMapping("/{id}/read")
    fun markAsRead(@PathVariable id: String): Mono<ResponseEntity<NotificationResponse>> {
        return notificationService.markAsRead(id)
            .map { notification -> ResponseEntity.ok(notification) }
            .onErrorResume { e ->
                if (e is IllegalArgumentException && e.message == "Notification not found") {
                    Mono.just(ResponseEntity.notFound().build())
                } else {
                    Mono.just(ResponseEntity.badRequest().build())
                }
            }
    }

    @PutMapping("/user/{userId}/read-all")
    fun markAllAsRead(@PathVariable userId: Long): Mono<ResponseEntity<String>> {
        return notificationService.markAllAsRead(userId)
            .map { message -> ResponseEntity.ok(message) }
    }
}