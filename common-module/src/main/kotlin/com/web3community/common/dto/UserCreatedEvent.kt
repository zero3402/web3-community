package com.web3community.common.dto

/**
 * UserCreatedEvent - 사용자 생성 완료 Kafka 이벤트 DTO
 *
 * auth-service가 회원가입 처리 후 Kafka 토픽 `user.created`에 발행하는 이벤트입니다.
 * user-service가 이 이벤트를 소비하여 사용자 정보를 DB에 저장합니다.
 *
 * 이벤트 기반 설계의 이점:
 * - auth-service와 user-service 간의 강한 결합 제거
 * - user-service가 일시 다운되더라도 auth-service는 정상 동작 (내결함성)
 * - Kafka의 at-least-once 보장으로 메시지 유실 방지
 * - blockchain-service도 동일 이벤트를 소비하여 지갑을 자동 생성 가능
 *
 * Kafka 설정:
 * - 토픽: [com.web3community.common.kafka.KafkaTopics.USER_CREATED] (`user.created`)
 * - 파티션 키: userId (동일 사용자의 이벤트가 동일 파티션으로 전달되어 순서 보장)
 * - 직렬화: JSON (spring-kafka JsonSerializer)
 *
 * 보안 주의사항:
 * - [password] 필드는 BCrypt 해시 값입니다. 평문 비밀번호를 절대 포함하지 마세요.
 * - Kafka 토픽은 ACL로 보호하여 인가된 소비자만 접근할 수 있어야 합니다.
 *
 * @property userId auth-service가 생성한 UUID 형식의 사용자 고유 식별자
 * @property email 사용자 이메일 주소 (로그인 아이디)
 * @property password BCrypt 해싱된 비밀번호 (소셜 로그인의 경우 임의 값 사용 가능)
 * @property nickname 사용자 화면 표시 이름 (2~30자)
 * @property provider 인증 제공자 식별자 ("LOCAL", "GOOGLE", "NAVER", "KAKAO" 등)
 * @property providerId 소셜 로그인 제공자의 사용자 고유 ID (일반 회원가입 시 null)
 * @property profileImageUrl 소셜 로그인 제공자의 프로필 이미지 URL (없으면 null)
 */
data class UserCreatedEvent(
    /** auth-service가 생성한 UUID 문자열 형식의 사용자 ID */
    val userId: String,

    /** 사용자 이메일 주소 (로그인 ID, 고유 값) */
    val email: String,

    /** BCrypt 해싱된 비밀번호 (평문 비밀번호 절대 사용 금지) */
    val password: String,

    /** 사용자 닉네임 (화면 표시 이름, 2~30자) */
    val nickname: String,

    /**
     * 인증 제공자 식별자
     *
     * - "LOCAL": 이메일/비밀번호 일반 회원가입
     * - "GOOGLE": Google OAuth2 소셜 로그인
     * - "NAVER": Naver OAuth2 소셜 로그인
     * - "KAKAO": Kakao OAuth2 소셜 로그인
     */
    val provider: String,

    /** 소셜 로그인 제공자가 부여한 사용자 고유 ID (일반 회원가입 시 null) */
    val providerId: String? = null,

    /** 소셜 로그인 제공자의 프로필 이미지 URL (없거나 일반 회원가입 시 null) */
    val profileImageUrl: String? = null,
)
