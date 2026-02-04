package com.web3community.user.application.service

import com.web3community.user.domain.service.UserDomainService
import com.web3community.user.application.dto.*
import com.web3community.user.domain.model.User
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.domain.Email
import com.web3community.sharedkernel.domain.Username
import com.web3community.sharedkernel.domain.Password
import com.web3community.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 사용자 애플리케이션 서비스
@Service
@Transactional
class UserApplicationService(
    private val userDomainService: UserDomainService,
    private val userRepository: UserRepository
) {

    fun registerUser(request: RegisterUserRequest): UserResponse {
        val user = userDomainService.createUser(
            email = request.email,
            username = request.username,
            password = request.password,
            displayName = request.displayName,
            role = request.role
        )
        
        return user.toResponse()
    }

    fun loginUser(request: LoginUserRequest): LoginResponse {
        val user = userDomainService.authenticateUser(request.email, request.password)
        user.login()
        
        val updatedUser = userRepository.save(user)
        
        return LoginResponse(
            user = updatedUser.toResponse(),
            token = generateToken(updatedUser.id.value.toString())
        )
    }

    fun getUserProfile(userId: Long): UserProfileResponse {
        val user = userRepository.findById(UserId.of(userId))
            .orElseThrow { com.web3community.sharedkernel.domain.DomainObjectNotFoundException("User", userId) }
        
        val followersCount = userRepository.countFollowers(userId)
        val followingCount = userRepository.countFollowing(userId)
        
        return user.toProfileResponse(followersCount, followingCount)
    }

    fun updateUserProfile(userId: Long, request: UpdateUserProfileRequest): UserProfileResponse {
        userDomainService.updateUserProfile(
            UserId.of(userId),
            request.displayName,
            request.bio,
            request.profileImageUrl,
            request.location,
            request.websiteUrl
        )
        
        val updatedUser = userRepository.findById(UserId.of(userId)).get()
        val followersCount = userRepository.countFollowers(userId)
        val followingCount = userRepository.countFollowing(userId)
        
        return updatedUser.toProfileResponse(followersCount, followingCount)
    }

    fun changePassword(userId: Long, request: ChangePasswordRequest): String {
        userDomainService.changeUserPassword(
            UserId.of(userId),
            request.currentPassword,
            request.newPassword
        )
        
        return "Password changed successfully"
    }

    fun followUser(followerId: Long, followeeId: Long): FollowResponse {
        userDomainService.followUser(UserId.of(followerId), UserId.of(followeeId))
        
        return FollowResponse(
            message = "Successfully followed user",
            isFollowing = true
        )
    }

    fun unfollowUser(followerId: Long, followeeId: Long): FollowResponse {
        userDomainService.unfollowUser(UserId.of(followerId), UserId.of(followeeId))
        
        return FollowResponse(
            message = "Successfully unfollowed user",
            isFollowing = false
        )
    }

    fun searchUsers(query: String, page: Int = 0, size: Int = 20): UserListResponse {
        val users = userRepository.searchUsers(query)
        
        return UserListResponse(
            users = users.map { it.toResponse() },
            total = users.size.toLong(),
            page = page,
            size = size
        )
    }

    private fun generateToken(userId: String): String {
        // JWT 생성 로직 (구현 필요)
        return "generated-token-for-$userId"
    }
}

// 확장 함수
fun User.toResponse(): UserResponse {
    return UserResponse(
        id = this.id.value,
        email = this.email.value,
        username = this.username.value,
        displayName = this.displayName,
        bio = this.bio,
        profileImageUrl = this.profileImageUrl,
        location = this.location,
        websiteUrl = this.websiteUrl,
        role = this.role.name,
        isActive = this.isActive,
        isEmailVerified = this.isEmailVerified,
        lastLoginAt = this.lastLoginAt?.toString(),
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString()
    )
}

fun User.toProfileResponse(followersCount: Long, followingCount: Long): UserProfileResponse {
    return UserProfileResponse(
        id = this.id.value,
        email = this.email.value,
        username = this.username.value,
        displayName = this.displayName,
        bio = this.bio,
        profileImageUrl = this.profileImageUrl,
        location = this.location,
        websiteUrl = this.websiteUrl,
        role = this.role.name,
        isActive = this.isActive,
        isEmailVerified = this.isEmailVerified,
        lastLoginAt = this.lastLoginAt?.toString(),
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString(),
        followersCount = followersCount,
        followingCount = followingCount
    )
}