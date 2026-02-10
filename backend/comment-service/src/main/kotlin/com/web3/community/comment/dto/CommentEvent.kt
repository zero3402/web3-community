package com.web3.community.comment.dto

data class CommentEvent(
    val eventType: String,
    val commentId: String,
    val postId: String,
    val authorId: Long,
    val authorNickname: String,
    val content: String?,
    val parentId: String?,
    val timestamp: Long = System.currentTimeMillis()
)
