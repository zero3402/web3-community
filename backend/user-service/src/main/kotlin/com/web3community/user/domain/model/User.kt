package com.web3community.user.domain.model

import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.Email
import com.web3community.sharedkernel.domain.Username
import com.web3community.sharedkernel.domain.Password
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.events.UserDomainEvent
import jakarta.persistence.*

// 사용자 애그리게이트 루트
@Entity
@Table(name = "users")
class User(
    @Column(name = "email", nullable = false, unique = true, columnDefinition = "VARCHAR(255)")
    var email: Email,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
        }

    @Column(name = "username", nullable = false, unique = true, columnDefinition = "VARCHAR(50)")
    var username: Username,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
        }

    @Column(name = "password", nullable = false, columnDefinition = "VARCHAR(255)")
    var password: Password,
        private set

    @Column(name = "display_name", nullable = false, columnDefinition = "VARCHAR(100)")
    var displayName: String,
        set(value) {
            require(value.isNotBlank()) { "Display name cannot be blank" }
            require(value.length <= 100) { "Display name must be at most 100 characters" }
            field = value
            addDomainEvent(UserDomainEvent.UserProfileUpdated(id.value.toString()))
        }

    @Column(name = "bio", columnDefinition = "TEXT")
    var bio: String? = null,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserProfileUpdated(id.value.toString()))
        }

    @Column(name = "profile_image_url", columnDefinition = "VARCHAR(500)")
    var profileImageUrl: String? = null,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserProfileUpdated(id.value.toString()))
        }

    @Column(name = "location", columnDefinition = "VARCHAR(100)")
    var location: String? = null,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserProfileUpdated(id.value.toString()))
        }

    @Column(name = "website_url", columnDefinition = "VARCHAR(500)")
    var websiteUrl: String? = null,
        set(value) {
            field = value?.let { Url.of(it) }
            addDomainEvent(UserDomainEvent.UserProfileUpdated(id.value.toString()))
        }

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "ENUM('USER', 'MODERATOR', 'ADMIN')")
    var role: UserRole = UserRole.USER,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
        }

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
        }

    @Column(name = "is_email_verified", nullable = false)
    var isEmailVerified: Boolean = false,
        set(value) {
            field = value
            addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
        }

    @Column(name = "last_login_at")
    var lastLoginAt: java.time.LocalDateTime? = null

    // 연관관계
    @OneToMany(mappedBy = "followee", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val followers: MutableSet<UserFollow> = mutableSetOf()

    @OneToMany(mappedBy = "follower", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val following: MutableSet<UserFollow> = mutableSetOf()

    // JPA 생성자
    protected constructor() : super()

    // 도메인 생성자
    constructor(
        email: Email,
        username: Username,
        password: Password,
        displayName: String
    ) : super() {
        this.email = email
        this.username = username
        this.password = password
        this.displayName = displayName
        this.addDomainEvent(UserDomainEvent.UserCreated(id.value.toString(), username.value, email.value))
    }

    // 비즈니스 메소드
    fun changePassword(newPassword: Password, currentPassword: Password) {
        if (!this.password.value.equals(currentPassword.value)) {
            throw PermissionDeniedException("Current password is incorrect")
        }
        this.password = newPassword
        addDomainEvent(UserDomainEvent.UserPasswordChanged(id.value.toString()))
    }

    fun login() {
        if (!isActive) {
            throw InvalidStateException("User account is not active")
        }
        lastLoginAt = java.time.LocalDateTime.now()
        addDomainEvent(UserDomainEvent.UserLoggedIn(id.value.toString()))
    }

    fun logout() {
        addDomainEvent(UserDomainEvent.UserLoggedOut(id.value.toString()))
    }

    fun follow(userToFollow: User) {
        if (this.id == userToFollow.id) {
            throw BusinessRuleViolationException("Cannot follow yourself")
        }
        
        if (isFollowing(userToFollow)) {
            throw BusinessRuleViolationException("Already following this user")
        }
        
        val follow = UserFollow(follower = this, followee = userToFollow)
        this.following.add(follow)
        addDomainEvent(UserDomainEvent.UserFollowed(id.value.toString(), userToFollow.id.value.toString()))
    }

    fun unfollow(userToUnfollow: User) {
        if (this.id == userToUnfollow.id) {
            throw BusinessRuleViolationException("Cannot unfollow yourself")
        }
        
        val follow = this.following.find { it.followee.id == userToUnfollow.id }
            ?: throw BusinessRuleViolationException("Not following this user")
        
        this.following.remove(follow)
        addDomainEvent(UserDomainEvent.UserUnfollowed(id.value.toString(), userToUnfollow.id.value.toString()))
    }

    fun isFollowing(user: User): Boolean {
        return this.following.any { it.followee.id == user.id }
    }

    fun verifyEmail() {
        this.isEmailVerified = true
        addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
    }

    fun deactivate() {
        this.isActive = false
        addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
    }

    fun activate() {
        this.isActive = true
        addDomainEvent(UserDomainEvent.UserUpdated(id.value.toString()))
    }

    override fun toString(): String {
        return "User(id=${id}, username=${username.value}, email=${email.value})"
    }
}