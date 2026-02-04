package com.web3community.post.domain.model

import jakarta.persistence.*

// 태그 엔티티
@Entity
@Table(name = "tags",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name"])])
class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(50)")
    val name: String,

    @Column(name = "slug", nullable = false, columnDefinition = "VARCHAR(50)")
    val slug: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "post_count", nullable = false)
    var postCount: Long = 0L,

    @OneToMany(mappedBy = "tag", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val postTags: MutableSet<PostTag> = mutableSetOf()
) {
    
    // JPA 생성자
    protected constructor()
    
    // 도메인 생성자
    constructor(name: String, slug: String) : this() {
        this.name = name.trim()
        this.slug = slug.trim().lowercase().replace(" ", "-")
    }
    
    fun addPost() {
        postCount++
    }
    
    fun removePost() {
        if (postCount > 0) {
            postCount--
        }
    }
    
    fun updateDescription(description: String?) {
        this.description = description?.take(500)
    }
}

// 포스트-태그 연관 엔티티
@Entity
@Table(name = "post_tags",
    uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "tag_id"])])
class PostTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    val tag: Tag,

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    
    // JPA 생성자
    protected constructor()
    
    // 도메인 생성자
    constructor(post: Post, tag: Tag) : this() {
        this.post = post
        this.tag = tag
    }
    
    override fun toString(): String {
        return "PostTag(postId=${post.id}, tag=${tag.name})"
    }
}

// 포스트 첨부파일 엔티티
@Entity
@Table(name = "post_attachments")
class PostAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @Column(name = "filename", nullable = false, columnDefinition = "VARCHAR(255)")
    val filename: String,

    @Column(name = "original_filename", nullable = false, columnDefinition = "VARCHAR(255)")
    val originalFilename: String,

    @Column(name = "mime_type", nullable = false, columnDefinition = "VARCHAR(100)")
    val mimeType: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "file_path", nullable = false, columnDefinition = "VARCHAR(500)")
    val filePath: String,

    @Column(name = "thumbnail_path", columnDefinition = "VARCHAR(500)")
    val thumbnailPath: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "ENUM('IMAGE', 'VIDEO', 'DOCUMENT', 'FILE', 'THUMBNAIL')")
    val type: AttachmentType,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    
    // JPA 생성자
    protected constructor()
    
    // 도메인 생성자
    constructor(
        post: Post,
        filename: String,
        originalFilename: String,
        mimeType: String,
        fileSize: Long,
        filePath: String,
        type: AttachmentType = AttachmentType.FILE
    ) : this() {
        this.post = post
        this.filename = filename
        this.originalFilename = originalFilename
        this.mimeType = mimeType
        this.fileSize = fileSize
        this.filePath = filePath
        this.type = type
    }
    
    fun updateDisplayOrder(order: Int) {
        displayOrder = order
    }
    
    fun updateThumbnail(thumbnailPath: String?) {
        this.thumbnailPath = thumbnailPath
    }
    
    override fun toString(): String {
        return "PostAttachment(id=${id}, filename=${originalFilename}, type=$type)"
    }
}

// 첨부파일 타입 Enum
enum class AttachmentType {
    IMAGE,
    VIDEO,
    DOCUMENT,
    FILE,
    THUMBNAIL
}