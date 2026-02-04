package com.web3community.comment.entity

import jakarta.persistence.*

@Entity
@Table(name = "comment_attachments")
data class CommentAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    val comment: Comment,
    
    @Column(nullable = false)
    val filename: String,
    
    @Column(nullable = false)
    val originalFilename: String,
    
    @Column(nullable = false)
    val mimeType: String,
    
    @Column(nullable = false)
    val fileSize: Long,
    
    @Column(nullable = false)
    val filePath: String,
    
    @Column(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    
    @Column(nullable = false)
    val type: AttachmentType = AttachmentType.FILE,
    
    @Column(name = "display_order")
    val displayOrder: Int = 0,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

enum class AttachmentType {
    IMAGE,
    VIDEO,
    DOCUMENT,
    FILE,
    THUMBNAIL
}