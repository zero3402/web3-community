package com.web3.community.comment.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("comments")
data class Comment(
    @Id
    val id: Long? = null,

    val postId: Long,
    val parentId: Long? = null,
    val depth: Int = 0,

    val authorId: Long,
    val authorNickname: String,

    var content: String,

    var likeCount: Long = 0,
    var deleted: Boolean = false,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
