package com.web3community.auth.service

import com.web3community.auth.dto.*
import com.web3community.auth.entity.AuthSession
import com.web3community.auth.repository.AuthRepository
import com.web3community.user.repository.UserRepository
import com.web3community.user.repository.UserPermissionRepository
import com.web3community.user.entity.UserPermission
import com.web3community.util.JwtUtil
import com.web3community.util.PasswordEncoderUtil
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
@Transactional
class AuthService(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val passwordEncoder: PasswordEncoderUtil
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<org.springframework.security.core.userdetails.UserDetails> {
        return authRepository.findByUsername(username)
            .cast(org.springframework.security.core.userdetails.User::class.java)
    }

    override fun findByEmail(email: String): Mono<org.springframework.security.core.userdetails.UserDetails> {
        return authRepository.findByEmail(email)
            .cast(org.springframework.security.core.userdetails.User::class.java)
    }

    override fun updateUser(user: org.springframework.security.core.userdetails.UserDetails): Mono<org.springframework.security.core.userdetails.UserDetails> {
        return userRepository.findById(user.username)
            .map { user ->
                // Update user info
                user
            }
            .cast(org.springframework.security.core.userdetails.User::class.java)
    }

    fun refreshToken(request: TokenRefreshRequest): Mono<TokenResponse> {
        return jwtUtil.validateToken(request.refreshToken)
            .flatMap { claims ->
                val username = claims.subject
                return authRepository.findByUsername(username)
                    .flatMap { user ->
                        if (!user.isActive) {
                            Mono.error(IllegalArgumentException("비활성화된 계정입니다."))
                        } else {
                            val newToken = jwtUtil.generateToken(username, user.role.toString())
                            val refreshToken = jwtUtil.generateRefreshToken(username)
                            
                            TokenResponse(
                                token = newToken,
                                refreshToken = refreshToken,
                                tokenType = "Bearer",
                                expiresIn = jwtUtil.getExpirationTime()
                            )
                        }
                    }
            }
            }
    }

    fun registerUser(request: AuthRegistrationRequest): Mono<AuthResponse> {
        return userRepository.findByEmail(request.email)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(IllegalArgumentException("이미 사용 중인 이메일입니다."))
                } else {
                    userRepository.existsByUsername(request.username)
                        .flatMap { usernameExists ->
                            if (usernameExists) {
                                Mono.error(IllegalArgumentException("이미 사용 중인 사용자 이름입니다."))
                            } else {
                                val user = com.web3community.user.entity.User(
                                    username = request.username,
                                    email = request.email,
                                    password = passwordEncoder.encode(request.password),
                                    displayName = request.displayName ?: request.username,
                                    role = com.web3community.user.dto.AuthRole.valueOf(request.role.name),
                                    isActive = true,
                                    isEmailVerified = false
                                )
                                
                                userRepository.save(user)
                                    .map { savedUser ->
                                        val token = jwtUtil.generateToken(savedUser.email, savedUser.role.toString())
                                        AuthResponse(
                                            token = token,
                                            refreshToken = jwtUtil.generateRefreshToken(savedUser.email),
                                            tokenType = "Bearer",
                                            expiresIn = jwtUtil.getExpirationTime(),
                                            user = toUserProfile(savedUser)
                                        )
                                    }
                            }
                        }
                }
            }
    }

    fun authenticateUser(request: AuthLoginRequest): Mono<AuthResponse> {
        return userRepository.findByEmail(request.email)
            .flatMap { user ->
                if (!user.isActive) {
                    Mono.error(IllegalArgumentException("비활성화된 계정입니다."))
                } else if (passwordEncoderUtil.matches(request.password, user.password)) {
                    // Update last login time
                    val updatedUser = user.copy(lastLoginAt = LocalDateTime.now())
                    userRepository.save(updatedUser)
                        .map { savedUser ->
                            val token = jwtUtil.generateToken(savedUser.email, savedUser.role.toString())
                            val refreshToken = jwtUtil.generateRefreshToken(savedUser.email)
                            
                            AuthResponse(
                                token = token,
                                refreshToken = refreshToken,
                                tokenType = "Bearer",
                                expiresIn = jwtUtil.getExpirationTime(),
                                user = toUserProfile(savedUser)
                            )
                        }
                } else {
                    Mono.error(IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."))
                }
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("존재하지 않는 사용자입니다.")))
    }

    fun getUserProfile(userId: String): Mono<UserProfile> {
        return userRepository.findById(userId.toLongOrNull() ?: 0L)
            .map { user ->
                toUserProfile(user)
            }
            .switchIfEmpty(Mono.error("User not found"))
    }

    fun updateProfile(userId: String, request: ProfileUpdateRequest): Mono<UserProfile> {
        return userRepository.findById(userId.toLongOrNull() ?: 0L)
            .flatMap { user ->
                val updatedUser = user.copy(
                    displayName = request.displayName ?: user.displayName,
                    bio = request.bio ?: user.bio,
                    profileImageUrl = request.profileImageUrl ?: user.profileImageUrl,
                    location = request.location ?: user.location,
                    websiteUrl = request.websiteUrl ?: user.websiteUrl,
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(updatedUser)
                    .map { toUserProfile(updatedUser) }
            }
            .switchIfEmpty(Mono.error("User not found")))
    }

    fun changePassword(userId: String, request: PasswordChangeRequest): Mono<String> {
        if (request.newPassword != request.confirmPassword) {
            return Mono.error(IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다."))
        }

        return userRepository.findById(userId.toLongOrNull() ?: 0L)
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
            .switchIfEmpty(Mono.error("User not found")))
    }

    fun resetPassword(request: PasswordResetRequest): Mono<String> {
        return userRepository.findByEmail(request.email)
            .flatMap { user ->
                if (!user.isActive) {
                    Mono.error("비활성화된 계정입니다. 비밀번호 재설정이 불가합니다.")
                }
                
                val resetToken = jwtUtil.generateResetToken()
                // TODO: 이메일 발송 기능 구현 필요
                userRepository.save(user.copy(
                    password = passwordEncoder.encode(resetToken),
                    updatedAt = LocalDateTime.now()
                ))
                .map { "패스워드 재설정 이메일을 발송했습니다." }
            }
            .switchIfEmpty(Mono.error("User not found")))
    }

    fun resetPasswordConfirm(request: PasswordResetConfirmation): Mono<String> {
        return jwtUtil.validateToken(request.token)
            .flatMap { claims ->
                val email = claims.subject
                return userRepository.findByEmail(email)
                    .flatMap { user ->
                    if (jwtUtil.validateToken(request.token, user.passwordHash)) {
                        val resetToken = jwtUtil.generateResetToken()
                        userRepository.save(user.copy(
                            password = passwordEncoder.encode(resetToken),
                            updatedAt = LocalDateTime.now()
                        )
                        .map { "패스워드가 성공적으로 재설정되었습니다." }
                    } else {
                        Mono.error("유효하지 않은 토큰입니다.")
                    }
                }
            }
    }

    fun changeUserRole(userId: String, request: RoleChangeRequest): Mono<UserProfile> {
        return userRepository.findById(userId.toLongOrNull() ?: 0L)
            .flatMap { user ->
                val updatedUser = user.copy(
                    role = com.web3community.user.dto.AuthRole.valueOf(request.role),
                    updatedAt = LocalDateTime.now()
                )
                userRepository.save(updatedUser)
                    .map { toUserProfile(updatedUser) }
            }
            .switchIfEmpty(Mono.error("User not found")))
    }

    private fun toUserProfile(user: com.web3community.user.entity.User): UserProfile {
        return UserProfile(
            id = user.id,
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            bio = user.bio,
            profileImageUrl = user.profileImageUrl,
            role = user.role.name,
            isActive = user.isActive,
            permissions = getPermissionsForRole(user.role),
            lastLoginAt = user.lastLoginAt?.toString(),
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString()
        )
    }

    private fun getPermissionsForRole(role: com.web3community.user.dto.AuthRole): List<String> {
        return when (role) {
            com.web3community.user.dto.AuthRole.USER -> listOf("READ_PROFILE", "UPDATE_OWN_PROFILE")
            com.web3community.user.dto.AuthRole.MODERATOR -> listOf("READ_PROFILE", "UPDATE_OWN_PROFILE", "DELETE_OWN_POST", "MANAGE_USERS")
            com.web3community.user.dto.AuthRole.ADMIN -> listOf(
                "READ_PROFILE", "UPDATE_ANY_PROFILE", "DELETE_OWN_ACCOUNT", 
                "MANAGE_USERS", "MANAGE_CONTENT", "MANAGE_NOTIFICATIONS", "SYSTEM_ADMIN"
            )
        }
    }

    private fun findByUsername(username: String): Mono<UserDetails> {
        return userRepository.findByEmail(username)
            .map { user ->
                User.withUsername(user.email)
                    .password(user.password)
                    .roles(user.role.name)
                    .build()
            }
    }
    }
    }
}