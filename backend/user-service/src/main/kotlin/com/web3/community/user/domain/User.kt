# =============================================================================
// 🎯 User Service - 사용자 관리 서비스
// =============================================================================
// 설명: 사용자 CRUD, 인증, 프로필 관리
// 기술: Spring Boot + WebFlux + Kotlin + MySQL
// 목적: 사용자 데이터 관리, JWT 인증
// 특징: Redis 세션, 이메일 인증, 소셜 로그인
// =============================================================================

package com.web3.community.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime

// =============================================================================
// 📦 엔티티 정의 (JPA)
// =============================================================================
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    
    @Column(nullable = false)
    @Column(unique = true)
    var username: String = "",
    
    @Column(nullable = false)
    var email: String = "",
    
    @Column(nullable = false)
    @Column(name = "password_hash", length = 100)
    var passwordHash: String = "",
    
    @Column(nullable = false)
    var nickname: String = "",
    
    @Column(nullable = false, length = 20)
    var profileImage: String? = null,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    var bio: String? = null,
    
    @CreatedDate
    @LastModifiedDate
    
    @Enumerated(Enum)
    enum class Role {
        ADMIN, MODERATOR, USER
    }
    
    @ElementCollection(fetch = FetchType.LAZY)
    @Table(name = "user_roles")
    class UserRole(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,
        
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user")
        private val user: User,
        
        @Enumerated(Enum(value = Role.USER)
        var role: Role = Role.USER,
        
        @Column(nullable = false)
        val grantedAt: LocalDateTime = LocalDateTime.now()
    )
)

    // =============================================================================
    // 권한 관련
    // =============================================================================
    @ElementCollection(fetch = FetchType.LAZY)
    @Table(name = "user_permissions")
    class Permission {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0
        
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinTable(name = "user_role")
        private val user: User,
        
        @Column(unique = true)
        @Column(nullable = false)
        var name: String,
        description: String,
        resource: String
        action: String
        
        @Column(nullable = false, columnDefinition = "TEXT")
        conditions: String? = null
    )

    @Enumerated(Enum)
    enum Permission {
        READ_POSTS, WRITE_POSTS, DELETE_POSTS, COMMENT_POSTS, MANAGE_USER, MANAGE_ADMIN, ADMIN_ALL
    }
)

// =============================================================================
    // 활동 기록
    // =============================================================================
    @ElementCollection(fetch = FetchType.LAZY)
    @Table(name = "user_activities")
    class UserActivity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,
        
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user")
        private val user: User,
        
        @Column(nullable = false, length = 50)
        val action: String,
        metadata: String,
        ipAddress: String,
        userAgent: String,
        createdAt: LocalDateTime = LocalDateTime.now(),
        
        @Column(nullable = false)
        description: String? = null
    )
}

    // =============================================================================
    // 성별 정보
    // =============================================================================
    @ElementCollection(fetch = FetchType.LAZY)
    @Table(name = "user_profiles")
    class UserProfile {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,
        
        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user")
        private val user: User,
        
        @Column(nullable = false)
        val firstName: String,
        lastName: String,
        dateOfBirth: LocalDate? = null,
        gender: String? = null,
        phone: String? = null,
        address: String? = null,
        website: String? = null,
        company: String? = null,
        jobTitle: String? = null,
        bio: String? = null,
        skills: List<String>? = null,
        social: Map<String, String>? = null,
        
        @Column(nullable = false, columnDefinition = "TEXT")
        preferences: String? = null,
        
        @CreatedDate
        @LastModifiedDate
    }
}

// =============================================================================
    // ⚡ 통계 정보
    // =============================================================================
    @ElementCollection(fetch = FetchType.LAZY)
    @Table(name = "user_stats")
    class UserStats {
        @Id
        @eneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,
        
        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user")
        private val user: User,
        
        @Column(nullable = false)
        val lastLoginAt: LocalDateTime? = null,
        var totalPosts: Long = 0,
        var totalComments: Long = 0,
        var totalLikes: Long = 0,
        var level: String = "BEGINNER",
        points: Long = 0,
        created_at: LocalDateTime = LocalDateTime.now(),
        updated_at: LocalDateTime.now()
    }
)