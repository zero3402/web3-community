package com.web3community.post.domain.model

import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.PostId
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.events.PostDomainEvent
import jakarta.persistence.*

// 포스트 애그리게이트 루트
@Entity
@Table(name = "posts")
class Post(
    @Column(name = "title", nullable = false, columnDefinition = "VARCHAR(200)")
    var title: String,
        set(value) {
            require(value.isNotBlank()) { "Title cannot be blank" }
            require(value.length <= 200) { "Title must be at most 200 characters" }
            field = value
            addDomainEvent(PostDomainEvent.PostUpdated(id.value.toString()))
        }

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
        set(value) {
            require(value.isNotBlank()) { "Content cannot be blank" }
            field = value
            addDomainEvent(PostDomainEvent.PostUpdated(id.value.toString()))
        }

    @Column(name = "author_id", nullable = false)
    var authorId: UserId,
        private set

    @Column(name = "category_id", nullable = false)
    var categoryId: Long,
        private set

    @Column(name = "featured_image_url", columnDefinition = "VARCHAR(500)")
    var featuredImageUrl: String? = null,
        set(value) {
            field = value
            addDomainEvent(PostDomainEvent.PostUpdated(id.value.toString()))
        }

    @Column(name = "excerpt", columnDefinition = "TEXT")
    var excerpt: String? = null,
        set(value) {
            field = value
            addDomainEvent(PostDomainEvent.PostUpdated(id.value.toString()))
        }

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED', 'DELETED')")
    var status: PostStatus = PostStatus.DRAFT,
        set(value) {
            field = value
            if (value == PostStatus.PUBLISHED && this.status != PostStatus.PUBLISHED) {
                addDomainEvent(PostDomainEvent.PostPublished(id.value.toString()))
            }
            addDomainEvent(PostDomainEvent.PostUpdated(id.value.toString()))
        }

    @Column(name = "is_pinned", nullable = false)
    var isPinned: Boolean = false,
        set(value) {
            field = value
            addDomainEvent(PostDomainEvent.PostUpdated(id.value.toString()))
        }

    @Column(name = "is_featured", nullable = false)
    var isFeatured: Boolean = false,
        set(value) {
            field = value
            addDomainEvent(PostDomainEvent.PostUpdated(id.value.toString()))
        }

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0L,
        private set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L,
        private set

    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = 0L,
        private set

    @Column(name = "share_count", nullable = false)
    var shareCount: Long = 0L,
        private set

    // 연관관계
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val tags: MutableSet<PostTag> = mutableSetOf()

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val attachments: MutableSet<PostAttachment> = mutableSetOf()

    // JPA 생성자
    protected constructor() : super()

    // 도메인 생성자
    constructor(
        title: String,
        content: String,
        authorId: UserId,
        categoryId: Long
    ) : super() {
        this.title = title
        this.content = content
        this.authorId = authorId
        this.categoryId = categoryId
        this.addDomainEvent(PostDomainEvent.PostCreated(id.value.toString(), authorId.value.toString(), title))
    }

    // 비즈니스 메소드
    fun publish() {
        if (status == PostStatus.PUBLISHED) {
            throw BusinessRuleViolationException("Post is already published")
        }
        status = PostStatus.PUBLISHED
    }

    fun archive() {
        status = PostStatus.ARCHIVED
    }

    fun delete() {
        status = PostStatus.DELETED
        addDomainEvent(PostDomainEvent.PostDeleted(id.value.toString()))
    }

    fun view() {
        viewCount++
        addDomainEvent(PostDomainEvent.PostViewed(id.value.toString(), authorId.value.toString()))
    }

    fun like(userId: UserId) {
        if (status != PostStatus.PUBLISHED) {
            throw InvalidStateException("Cannot like unpublished post")
        }
        likeCount++
        addDomainEvent(PostDomainEvent.PostLiked(id.value.toString(), userId.value.toString()))
    }

    fun unlike(userId: UserId) {
        if (likeCount > 0) {
            likeCount--
        }
        addDomainEvent(PostDomainEvent.PostUnliked(id.value.toString(), userId.value.toString()))
    }

    fun share(userId: UserId) {
        if (status != PostStatus.PUBLISHED) {
            throw InvalidStateException("Cannot share unpublished post")
        }
        shareCount++
        addDomainEvent(PostDomainEvent.PostShared(id.value.toString(), userId.value.toString()))
    }

    fun addTag(tagName: String) {
        val normalizedTag = tagName.trim().lowercase()
        if (normalizedTag.isNotBlank()) {
            val tag = PostTag(this, Tag(normalizedTag))
            tags.add(tag)
        }
    }

    fun removeTag(tagName: String) {
        val normalizedTag = tagName.trim().lowercase()
        tags.removeIf { it.tag.name == normalizedTag }
    }

    fun incrementCommentCount() {
        commentCount++
    }

    fun decrementCommentCount() {
        if (commentCount > 0) {
            commentCount--
        }
    }

    fun isEditableBy(userId: UserId): Boolean {
        return authorId == userId && status != PostStatus.DELETED
    }

    override fun toString(): String {
        return "Post(id=${id}, title=$title, authorId=${authorId.value}, status=$status)"
    }
}

// 포스트 상태 Enum
enum class PostStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
    DELETED
}