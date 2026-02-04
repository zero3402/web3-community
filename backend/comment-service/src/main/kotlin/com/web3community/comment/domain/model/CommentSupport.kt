package com.web3community.comment.domain.model

import jakarta.persistence.*

// 댓글 리엑션 엔티티
@Entity
@Table(name = "comment_reactions")
class CommentReaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    val comment: Comment,

    @Column(name = "user_id", nullable = false)
    val userId: com.web3community.sharedkernel.domain.UserId,

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, columnDefinition = "ENUM('LIKE', 'DISLIKE', 'LAUGH', 'LOVE', 'ANGRY', 'SAD')")
    val reactionType: ReactionType,

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    
    // JPA 생성자
    protected constructor()
    
    // 도메인 생성자
    constructor(
        comment: Comment,
        userId: com.web3community.sharedkernel.domain.UserId,
        reactionType: ReactionType
    ) : this() {
        this.comment = comment
        this.userId = userId
        this.reactionType = reactionType
    }
}

// 리엑션 타입 Enum
enum class ReactionType {
    LIKE,
    DISLIKE,
    LAUGH,
    LOVE,
    ANGRY,
    SAD
}

// 댓글 첨부파일 엔티티
@Entity
@Table(name = "comment_attachments")
class CommentAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    val comment: Comment,

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
    val displayOrder: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    
    // JPA 생성자
    protected constructor()
    
    // 도메인 생성자
    constructor(
        comment: Comment,
        filename: String,
        originalFilename: String,
        mimeType: String,
        fileSize: Long,
        filePath: String,
        type: AttachmentType = AttachmentType.FILE
    ) : this() {
        this.comment = comment
        this.filename = filename
        this.originalFilename = originalFilename
        this.mimeType = mimeType
        this.fileSize = fileSize
        this.filePath = filePath
        this.type = type
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