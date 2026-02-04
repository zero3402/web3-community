package com.web3community.post.entity

import jakarta.persistence.*

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, unique = true)
    val name: String,
    
    @Column(unique = true)
    val slug: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(name = "parent_id")
    val parentId: Long? = null,
    
    @Column(name = "display_order")
    val displayOrder: Int = 0,
    
    @Column(name = "is_active")
    val isActive: Boolean = true,
    
    @Column(name = "post_count")
    val postCount: Long = 0,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val posts: MutableList<com.web3community.post.entity.Post> = mutableListOf()
)