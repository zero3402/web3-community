package com.web3.community.comment.dto

data class CommentEvent(
    val eventType: String,
    val commentId: Long,
    val postId: Long,
    val authorId: Long,
    val authorNickname: String,
    val content: String?,
    val parentId: Long?,
    val timestamp: Long = System.currentTimeMillis()
)
