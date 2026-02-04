package com.web3community.user.service

import com.web3community.user.dto.*
import com.web3community.user.entity.User
import com.web3community.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val passwordEncoder: PasswordEncoderUtil
) {

    fun registerUser(request: UserRegistrationRequest): Mono<LoginResponse> {
        return emailExists(request.email)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(IllegalArgumentException("이미 사용 중인 이메일입니다."))
                } else {
                    usernameExists(request.username)
                        .flatMap { usernameExists ->
                            if (usernameExists) {
                                Mono.error(IllegalArgumentException("이미 사용 중인 사용자 이름입니다."))
                            } else {
                                val user = User(
                                    username = request.username,
                                    email = request.email,
                                    password = passwordEncoder.encode(request.password),
                                    displayName = request.displayName ?: request.username,
                                    bio = request.bio,
                                    profileImageUrl = request.profileImageUrl,
                                    location = request.location,
                                    websiteUrl = request.websiteUrl,
                                    role = request.role,
                                    isActive = true,
                                    isEmailVerified = false
                                )

                                userRepository.save(user)
                                    .map { savedUser ->
                                        val token = jwtUtil.generateToken(savedUser.email, savedUser.role.name)
                                        LoginResponse(
                                            token = token,
                                            refreshToken = jwtUtil.generateRefreshToken(savedUser.email),
                                            expiresIn = jwtUtil.getExpirationTime(),
                                            user = savedUser.toResponse()
                                        )
                                    }
                            }
                        }
                }
            }
    }

    fun authenticateUser(request: UserLoginRequest): Mono<LoginResponse> {
        return userRepository.findByEmail(request.email)
            .flatMap { user ->
                if (!user.isActive) {
                    Mono.error(IllegalArgumentException("비활성화된 계정입니다."))
                } else if (passwordEncoder.matches(request.password, user.password)) {
                    // Update last login time
                    val updatedUser = user.copy(lastLoginAt = LocalDateTime.now())
                    userRepository.save(updatedUser)
                        .map { savedUser ->
                            val token = jwtUtil.generateToken(savedUser.email, savedUser.role.name)
                            val refreshToken = jwtUtil.generateRefreshToken(savedUser.email)
                            LoginResponse(
                                token = token,
                                refreshToken = refreshToken,
                                expiresIn = jwtUtil.getExpirationTime(),
                                user = savedUser.toResponse()
                            )
                        }
                } else {
                    Mono.error(IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."))
                }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("존재하지 않는 사용자입니다.")))
    }

    fun getAllUsers(page: Int, size: Int, sortBy: String, direction: String): Flux<UserResponse> {
        val sort = Sort.Direction.fromStringOrNull(direction) ?: Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(sort, sortBy))
        
        return userRepository.findAllBy(pageable)
            .map { it.toResponse() }
    }

    fun getUserById(id: Long): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { it.toResponse() }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun getUserByEmail(email: String): Mono<UserResponse> {
        return userRepository.findByEmail(email)
            .map { it.toResponse() }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun updateUser(id: Long, request: UserUpdateRequest): Mono<UserResponse> {
        return userRepository.findById(id)
            .flatMap { user ->
                request.username?.let { newUsername ->
                    if (newUsername != user.username) {
                        usernameExists(newUsername)
                            .flatMap { exists ->
                                if (exists) {
                                    Mono.error(IllegalArgumentException("이미 사용 중인 사용자 이름입니다."))
                                } else {
                                    Mono.just(newUsername)
                                }
                            }
                    } ?: Mono.just(user.username)
                } ?: Mono.just(user.username)
            }
            .flatMap { validUsername ->
                userRepository.findById(id)
                    .map { user ->
                        user.copy(
                            username = validUsername,
                            displayName = request.displayName ?: user.displayName,
                            bio = request.bio ?: user.bio,
                            profileImageUrl = request.profileImageUrl ?: user.profileImageUrl,
                            location = request.location ?: user.location,
                            websiteUrl = request.websiteUrl ?: user.websiteUrl,
                            updatedAt = LocalDateTime.now()
                        )
                    }
                    .flatMap { userRepository.save(it) }
                    .map { it.toResponse() }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun patchUser(id: Long, request: UserPatchRequest): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                user.copy(
                    displayName = request.displayName ?: user.displayName,
                    bio = request.bio ?: user.bio,
                    profileImageUrl = request.profileImageUrl ?: user.profileImageUrl,
                    location = request.location ?: user.location,
                    websiteUrl = request.websiteUrl ?: user.websiteUrl,
                    updatedAt = LocalDateTime.now()
                )
            }
            .flatMap { userRepository.save(it) }
            .map { it.toResponse() }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun changePassword(id: Long, request: PasswordChangeRequest): Mono<String> {
        if (request.newPassword != request.confirmPassword) {
            return Mono.error(IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다."))
        }

        return userRepository.findById(id)
            .flatMap { user ->
                if (passwordEncoderUtil.matches(request.currentPassword, user.password)) {
                    val encodedNewPassword = passwordEncoderUtil.encode(request.newPassword)
                    val updatedUser = user.copy(
                        password = encodedNewPassword,
                        updatedAt = LocalDateTime.now()
                    )
                    userRepository.save(updatedUser)
                        .map { "비밀번호가 성공적으로 변경되었습니다." }
                } else {
                    Mono.error(IllegalArgumentException("현재 비밀번호가 올바르지 않습니다."))
                }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun deleteUser(id: Long): Mono<String> {
        return userRepository.findById(id)
            .flatMap { user ->
                val deletedUser = user.copy(
                    isActive = false,
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(deletedUser)
                .map { "사용자가 성공적으로 삭제되었습니다." }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun permanentlyDeleteUser(id: Long): Mono<String> {
        return userRepository.deleteById(id)
            .map { "사용자가 영구적으로 삭제되었습니다." }
    }

    fun toggleUserActive(id: Long): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                val toggledUser = user.copy(
                    isActive = !user.isActive,
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(toggledUser)
                .map { it.toResponse() }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun changeUserRole(id: Long, request: RoleChangeRequest): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                val updatedUser = user.copy(
                    role = request.role,
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(updatedUser)
                .map { it.toResponse() }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    fun updateProfileImage(id: Long, imageUrl: String): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                val updatedUser = user.copy(
                    profileImageUrl = imageUrl,
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(updatedUser)
                .map { it.toResponse() }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("User not found")))
    }

    private fun toResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            bio = user.bio,
            profileImageUrl = user.profileImageUrl,
            role = user.role,
            isActive = user.isActive,
            isEmailVerified = user.isEmailVerified,
            location = user.location,
            websiteUrl = user.websiteUrl,
            lastLoginAt = user.lastLoginAt?.toString(),
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString()
        )
    }
}