package com.web3.community.user.service

import com.web3.community.common.dto.PageResponse
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.user.dto.CreateUserRequest
import com.web3.community.user.dto.UpdateUserRequest
import com.web3.community.user.dto.UserResponse
import com.web3.community.user.entity.User
import com.web3.community.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun createUser(request: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }

        val user = User(
            email = request.email,
            nickname = request.nickname
        )
        return UserResponse.from(userRepository.save(user))
    }

    fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        return UserResponse.from(user)
    }

    @Transactional
    fun updateUser(userId: Long, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        request.nickname?.let { user.nickname = it }
        request.bio?.let { user.bio = it }
        request.profileImageUrl?.let { user.profileImageUrl = it }
        user.updatedAt = LocalDateTime.now()

        return UserResponse.from(userRepository.save(user))
    }

    fun getAllUsers(pageable: Pageable): PageResponse<UserResponse> {
        val page = userRepository.findAll(pageable)
        return PageResponse(
            content = page.content.map { UserResponse.from(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            isFirst = page.isFirst,
            isLast = page.isLast
        )
    }

    @Transactional
    fun updateUserRole(id: Long, role: String): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        user.role = role
        user.updatedAt = LocalDateTime.now()

        return UserResponse.from(userRepository.save(user))
    }

    @Transactional
    fun deactivateUser(id: Long) {
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        user.active = false
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
    }
}
