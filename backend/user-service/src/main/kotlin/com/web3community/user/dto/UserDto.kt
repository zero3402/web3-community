package com.web3community.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 사용자 등록 요청 DTO
 * 회원가입 시 필요한 데이터를 담고 있음
 */
data class UserRegistrationRequest(
    @field:NotBlank(message = "사용자 이름은 필수입니다.")
    @field:Size(min = 3, max = 50, message = "사용자 이름은 3~50자 사이여야 합니다.")
    val username: String,
    
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8~100자 사이여야 합니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val password: String,
    
    @field:Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
    val displayName: String? = null,
    
    @field:Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    val bio: String? = null,
    
    @field:Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
    val profileImageUrl: String? = null,
    
    val role: UserRole = UserRole.USER
)

/**
 * 사용자 로그인 요청 DTO
 * 인증에 필요한 이메일과 비밀번호를 담고 있음
 */
data class UserLoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val password: String
)

/**
 * 사용자 정보 수정 요청 DTO
 * 모든 필드를 수정할 수 있는 전체 수정용 DTO
 */
data class UserUpdateRequest(
    @field:Size(min = 3, max = 50, message = "사용자 이름은 3~50자 사이여야 합니다.")
    val username: String? = null,
    
    @field:Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
    val displayName: String? = null,
    
    @field:Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    val bio: String? = null,
    
    @field:Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
    val profileImageUrl: String? = null,
    
    @field:Size(max = 100, message = "위치는 100자 이하여야 합니다.")
    val location: String? = null,
    
    @field:Size(max = 255, message = "웹사이트 URL은 255자 이하여야 합니다.")
    val websiteUrl: String? = null,
    
    @field:Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    val phoneNumber: String? = null
)

/**
 * 사용자 부분 수정 요청 DTO
 * 특정 필드만 수정할 수 있는 부분 수정용 DTO
 */
data class UserPatchRequest(
    @field:Size(max = 100, message = "표시 이름은 100자 이하여야 합니다.")
    val displayName: String? = null,
    
    @field:Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    val bio: String? = null,
    
    @field:Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
    val profileImageUrl: String? = null,
    
    @field:Size(max = 100, message = "위치는 100자 이하여야 합니다.")
    val location: String? = null,
    
    @field:Size(max = 255, message = "웹사이트 URL은 255자 이하여야 합니다.")
    val websiteUrl: String? = null,
    
    @field:Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    val phoneNumber: String? = null
)

/**
 * 비밀번호 변경 요청 DTO
 */
data class PasswordChangeRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val currentPassword: String,
    
    @field:NotBlank(message = "새 비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 100, message = "새 비밀번호는 8~100자 사이여야 합니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val newPassword: String,
    
    @field:NotBlank(message = "비밀번호 확인은 필수입니다.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val confirmPassword: String
)

/**
 * 역할 변경 요청 DTO
 */
data class RoleChangeRequest(
    @field:NotBlank(message = "역할은 필수입니다.")
    val role: UserRole
)

/**
 * 로그인 응답 DTO
 * JWT 토큰과 사용자 정보를 포함
 */
data class LoginResponse(
    val token: String,
    val refreshToken: String?,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)

/**
 * 사용자 응답 DTO
 * 클라이언트에게 전달되는 사용자 정보
 * 민감 정보(비밀번호 등)는 제외됨
 */
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val role: UserRole,
    val isActive: Boolean,
    val isEmailVerified: Boolean,
    val location: String?,
    val websiteUrl: String?,
    val phoneNumber: String?,
    val lastLoginAt: String?,
    val createdAt: String,
    val updatedAt: String
)

/**
 * 사용자 프로필 응답 DTO
 * 추가 통계 정보를 포함한 상세 프로필 정보
 */
data class UserProfileResponse(
    val user: UserResponse,
    val stats: UserStats
)

/**
 * 사용자 통계 정보 DTO
 */
data class UserStats(
    val postCount: Long,
    val commentCount: Long,
    val followerCount: Long,
    val followingCount: Long,
    val likesReceived: Long,
    val dislikesReceived: Long
)

/**
 * 사용자 통계 응답 DTO
 * 전체 사용자 통계 정보
 */
data class UserStatsResponse(
    val totalUsers: Long,
    val activeUsers: Long,
    val inactiveUsers: Long,
    val verifiedUsers: Long,
    val unverifiedUsers: Long,
    val newUsersThisMonth: Long,
    val usersByRole: Map<UserRole, Long>
)

/**
 * 사용자 검색 응답 DTO
 */
data class UserSearchResponse(
    val users: List<UserResponse>,
    val totalCount: Long,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * 사용자 역할 enum
 */
enum class UserRole {
    USER,       // 일반 사용자
    MODERATOR,  // 모더레이터
    ADMIN       // 관리자
}

/**
 * 사용자 상태 enum
 */
enum class UserStatus {
    ACTIVE,     // 활성
    INACTIVE,   // 비활성
    SUSPENDED,  // 정지됨
    BANNED      // 차단됨
}

/**
 * 사용자 정렬 기준 enum
 */
enum class UserSortBy {
    USERNAME,
    EMAIL,
    DISPLAY_NAME,
    CREATED_AT,
    UPDATED_AT,
    LAST_LOGIN_AT,
    POST_COUNT,
    COMMENT_COUNT
}