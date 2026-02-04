package com.web3community.sharedkernel.domain

import java.util.*

// 사용자 ID Value Object
@JvmInlineValue
value class UserId(val value: Long) {
    companion object {
        fun generate(): UserId = UserId(System.currentTimeMillis())
        fun of(value: Long): UserId = UserId(value)
    }
}

// 포스트 ID Value Object
@JvmInlineValue
value class PostId(val value: Long) {
    companion object {
        fun generate(): PostId = PostId(System.currentTimeMillis())
        fun of(value: Long): PostId = PostId(value)
    }
}

// 댓글 ID Value Object
@JvmInlineValue
value class CommentId(val value: Long) {
    companion object {
        fun generate(): CommentId = CommentId(System.currentTimeMillis())
        fun of(value: Long): CommentId = CommentId(value)
    }
}

// 알림 ID Value Object
@JvmInlineValue
value class NotificationId(val value: Long) {
    companion object {
        fun generate(): NotificationId = NotificationId(System.currentTimeMillis())
        fun of(value: Long): NotificationId = NotificationId(value)
    }
}

// 세션 ID Value Object
@JvmInlineValue
value class SessionId(val value: String) {
    companion object {
        fun generate(): SessionId = SessionId(UUID.randomUUID().toString())
        fun of(value: String): SessionId = SessionId(value)
    }
}