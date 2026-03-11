package com.web3community.user.dto

import com.web3community.user.domain.entity.User

/**
 * InternalUserResponse - 내부 서비스 간 통신용 응답 DTO
 *
 * auth-service의 Feign 클라이언트([GET /users/internal/by-email/{email}])가
 * 호출하는 내부 API의 응답 DTO이다.
 *
 * 주의사항:
 * - 이 DTO는 외부 공개 API에서 절대 반환하면 안 된다.
 * - [password] 필드에 BCrypt 해시 값이 포함되므로 내부 통신 전용으로만 사용한다.
 * - API Gateway가 `/users/internal/**` 경로를 외부에 노출하지 않도록 라우팅 설정 필요.
 *
 * auth-service Feign 클라이언트 인터페이스 기대 구조:
 * ```kotlin
 * data class InternalUserResponse(
 *     val id: String,        // externalId (UUID 문자열, JWT sub와 동일)
 *     val email: String,
 *     val password: String?, // BCrypt 해시 (OAuth2 사용자는 null)
 *     val nickname: String,
 * )
 * ```
 *
 * `id` 필드가 String인 이유:
 * - auth-service는 JWT의 sub 클레임(UUID 문자열)을 userId로 사용한다.
 * - auth-service Feign 호출 후 `user.id`로 토큰을 발급하므로,
 *   이 서비스의 DB PK(Long)가 아닌 externalId(UUID String)를 반환해야 한다.
 *
 * @property id externalId (UUID 문자열). auth-service JWT sub 클레임과 동일.
 * @property email 이메일 주소.
 * @property password BCrypt 해시 비밀번호. OAuth2 사용자는 null.
 * @property nickname 화면 표시 이름.
 */
data class InternalUserResponse(

    /**
     * externalId (UUID 문자열).
     *
     * auth-service가 JWT 토큰 발급 시 sub 클레임으로 사용하는 값.
     * 필드명을 `id`로 유지하여 auth-service Feign 클라이언트와 호환성을 보장한다.
     */
    val id: String,

    /** 이메일 주소 */
    val email: String,

    /**
     * BCrypt 해시 비밀번호.
     *
     * auth-service 로그인 시 평문 비밀번호와 비교 검증에 사용.
     * OAuth2 소셜 로그인 사용자는 null.
     */
    val password: String?,

    /** 화면 표시 이름 */
    val nickname: String,
) {
    companion object {

        /**
         * [User] 엔티티로부터 [InternalUserResponse] 변환
         *
         * [id] 필드는 DB PK가 아닌 [User.externalId]로 매핑한다.
         * auth-service가 JWT sub 클레임(UUID)을 기준으로 토큰을 관리하기 때문이다.
         *
         * @param user 변환할 User 엔티티
         * @return InternalUserResponse DTO
         */
        fun from(user: User): InternalUserResponse = InternalUserResponse(
            id = user.externalId,   // 중요: DB PK(Long)가 아닌 UUID(String)
            email = user.email,
            password = user.password,
            nickname = user.nickname,
        )
    }
}
