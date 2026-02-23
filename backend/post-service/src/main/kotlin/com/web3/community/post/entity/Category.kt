package com.web3.community.post.entity

import jakarta.persistence.*

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(nullable = false)
    var displayOrder: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true
)
