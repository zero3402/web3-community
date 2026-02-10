package com.web3.community.comment.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "comments")
data class Comment(
    @Id
    val id: String? = null,

    @Indexed
    val postId: String,

    val parentId: String? = null,
    val depth: Int = 0,

    val authorId: Long,
    val authorNickname: String,

    var content: String,

    var likeCount: Long = 0,
    val likedUserIds: MutableSet<Long> = mutableSetOf(),
    var deleted: Boolean = false,

    @Indexed
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
