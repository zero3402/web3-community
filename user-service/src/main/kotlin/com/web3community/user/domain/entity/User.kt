package com.web3community.user.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * User - 사용자 JPA 엔티티
 *
 * auth-service가 Kafka로 발행하는 [com.web3community.common.dto.UserCreatedEvent]를
 * 소비하여 생성되는 사용자 레코드이다.
 *
 * 설계 결정:
 * - `externalId`: auth-service가 생성한 UUID 문자열. JWT의 sub 클레임과 동일.
 *   API Gateway가 X-User-Id 헤더에 DB auto-increment id(Long)를 전파하므로
 *   두 가지 ID 체계를 모두 보유한다.
 * - `password`: OAuth2 소셜 로그인 사용자는 null. 일반 가입자는 BCrypt 해시 값.
 * - soft delete 없음: 탈퇴 시 status를 INACTIVE로 변경.
 *
 * DB 테이블: `users`
 *
 * @property id DB auto-increment PK (Long). API Gateway가 X-User-Id 헤더에 전파.
 * @property externalId auth-service가 생성한 UUID 문자열 (JWT sub 클레임).
 * @property email 사용자 이메일 주소 (고유, 로그인 ID).
 * @property password BCrypt 해시 비밀번호. OAuth2 사용자는 null.
 * @property nickname 사용자 화면 표시 이름 (2~30자).
 * @property provider 인증 제공자 식별자 ("LOCAL", "GOOGLE", "NAVER", "KAKAO").
 * @property providerId 소셜 로그인 제공자의 사용자 고유 ID. 일반 가입자는 null.
 * @property profileImageUrl 프로필 이미지 URL. 없으면 null.
 * @property status 계정 상태 ([UserStatus.ACTIVE], [UserStatus.INACTIVE], [UserStatus.BANNED]).
 * @property createdAt 계정 생성 일시 (JPA Auditing 자동 기록).
 * @property updatedAt 마지막 수정 일시 (JPA Auditing 자동 기록).
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User(

    /**
     * DB auto-increment PK.
     * API Gateway는 JWT 검증 후 이 값을 X-User-Id 헤더에 담아 downstream에 전파한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * auth-service가 생성한 UUID 문자열.
     * JWT의 sub 클레임과 동일하며, Kafka 이벤트의 userId 필드로 전달된다.
     * 멱등성 처리에 사용: 동일한 externalId가 이미 존재하면 중복 생성하지 않는다.
     */
    @Column(name = "external_id", unique = true, nullable = false, length = 36)
    val externalId: String,

    /**
     * 사용자 이메일 주소.
     * 로그인 ID로 사용되며 고유 값이어야 한다.
     */
    @Column(unique = true, nullable = false, length = 255)
    val email: String,

    /**
     * BCrypt 해시 비밀번호.
     * OAuth2 소셜 로그인 사용자는 비밀번호가 없으므로 null 허용.
     * 평문 비밀번호는 절대 저장하지 않는다.
     */
    @Column(nullable = true, length = 255)
    var password: String?,

    /**
     * 사용자 닉네임 (화면 표시 이름).
     * 2자 이상 30자 이하. 변경 가능.
     */
    @Column(nullable = false, length = 30)
    var nickname: String,

    /**
     * 인증 제공자 식별자.
     * - "LOCAL": 이메일/비밀번호 일반 회원가입
     * - "GOOGLE": Google OAuth2 소셜 로그인
     * - "NAVER": Naver OAuth2 소셜 로그인
     * - "KAKAO": Kakao OAuth2 소셜 로그인
     */
    @Column(nullable = false, length = 20)
    val provider: String,

    /**
     * 소셜 로그인 제공자가 부여한 사용자 고유 ID.
     * 일반 회원가입 시 null.
     * OAuth2 재로그인 시 기존 회원 식별에 사용된다.
     */
    @Column(name = "provider_id", nullable = true, length = 255)
    val providerId: String? = null,

    /**
     * 프로필 이미지 URL.
     * 소셜 로그인 시 제공자의 프로필 이미지로 초기화.
     * 사용자가 직접 변경 가능.
     */
    @Column(name = "profile_image_url", nullable = true, length = 512)
    var profileImageUrl: String? = null,

    /**
     * 계정 상태.
     * - ACTIVE: 정상 활성 계정
     * - INACTIVE: 탈퇴 계정 (soft delete)
     * - BANNED: 운영자에 의해 정지된 계정
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: UserStatus = UserStatus.ACTIVE,

    /**
     * 계정 생성 일시.
     * JPA Auditing이 자동으로 기록. 변경 불가.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = true, updatable = false)
    val createdAt: LocalDateTime? = null,

    /**
     * 마지막 수정 일시.
     * JPA Auditing이 자동으로 기록. 프로필 수정, 상태 변경 시 자동 갱신.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime? = null,
)

/**
 * UserStatus - 사용자 계정 상태 열거형
 *
 * 하드 삭제(row 삭제) 대신 상태 변경으로 계정을 관리한다.
 * 이를 통해 탈퇴 후 재가입 시 이메일 중복 방지 및 이력 추적이 가능하다.
 *
 * @property ACTIVE 정상 활성 계정. 모든 기능 사용 가능.
 * @property INACTIVE 탈퇴 계정. 로그인 불가, 데이터는 보존.
 * @property BANNED 운영자에 의해 정지된 계정. 로그인 불가.
 */
enum class UserStatus {
    /** 정상 활성 계정 */
    ACTIVE,

    /** 탈퇴 계정 (soft delete) */
    INACTIVE,

    /** 운영자 정지 계정 */
    BANNED,
}
