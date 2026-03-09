package com.web3.community.post.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false)
    val authorId: Long,

    @Column(nullable = false, length = 100)
    val authorNickname: String,

    @Column(nullable = false)
    var categoryId: Long,

    @Column(nullable = false, length = 100)
    var categoryName: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_tags", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "tag", length = 100)
    var tags: MutableList<String> = mutableListOf(),

    @Column(nullable = false)
    var viewCount: Long = 0,

    @Column(nullable = false)
    var likeCount: Long = 0,

    @Column(nullable = false)
    var commentCount: Long = 0,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_likes", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "user_id")
    val likedUserIds: MutableSet<Long> = mutableSetOf(),

    @Column(nullable = false)
    var published: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
