package com.web3community.user.service

import com.web3community.user.dto.*
import com.web3community.user.repository.UserRepository
import com.web3community.user.entity.User
import com.web3community.util.JwtUtil
import com.web3community.util.PasswordEncoderUtil
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * 사용자 서비스 클래스
 * 사용자 관련 비즈니스 로직을 처리
 * 리액티브 프로그래밍 방식으로 구현
 */
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoderUtil: PasswordEncoderUtil,
    private val jwtUtil: JwtUtil
) {

    /**
     * 사용자 등록
     * 이메일 중복 확인 후 비밀번호 암호화하여 저장
     */
    fun registerUser(request: UserRegistrationRequest): Mono<UserResponse> {
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
                                    password = passwordEncoderUtil.encode(request.password),
                                    displayName = request.displayName ?: request.username,
                                    bio = request.bio,
                                    profileImageUrl = request.profileImageUrl,
                                    role = request.role,
                                    isActive = true,
                                    isEmailVerified = false
                                )
                                
                                userRepository.save(user)
                                    .map { it.toResponse() }
                            }
                        }
                }
            }
    }

    /**
     * 사용자 인증 (로그인)
     * 이메일과 비밀번호로 사용자 인증 후 JWT 토큰 발급
     */
    fun authenticateUser(request: UserLoginRequest): Mono<LoginResponse> {
        return userRepository.findByEmail(request.email)
            .flatMap { user ->
                if (!user.isActive) {
                    Mono.error(IllegalArgumentException("비활성화된 계정입니다."))
                } else if (passwordEncoderUtil.matches(request.password, user.password)) {
                    // 마지막 로그인 시간 업데이트
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

    /**
     * 전체 사용자 목록 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    fun getAllUsers(page: Int, size: Int, sortBy: String, direction: String): Flux<UserResponse> {
        val sort = Sort.Direction.fromStringOrNull(direction) ?: Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(sort, sortBy))
        
        return userRepository.findAllBy(pageable)
            .map { it.toResponse() }
    }

    /**
     * ID로 사용자 조회
     */
    @Transactional(readOnly = true)
    fun getUserById(id: Long): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { it.toResponse() }
    }

    /**
     * 이메일로 사용자 조회
     */
    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): Mono<UserResponse> {
        return userRepository.findByEmail(email)
            .map { it.toResponse() }
    }

    /**
     * 사용자 이름으로 사용자 검색
     */
    @Transactional(readOnly = true)
    fun searchUsersByUsername(username: String, page: Int, size: Int): Flux<UserResponse> {
        val pageable = PageRequest.of(page, size)
        return userRepository.findByUsernameContainingIgnoreCase(username, pageable)
            .map { it.toResponse() }
    }

    /**
     * 이메일로 사용자 검색 (부분 일치)
     */
    @Transactional(readOnly = true)
    fun searchUsersByEmail(email: String, page: Int, size: Int): Flux<UserResponse> {
        val pageable = PageRequest.of(page, size)
        return userRepository.findByEmailContainingIgnoreCase(email, pageable)
            .map { it.toResponse() }
    }

    /**
     * 사용자 정보 전체 수정
     */
    fun updateUser(id: Long, request: UserUpdateRequest): Mono<UserResponse> {
        return userRepository.findById(id)
            .flatMap { user ->
                request.username?.let { username ->
                    if (username != user.username) {
                        usernameExists(username)
                            .flatMap { exists ->
                                if (exists) {
                                    Mono.error(IllegalArgumentException("이미 사용 중인 사용자 이름입니다."))
                                } else {
                                    Mono.just(username)
                                }
                            }
                    } else {
                        Mono.just(username)
                    }
                } ?: Mono.just(user.username)
            }
            .flatMap { validUsername ->
                userRepository.findById(id)
                    .map { user ->
                        user.copy(
                            username = request.username ?: user.username,
                            displayName = request.displayName ?: user.displayName,
                            bio = request.bio ?: user.bio,
                            profileImageUrl = request.profileImageUrl ?: user.profileImageUrl,
                            location = request.location ?: user.location,
                            websiteUrl = request.websiteUrl ?: user.websiteUrl,
                            phoneNumber = request.phoneNumber ?: user.phoneNumber,
                            updatedAt = LocalDateTime.now()
                        )
                    }
                    .flatMap { userRepository.save(it) }
                    .map { it.toResponse() }
            }
    }

    /**
     * 사용자 정보 부분 수정
     */
    fun patchUser(id: Long, request: UserPatchRequest): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                user.copy(
                    displayName = request.displayName ?: user.displayName,
                    bio = request.bio ?: user.bio,
                    profileImageUrl = request.profileImageUrl ?: user.profileImageUrl,
                    location = request.location ?: user.location,
                    websiteUrl = request.websiteUrl ?: user.websiteUrl,
                    phoneNumber = request.phoneNumber ?: user.phoneNumber,
                    updatedAt = LocalDateTime.now()
                )
            }
            .flatMap { userRepository.save(it) }
            .map { it.toResponse() }
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(id: Long, request: PasswordChangeRequest): Mono<String> {
        if (request.newPassword != request.confirmPassword) {
            return Mono.error(IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다."))
        }

        return userRepository.findById(id)
            .flatMap { user ->
                if (passwordEncoderUtil.matches(request.currentPassword, user.password)) {
                    val updatedUser = user.copy(
                        password = passwordEncoderUtil.encode(request.newPassword),
                        updatedAt = LocalDateTime.now()
                    )
                    userRepository.save(updatedUser)
                        .map { "비밀번호가 성공적으로 변경되었습니다." }
                } else {
                    Mono.error(IllegalArgumentException("현재 비밀번호가 올바르지 않습니다."))
                }
            }
    }

    /**
     * 사용자 소프트 삭제
     */
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
    }

    /**
     * 사용자 영구 삭제
     */
    fun permanentlyDeleteUser(id: Long): Mono<String> {
        return userRepository.deleteById(id)
            .map { "사용자가 영구적으로 삭제되었습니다." }
    }

    /**
     * 사용자 활성화 상태 토글
     */
    fun toggleUserActive(id: Long): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                user.copy(
                    isActive = !user.isActive,
                    updatedAt = LocalDateTime.now()
                )
            }
            .flatMap { userRepository.save(it) }
            .map { it.toResponse() }
    }

    /**
     * 사용자 역할 변경
     */
    fun changeUserRole(id: Long, request: RoleChangeRequest): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                user.copy(
                    role = request.role,
                    updatedAt = LocalDateTime.now()
                )
            }
            .flatMap { userRepository.save(it) }
            .map { it.toResponse() }
    }

    /**
     * 프로필 이미지 업데이트
     */
    fun updateProfileImage(id: Long, imageUrl: String): Mono<UserResponse> {
        return userRepository.findById(id)
            .map { user ->
                user.copy(
                    profileImageUrl = imageUrl,
                    updatedAt = LocalDateTime.now()
                )
            }
            .flatMap { userRepository.save(it) }
            .map { it.toResponse() }
    }

    /**
     * 사용자 통계 정보 조회
     */
    @Transactional(readOnly = true)
    fun getUserStats(): Mono<UserStatsResponse> {
        val totalUsers = userRepository.count()
        val activeUsers = userRepository.countByIsActive(true)
        val inactiveUsers = userRepository.countByIsActive(false)
        val verifiedUsers = userRepository.countByIsEmailVerified(true)
        val unverifiedUsers = userRepository.countByIsEmailVerified(false)
        
        val oneMonthAgo = LocalDateTime.now().minusMonths(1)
        val newUsersThisMonth = userRepository.countByCreatedAtAfter(oneMonthAgo)
        
        return Mono.zip(
            totalUsers,
            activeUsers,
            inactiveUsers,
            verifiedUsers,
            unverifiedUsers,
            newUsersThisMonth
        ) { total, active, inactive, verified, unverified, newThisMonth ->
            UserStatsResponse(
                totalUsers = total,
                activeUsers = active,
                inactiveUsers = inactive,
                verifiedUsers = verified,
                unverifiedUsers = unverified,
                newUsersThisMonth = newThisMonth,
                usersByRole = mapOf(
                    UserRole.USER to userRepository.countByRole(UserRole.USER),
                    UserRole.MODERATOR to userRepository.countByRole(UserRole.MODERATOR),
                    UserRole.ADMIN to userRepository.countByRole(UserRole.ADMIN)
                )
            )
        }
    }

    /**
     * 최근 가입한 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getRecentUsers(limit: Int): Flux<UserResponse> {
        return userRepository.findAllByOrderByCreatedAtDesc()
            .take(limit.toLong())
            .map { it.toResponse() }
    }

    /**
     * 활성 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getActiveUsers(page: Int, size: Int): Flux<UserResponse> {
        val pageable = PageRequest.of(page, size)
        return userRepository.findByIsActive(true, pageable)
            .map { it.toResponse() }
    }

    /**
     * 비활성 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getInactiveUsers(page: Int, size: Int): Flux<UserResponse> {
        val pageable = PageRequest.of(page, size)
        return userRepository.findByIsActive(false, pageable)
            .map { it.toResponse() }
    }

    /**
     * 사용자 존재 여부 확인
     */
    @Transactional(readOnly = true)
    fun userExists(id: Long): Mono<Boolean> {
        return userRepository.existsById(id)
    }

    /**
     * 이메일 중복 확인
     */
    @Transactional(readOnly = true)
    fun emailExists(email: String): Mono<Boolean> {
        return userRepository.existsByEmail(email)
    }

    /**
     * 사용자 이름 중복 확인
     */
    @Transactional(readOnly = true)
    fun usernameExists(username: String): Mono<Boolean> {
        return userRepository.existsByUsername(username)
    }

    /**
     * 사용자 상세 프로필 조회
     * 통계 정보를 포함한 상세 프로필 정보를 반환
     */
    @Transactional(readOnly = true)
    fun getUserProfile(id: Long): Mono<UserProfileResponse> {
        return userRepository.findById(id)
            .map { user ->
                val stats = UserStats(
                    postCount = 0L, // TODO: post-service 연동 필요
                    commentCount = 0L, // TODO: comment-service 연동 필요
                    followerCount = 0L, // TODO: follow 기능 구현 필요
                    followingCount = 0L, // TODO: follow 기능 구현 필요
                    likesReceived = 0L, // TODO: like 기능 구현 필요
                    dislikesReceived = 0L // TODO: dislike 기능 구현 필요
                )
                
                UserProfileResponse(
                    user = user.toResponse(),
                    stats = stats
                )
            }
    }
}