package com.web3community.sharedkernel.events

import com.web3community.sharedkernel.domain.DomainEvent
import java.time.LocalDateTime

// 사용자 관련 이벤트
sealed class UserDomainEvent(userId: String) : DomainEvent(userId, "User") {
    class UserCreated(userId: String, val username: String, val email: String) : UserDomainEvent(userId)
    class UserUpdated(userId: String) : UserDomainEvent(userId)
    class UserDeleted(userId: String) : UserDomainEvent(userId)
    class UserLoggedIn(userId: String) : UserDomainEvent(userId)
    class UserLoggedOut(userId: String) : UserDomainEvent(userId)
    class UserFollowed(userId: String, val followedUserId: String) : UserDomainEvent(userId)
    class UserUnfollowed(userId: String, val unfollowedUserId: String) : UserDomainEvent(userId)
    class UserPasswordChanged(userId: String) : UserDomainEvent(userId)
    class UserProfileUpdated(userId: String) : UserDomainEvent(userId)
}

// 포스트 관련 이벤트
sealed class PostDomainEvent(postId: String) : DomainEvent(postId, "Post") {
    class PostCreated(postId: String, val authorId: String, val title: String) : PostDomainEvent(postId)
    class PostUpdated(postId: String) : PostDomainEvent(postId)
    class PostDeleted(postId: String) : PostDomainEvent(postId)
    class PostPublished(postId: String) : PostDomainEvent(postId)
    class PostLiked(postId: String, val userId: String) : PostDomainEvent(postId)
    class PostUnliked(postId: String, val userId: String) : PostDomainEvent(postId)
    class PostShared(postId: String, val userId: String) : PostDomainEvent(postId)
    class PostViewed(postId: String, val userId: String?) : PostDomainEvent(postId)
}

// 댓글 관련 이벤트
sealed class CommentDomainEvent(commentId: String) : DomainEvent(commentId, "Comment") {
    class CommentCreated(commentId: String, val postId: String, val authorId: String, val content: String) : CommentDomainEvent(commentId)
    class CommentUpdated(commentId: String) : CommentDomainEvent(commentId)
    class CommentDeleted(commentId: String) : CommentDomainEvent(commentId)
    class CommentLiked(commentId: String, val userId: String) : CommentDomainEvent(commentId)
    class CommentUnliked(commentId: String, val userId: String) : CommentDomainEvent(commentId)
    class CommentReplied(commentId: String, val parentCommentId: String, val authorId: String) : CommentDomainEvent(commentId)
}

// 알림 관련 이벤트
sealed class NotificationDomainEvent(notificationId: String) : DomainEvent(notificationId, "Notification") {
    class NotificationSent(notificationId: String, val recipientId: String, val type: String) : NotificationDomainEvent(notificationId)
    class NotificationRead(notificationId: String, val userId: String) : NotificationDomainEvent(notificationId)
    class NotificationDeleted(notificationId: String, val userId: String) : NotificationDomainEvent(notificationId)
}

// 애널리틱스 관련 이벤트
sealed class AnalyticsDomainEvent(sessionId: String) : DomainEvent(sessionId, "Analytics") {
    class PageViewed(sessionId: String, val userId: String?, val pageUrl: String) : AnalyticsDomainEvent(sessionId)
    class UserLoggedIn(sessionId: String, val userId: String) : AnalyticsDomainEvent(sessionId)
    class UserRegistered(sessionId: String, val userId: String) : AnalyticsDomainEvent(sessionId)
    class Searched(sessionId: String, val userId: String?, val query: String) : AnalyticsDomainEvent(sessionId)
}