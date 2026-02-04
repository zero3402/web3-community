package com.web3community.user.entity

import com.web3community.user.dto.UserRole
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "users", indexes = [
    Index(name = "idx_username", columnList = "username"),
    Index(name = "idx_email", columnList = "email"),
    Index(name = "idx_role", columnList = "role"),
    Index(name = "idx_active", columnList = "is_active"),
    Index(name = "idx_created_at", columnList = "created_at")
])
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    val username: String,
    
    @Column(name = "email", nullable = false, unique = true, length = 100)
    val email: String,
    
    @Column(name = "password", nullable = false)
    val password: String,
    
    @Column(name = "display_name", length = 100)
    val displayName: String? = null,
    
    @Column(name = "bio", columnDefinition = "TEXT")
    val bio: String? = null,
    
    @Column(name = "profile_image_url", length = 255)
    val profileImageUrl: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: UserRole = UserRole.USER,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @Column(name = "is_email_verified", nullable = false)
    val isEmailVerified: Boolean = false,
    
    @Column(name = "location", length = 100)
    val location: String? = null,
    
    @Column(name = "website_url", length = 255)
    val websiteUrl: String? = null,
    
    @Column(name = "last_login_at")
    val lastLoginAt: LocalDateTime? = null,
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    // JPA requires no-arg constructor
    constructor() : this(0, "", "", "")
}