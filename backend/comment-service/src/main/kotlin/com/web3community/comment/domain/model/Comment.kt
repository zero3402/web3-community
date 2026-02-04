package com.web3community.comment.domain.model

import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.CommentId
import com.web3community.sharedkernel.domain.PostId
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.events.CommentDomainEvent
import jakarta.persistence.*

// 댓글 애그리게이트 루트
@Entity
@Table(name = "comments")
class Comment(
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
        set(value) {
            require(value.isNotBlank()) { "Content cannot be blank" }
            require(value.length >= 1) { "Content must be at least 1 character" }
            require(value.length <= 2000) { "Content must be at most 2000 characters" }
            field = value
            addDomainEvent(CommentDomainEvent.CommentUpdated(id.value.toString()))
        }

    @Column(name = "post_id", nullable = false)
    var postId: PostId,
        private set

    @Column(name = "author_id", nullable = false)
    var authorId: UserId,
        private set

    @Column(name = "parent_id")
    var parentId: CommentId? = null,
        private set

    @Column(name = "thread_id", nullable = false)
    var threadId: Long,
        private set

    @Column(name = "level", nullable = false)
    var level: Int = 0,
        private set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L,
        private set

    @Column(name = "dislike_count", nullable = false)
    var dislikeCount: Long = 0L,
        private set

    @Column(name = "reply_count", nullable = false)
    var replyCount: Long = 0L,
        private set

    @Column(name = "is_edited", nullable = false)
    var isEdited: Boolean = false,
        private set

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,
        private set

    @Column(name = "is_pinned", nullable = false)
    var isPinned: Boolean = false,
        private set

    @Column(name = "is_reported", nullable = false)
    var isReported: Boolean = false,
        private set

    @Column(name = "edited_content", columnDefinition = "TEXT")
    var editedContent: String? = null,
        private set

    @Column(name = "edited_at")
    var editedAt: java.time.LocalDateTime? = null,
        private set

    @Column(name = "deleted_at")
    var deletedAt: java.time.LocalDateTime? = null,
        private set

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    val parent: Comment? = null

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val replies: MutableSet<Comment> = mutableSetOf()

    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val reactions: MutableSet<CommentReaction> = mutableSetOf()

    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val attachments: MutableSet<CommentAttachment> = mutableSetOf()

    // JPA 생성자
    protected constructor() : super()

    // 도메인 생성자
    constructor(
        content: String,
        postId: PostId,
        authorId: UserId
    ) : super() {
        this.content = content
        this.postId = postId
        this.authorId = authorId
        this.threadId = generateThreadId()
        this.level = 0
        this.addDomainEvent(CommentDomainEvent.CommentCreated(
            id.value.toString(),
            postId.value.toString(),
            authorId.value.toString(),
            content
        ))
    }

    // 댓글에 대한 답글 생성자
    constructor(
        content: String,
        postId: PostId,
        authorId: UserId,
        parentComment: Comment
    ) : super() {
        this.content = content
        this.postId = postId
        this.authorId = authorId
        this.parentId = parentComment.id
        this.threadId = parentComment.threadId
        this.level = parentComment.level + 1
        
        // 최대 뎈스프 레벨 검증
        if (this.level > 5) {
            throw BusinessRuleViolationException("Maximum nesting level reached")
        }
        
        this.addDomainEvent(CommentDomainEvent.CommentReplied(id.value.toString(), parentComment.id.value.toString()))
    }

    // 비즈니스 메소드
    fun editContent(newContent: String, editorId: UserId) {
        if (this.authorId != editorId) {
            throw PermissionDeniedException("Only author can edit comment")
        }
        
        if (this.isDeleted) {
            throw InvalidStateException("Cannot edit deleted comment")
        }
        
        this.editedContent = this.content
        this.content = newContent
        this.isEdited = true
        this.editedAt = java.time.LocalDateTime.now()
        this.addDomainEvent(CommentDomainEvent.CommentUpdated(id.value.toString()))
    }

    fun delete(deleterId: UserId) {
        if (this.authorId != deleterId) {
            throw PermissionDeniedException("Only author can delete comment")
        }
        
        if (this.isDeleted) {
            throw InvalidStateException("Comment already deleted")
        }
        
        this.content = "[This comment has been deleted]"
        this.isDeleted = true
        this.deletedAt = java.time.LocalDateTime.now()
        this.addDomainEvent(CommentDomainEvent.CommentDeleted(id.value.toString()))
    }

    fun like(userId: UserId) {
        if (this.isDeleted) {
            throw InvalidStateException("Cannot like deleted comment")
        }
        
        this.likeCount++
        this.addDomainEvent(CommentDomainEvent.CommentLiked(id.value.toString(), userId.value.toString()))
    }

    fun unlike(userId: UserId) {
        if (this.likeCount > 0) {
            this.likeCount--
        }
        this.addDomainEvent(CommentDomainEvent.CommentUnliked(id.value.toString(), userId.value.toString()))
    }

    fun report() {
        this.isReported = true
        this.addDomainEvent(CommentDomainEvent.CommentUpdated(id.value.toString()))
    }

    fun pin(userId: UserId) {
        // 관리자 권한 검증 필요 (구현)
        this.isPinned = true
        this.addDomainEvent(CommentDomainEvent.CommentUpdated(id.value.toString()))
    }

    fun unpin(userId: UserId) {
        // 관리자 권한 검증 필요 (구현)
        this.isPinned = false
        this.addDomainEvent(CommentDomainEvent.CommentUpdated(id.value.toString()))
    }

    fun addReply(reply: Comment) {
        this.replyCount++
    }

    fun removeReply() {
        if (this.replyCount > 0) {
            this.replyCount--
        }
    }

    fun addAttachment(attachment: CommentAttachment) {
        this.attachments.add(attachment)
    }

    fun canReply(): Boolean {
        return this.level < 5
    }

    fun isRoot(): Boolean {
        return this.parentId == null
    }

    fun isEditableBy(userId: UserId): Boolean {
        return this.authorId == userId && !this.isDeleted
    }

    private fun generateThreadId(): Long {
        return System.currentTimeMillis()
    }

    override fun toString(): String {
        return "Comment(id=${id}, postId=${postId.value}, authorId=${authorId.value}, level=$level, deleted=$isDeleted)"
    }
}