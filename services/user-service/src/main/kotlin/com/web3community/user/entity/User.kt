package com.web3community.user.entity

import com.web3community.user.dto.UserRole
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@Entity
@Table(name = "users")
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: UserRole = UserRole.USER,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    
    constructor() : this(0, "", "", "")
}