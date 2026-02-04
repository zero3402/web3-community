package com.web3community.notification.repository

import com.web3community.notification.entity.Notification
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface NotificationRepository : ReactiveMongoRepository<Notification, String> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): Flux<Notification>
    fun findByUserIdAndIsReadOrderByCreatedAtDesc(userId: Long, isRead: Boolean): Flux<Notification>
    fun countByUserIdAndIsRead(userId: Long, isRead: Boolean): Mono<Long>
}