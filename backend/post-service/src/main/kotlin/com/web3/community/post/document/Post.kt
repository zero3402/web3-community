package com.web3.community.post.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "posts")
data class Post(
    @Id
    val id: String? = null,

    var title: String,
    var content: String,

    @Indexed
    val authorId: Long,
    val authorNickname: String,

    @Indexed
    var categoryId: String,
    var categoryName: String,

    var tags: List<String> = emptyList(),

    var viewCount: Long = 0,
    var likeCount: Long = 0,
    var commentCount: Long = 0,
    val likedUserIds: MutableSet<Long> = mutableSetOf(),

    var published: Boolean = true,

    @Indexed
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
