package com.web3community.user.domain.model

import com.web3community.sharedkernel.domain.UserId
import jakarta.persistence.*

// 사용자 역열 Enum
enum class UserRole {
    USER,
    MODERATOR,
    ADMIN
}

// 사용자 팔로우 관계 엔티티
@Entity
@Table(name = "user_follows", 
    uniqueConstraints = [UniqueConstraint(columnNames = ["follower_id", "followee_id"])])
class UserFollow(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    val follower: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", nullable = false)
    val followee: User,

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    
    // JPA 생성자
    protected constructor()
    
    // 도메인 생성자
    constructor(follower: User, followee: User) : this() {
        this.follower = follower
        this.followee = followee
    }
    
    override fun toString(): String {
        return "UserFollow(follower=${follower.username.value}, followee=${followee.username.value})"
    }
}