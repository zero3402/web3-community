package com.web3community.user.dto

import com.web3community.user.domain.entity.User
import com.web3community.user.domain.entity.UserStatus
import java.time.LocalDateTime

/**
 * UserResponse - 사용자 공개 프로필 응답 DTO
 *
 * 인증된 사용자가 자신의 프로필을 조회하거나,
 * 다른 사용자의 공개 프로필을 조회할 때 반환하는 응답 DTO이다.
 *
 * 민감 정보 제외:
 * - [password]: BCrypt 해시라도 외부에 노출하지 않는다.
 * - [externalId]: auth-service 내부 UUID는 공개하지 않는다.
 *
 * @property id DB auto-increment PK (Long).
 * @property externalId auth-service UUID (내부 시스템 연동용).
 * @property email 이메일 주소.
 * @property nickname 화면 표시 이름.
 * @property provider 인증 제공자 ("LOCAL", "GOOGLE" 등).
 * @property profileImageUrl 프로필 이미지 URL (null 가능).
 * @property status 계정 상태.
 * @property createdAt 가입 일시.
 */
data class UserResponse(
    /** DB auto-increment PK */
    val id: Long,

    /** auth-service UUID (외부 시스템 연동 및 JWT sub 클레임 대응) */
    val externalId: String,

    /** 이메일 주소 */
    val email: String,

    /** 화면 표시 이름 */
    val nickname: String,

    /** 인증 제공자 식별자 */
    val provider: String,

    /** 프로필 이미지 URL (없으면 null) */
    val profileImageUrl: String?,

    /** 계정 상태 */
    val status: UserStatus,

    /** 가입 일시 */
    val createdAt: LocalDateTime?,
) {
    companion object {

        /**
         * [User] 엔티티로부터 [UserResponse] 변환
         *
         * 서비스 레이어에서 엔티티를 응답 DTO로 변환할 때 사용한다.
         *
         * @param user 변환할 User 엔티티
         * @return UserResponse DTO
         */
        fun from(user: User): UserResponse = UserResponse(
            id = user.id,
            externalId = user.externalId,
            email = user.email,
            nickname = user.nickname,
            provider = user.provider,
            profileImageUrl = user.profileImageUrl,
            status = user.status,
            createdAt = user.createdAt,
        )
    }
}
