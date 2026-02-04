package com.web3community.post.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val content: String,
    
    @Column(nullable = false)
    val authorId: Long,
    
    @Column(name = "category_id")
    val categoryId: Long,
    
    @Column(name = "featured_image_url")
    val featuredImageUrl: String? = null,
    
    @Column(name = "excerpt")
    val excerpt: String? = null,
    
    @Column(nullable = false)
    val status: PostStatus = PostStatus.DRAFT,
    
    @Column(name = "is_pinned")
    val isPinned: Boolean = false,
    
    @Column(name = "is_featured")
    val isFeatured: Boolean = false,
    
    @Column(name = "view_count")
    val viewCount: Long = 0,
    
    @Column(name = "like_count")
    val likeCount: Long = 0,
    
    @Column(name = "comment_count")
    val commentCount: Long = 0,
    
    @Column(name = "share_count")
    val shareCount: Long = 0,
    
    @Column(name = "published_at")
    val publishedAt: LocalDateTime? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "created_by")
    val createdBy: Long? = null,
    
    @Column(name = "updated_by")
    val updatedBy: Long? = null,
    
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val tags: MutableSet<PostTag> = mutableSetOf(),
    
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val attachments: MutableList<PostAttachment> = mutableListOf(),
    
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val metrics: MutableList<PostMetric> = mutableListOf()
)

enum class PostStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
    DELETED
}