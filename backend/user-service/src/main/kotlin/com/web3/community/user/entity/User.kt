package com.web3.community.user.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 50)
    var nickname: String,

    @Column(unique = true, nullable = false, length = 100)
    val email: String,

    @Column(length = 500)
    var bio: String? = null,

    @Column(length = 255)
    var profileImageUrl: String? = null,

    @Column(nullable = false, length = 20)
    var role: String = "USER",

    @Column(nullable = false)
    var active: Boolean = true,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
