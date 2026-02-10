package com.web3.community.auth.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "auth_credentials")
data class AuthCredential(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long = 0,

        @Column(unique = true, nullable = false, length = 100)
        val email: String,

        @Column(nullable = false)
        var password: String,

        @Column(nullable = false, length = 20)
        @Enumerated(EnumType.STRING)
        val role: Role = Role.USER,

        @Column(nullable = false)
        val userId: Long,

        @Column(nullable = false)
        var enabled: Boolean = true,

        val createdAt: LocalDateTime = LocalDateTime.now(),

        var updatedAt: LocalDateTime = LocalDateTime.now()
)


