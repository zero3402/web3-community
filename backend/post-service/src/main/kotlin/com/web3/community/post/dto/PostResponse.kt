package com.web3.community.post.dto

import com.web3.community.post.document.Post
import java.time.LocalDateTime

data class PostResponse(
    val id: String,
    val title: String,
    val content: String,
    val authorId: Long,
    val authorNickname: String,
    val categoryId: String,
    val categoryName: String,
    val tags: List<String>,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                authorId = post.authorId,
                authorNickname = post.authorNickname,
                categoryId = post.categoryId,
                categoryName = post.categoryName,
                tags = post.tags,
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}

data class PostSummaryResponse(
    val id: String,
    val title: String,
    val authorNickname: String,
    val categoryName: String,
    val tags: List<String>,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostSummaryResponse {
            return PostSummaryResponse(
                id = post.id!!,
                title = post.title,
                authorNickname = post.authorNickname,
                categoryName = post.categoryName,
                tags = post.tags,
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                createdAt = post.createdAt
            )
        }
    }
}
