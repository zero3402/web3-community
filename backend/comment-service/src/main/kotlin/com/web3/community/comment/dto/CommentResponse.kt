package com.web3.community.comment.dto

import com.web3.community.comment.document.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: String,
    val postId: String,
    val parentId: String?,
    val depth: Int,
    val authorId: Long,
    val authorNickname: String,
    val content: String,
    val likeCount: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val children: MutableList<CommentResponse> = mutableListOf()
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            return CommentResponse(
                id = comment.id!!,
                postId = comment.postId,
                parentId = comment.parentId,
                depth = comment.depth,
                authorId = comment.authorId,
                authorNickname = if (comment.deleted) "Deleted" else comment.authorNickname,
                content = if (comment.deleted) "This comment has been deleted." else comment.content,
                likeCount = comment.likeCount,
                deleted = comment.deleted,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt
            )
        }
    }
}
