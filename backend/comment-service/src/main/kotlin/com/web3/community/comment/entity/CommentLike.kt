package com.web3.community.comment.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("comment_likes")
data class CommentLike(
    @Id
    val id: Long? = null,
    val commentId: Long,
    val userId: Long
)
