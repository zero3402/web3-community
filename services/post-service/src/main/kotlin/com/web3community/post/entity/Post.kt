package com.web3community.post.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Document(collection = "posts")
data class Post(
    @Id
    val id: String? = null,
    
    val title: String,
    
    val content: String,
    
    val authorId: Long,
    
    val authorName: String,
    
    val category: String? = null,
    
    val tags: List<String> = emptyList(),
    
    val likeCount: Long = 0,
    
    val viewCount: Long = 0,
    
    val isActive: Boolean = true,
    
    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    constructor() : this(null, "", "", 0, "")
}