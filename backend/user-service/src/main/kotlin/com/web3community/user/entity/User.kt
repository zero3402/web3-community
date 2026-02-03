package com.web3community.user.entity

import com.web3community.user.dto.UserResponse
import com.web3community.user.dto.UserRole
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

/**
 * 사용자 엔티티 클래스
 * 데이터베이스의 users 테이블과 매핑됨
 * JPA 엔티티이며 리액티브 프로그래밍을 지원
 */
@Entity
@Table(name = "users", indexes = [
    Index(name = "idx_username", columnList = "username"),
    Index(name = "idx_email", columnList = "email"),
    Index(name = "idx_role", columnList = "role"),
    Index(name = "idx_active", columnList = "is_active"),
    Index(name = "idx_created_at", columnList = "created_at"),
    Index(name = "idx_last_login_at", columnList = "last_login_at")
])
data class User(
    
    /**
     * 사용자 고유 ID (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    /**
     * 사용자 이름 (고유값)
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    val username: String,
    
    /**
     * 사용자 이메일 (고유값)
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    val email: String,
    
    /**
     * 비밀번호 (암호화되어 저장)
     */
    @Column(name = "password", nullable = false)
    val password: String,
    
    /**
     * 표시 이름 (화면에 표시될 이름)
     */
    @Column(name = "display_name", length = 100)
    val displayName: String?,
    
    /**
     * 자기소개
     */
    @Column(name = "bio", columnDefinition = "TEXT")
    val bio: String?,
    
    /**
     * 프로필 이미지 URL
     */
    @Column(name = "profile_image_url", length = 255)
    val profileImageUrl: String?,
    
    /**
     * 사용자 역할 (USER, MODERATOR, ADMIN)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: UserRole,
    
    /**
     * 활성화 상태 (true: 활성, false: 비활성)
     */
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    /**
     * 이메일 인증 상태 (true: 인증됨, false: 인증 안됨)
     */
    @Column(name = "is_email_verified", nullable = false)
    val isEmailVerified: Boolean = false,
    
    /**
     * 위치 정보
     */
    @Column(name = "location", length = 100)
    val location: String?,
    
    /**
     * 개인 웹사이트 URL
     */
    @Column(name = "website_url", length = 255)
    val websiteUrl: String?,
    
    /**
     * 전화번호
     */
    @Column(name = "phone_number", length = 20)
    val phoneNumber: String?,
    
    /**
     * 마지막 로그인 시간
     */
    @Column(name = "last_login_at")
    val lastLoginAt: LocalDateTime? = null,
    
    /**
     * 계정 생성 시간 (자동 설정)
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    /**
     * 마지막 수정 시간 (자동 업데이트)
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) : Persistable<Long> {

    /**
     * 새로운 엔티티인지 확인하는 메소드
     * @return true: 새로운 엔티티, false: 기존 엔티티
     */
    @Transient
    override fun isNew(): Boolean = id == 0L

    /**
     * 엔티티를 UserResponse DTO로 변환
     * 민감 정보(비밀번호 등)는 제외됨
     * @return UserResponse DTO
     */
    fun toResponse(): UserResponse {
        return UserResponse(
            id = id,
            username = username,
            email = email,
            displayName = displayName,
            bio = bio,
            profileImageUrl = profileImageUrl,
            role = role,
            isActive = isActive,
            isEmailVerified = isEmailVerified,
            location = location,
            websiteUrl = websiteUrl,
            phoneNumber = phoneNumber,
            lastLoginAt = lastLoginAt?.toString(),
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )
    }

    /**
     * equals 메소드 재정의
     * ID 기준으로 동일성 비교
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    /**
     * hashCode 메소드 재정의
     * ID 기준으로 해시코드 생성
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }

    /**
     * toString 메소드 재정의
     * 민감 정보를 제외한 필드 정보 출력
     */
    override fun toString(): String {
        return "User(id=$id, username='$username', email='$email', displayName=$displayName, role=$role, isActive=$isActive, isEmailVerified=$isEmailVerified)"
    }

    /**
     * copy 메소드 확장
     * JPA 엔티티의 불변성을 유지하며 필요한 필드만 수정
     */
    fun copy(
        id: Long = this.id,
        username: String = this.username,
        email: String = this.email,
        password: String = this.password,
        displayName: String? = this.displayName,
        bio: String? = this.bio,
        profileImageUrl: String? = this.profileImageUrl,
        role: UserRole = this.role,
        isActive: Boolean = this.isActive,
        isEmailVerified: Boolean = this.isEmailVerified,
        location: String? = this.location,
        websiteUrl: String? = this.websiteUrl,
        phoneNumber: String? = this.phoneNumber,
        lastLoginAt: LocalDateTime? = this.lastLoginAt,
        createdAt: LocalDateTime = this.createdAt,
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): User {
        return User(
            id = id,
            username = username,
            email = email,
            password = password,
            displayName = displayName,
            bio = bio,
            profileImageUrl = profileImageUrl,
            role = role,
            isActive = isActive,
            isEmailVerified = isEmailVerified,
            location = location,
            websiteUrl = websiteUrl,
            phoneNumber = phoneNumber,
            lastLoginAt = lastLoginAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}