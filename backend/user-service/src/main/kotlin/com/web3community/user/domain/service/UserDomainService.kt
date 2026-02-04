package com.web3community.user.domain.service

import com.web3community.user.domain.model.User
import com.web3community.user.domain.model.UserRole
import com.web3community.sharedkernel.domain.*
import com.web3community.sharedkernel.domain.Email
import com.web3community.sharedkernel.domain.Username
import com.web3community.sharedkernel.domain.Password
import com.web3community.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 사용자 도메인 서비스
@Service
@Transactional
class UserDomainService(
    private val userRepository: UserRepository,
    private val passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder
) {

    fun createUser(
        email: String,
        username: String,
        password: String,
        displayName: String,
        role: UserRole = UserRole.USER
    ): User {
        // 중복 확인
        if (userRepository.existsByEmail(Email.of(email))) {
            throw BusinessRuleViolationException("Email already exists")
        }
        
        if (userRepository.existsByUsername(Username.of(username))) {
            throw BusinessRuleViolationException("Username already exists")
        }
        
        // 사용자 생성
        val user = User(
            email = Email.of(email),
            username = Username.of(username),
            password = Password.of(encodePassword(password)),
            displayName = displayName
        )
        
        user.role = role
        
        return userRepository.save(user)
    }

    fun authenticateUser(email: String, password: String): User {
        val user = userRepository.findByEmail(Email.of(email))
            .orElseThrow { DomainObjectNotFoundException("User", email) }
        
        if (!user.password.value.equals(password) || !passwordEncoder.matches(password, user.password.value)) {
            throw PermissionDeniedException("Invalid credentials")
        }
        
        if (!user.isActive) {
            throw InvalidStateException("Account is deactivated")
        }
        
        return user
    }

    fun changeUserPassword(
        userId: UserId,
        currentPassword: String,
        newPassword: String
    ) {
        val user = userRepository.findById(userId)
            .orElseThrow { DomainObjectNotFoundException("User", userId) }
        
        user.changePassword(Password.of(newPassword), Password.of(currentPassword))
        userRepository.save(user)
    }

    fun updateUserProfile(
        userId: UserId,
        displayName: String,
        bio: String?,
        profileImageUrl: String?,
        location: String?,
        websiteUrl: String?
    ) {
        val user = userRepository.findById(userId)
            .orElseThrow { DomainObjectNotFoundException("User", userId) }
        
        user.displayName = displayName
        user.bio = bio
        user.profileImageUrl = profileImageUrl
        user.location = location
        user.websiteUrl = websiteUrl
        
        userRepository.save(user)
    }

    fun followUser(followerId: UserId, followeeId: UserId) {
        val follower = userRepository.findById(followerId)
            .orElseThrow { DomainObjectNotFoundException("User", followerId) }
        
        val followee = userRepository.findById(followeeId)
            .orElseThrow { DomainObjectNotFoundException("User", followeeId) }
        
        follower.follow(followee)
        userRepository.save(follower)
    }

    fun unfollowUser(followerId: UserId, followeeId: UserId) {
        val follower = userRepository.findById(followerId)
            .orElseThrow { DomainObjectNotFoundException("User", followerId) }
        
        val followee = userRepository.findById(followeeId)
            .orElseThrow { DomainObjectNotFoundException("User", followeeId) }
        
        follower.unfollow(followee)
        userRepository.save(follower)
    }

    fun verifyUserEmail(userId: UserId) {
        val user = userRepository.findById(userId)
            .orElseThrow { DomainObjectNotFoundException("User", userId) }
        
        user.verifyEmail()
        userRepository.save(user)
    }

    fun deactivateUser(userId: UserId) {
        val user = userRepository.findById(userId)
            .orElseThrow { DomainObjectNotFoundException("User", userId) }
        
        user.deactivate()
        userRepository.save(user)
    }

    private fun encodePassword(password: String): String {
        return passwordEncoder.encode(password)
    }
}