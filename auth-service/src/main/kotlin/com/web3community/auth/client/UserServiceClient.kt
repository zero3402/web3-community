package com.web3community.auth.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/**
 * UserServiceClient - User Service Feign 클라이언트
 *
 * User Service의 내부 API를 선언적으로 호출하는 OpenFeign 인터페이스입니다.
 * auth-service에서 회원가입/로그인 시 사용자 정보 조회에 사용됩니다.
 *
 * Feign 설정 (application.yml):
 * ```yaml
 * feign:
 *   client:
 *     config:
 *       user-service:
 *         connect-timeout: 3000   # 연결 타임아웃: 3초
 *         read-timeout: 5000      # 읽기 타임아웃: 5초
 *
 * user-service:
 *   url: ${USER_SERVICE_URL:http://localhost:8082}
 * ```
 *
 * 활성화:
 * [com.web3community.auth.AuthApplication]의 `@EnableFeignClients` 어노테이션으로 활성화됩니다.
 *
 * 내결함성 설계:
 * - Feign 호출 실패 시 [AuthService][com.web3community.auth.service.AuthService]에서
 *   `runCatching`으로 예외를 처리합니다.
 * - 사용자 없음(404)은 null 반환으로 처리됩니다 (404 → null 디코더 설정 필요 또는 예외 처리).
 *
 * 보안:
 * - `/users/internal/**` 경로는 서비스 간 내부 통신 전용입니다.
 * - API Gateway를 통해 외부에서 접근할 수 없도록 라우팅 규칙에서 차단해야 합니다.
 */
@FeignClient(
    name = "user-service",
    url = "\${user-service.url:http://localhost:8082}"
)
interface UserServiceClient {

    /**
     * 이메일로 사용자 조회
     *
     * User Service의 내부 전용 엔드포인트를 호출하여 이메일에 해당하는 사용자를 조회합니다.
     *
     * 사용 시나리오:
     * 1. 회원가입 시 이메일 중복 확인 (결과가 non-null이면 이미 사용 중인 이메일)
     * 2. 로그인 시 사용자 존재 여부 및 비밀번호 해시 조회
     *
     * @param email 조회할 사용자의 이메일 주소
     * @return 이메일에 해당하는 사용자 정보, 없으면 null
     */
    @GetMapping("/users/internal/by-email/{email}")
    fun findByEmail(@PathVariable email: String): UserResponse?
}

/**
 * UserResponse - User Service에서 반환하는 사용자 정보 DTO
 *
 * User Service의 내부 API 응답을 역직렬화하는 데이터 클래스입니다.
 * auth-service에서만 사용하는 내부 DTO이므로 com.web3community.auth.client 패키지에 위치합니다.
 *
 * 필드 설계:
 * - [id]: auth-service가 생성한 UUID 문자열 (user-service DB에도 동일 값으로 저장)
 * - [password]: BCrypt 해시 값, 소셜 로그인 계정은 null일 수 있음
 *
 * @property id 사용자 고유 식별자 (UUID 문자열)
 * @property email 사용자 이메일 주소 (로그인 ID)
 * @property password BCrypt 해시 비밀번호 (소셜 로그인 계정의 경우 null)
 * @property nickname 사용자 닉네임 (화면 표시 이름)
 */
data class UserResponse(
    /** auth-service가 생성하여 Kafka로 전달한 UUID 문자열 사용자 ID */
    val id: String,

    /** 사용자 이메일 주소 (고유 식별자 역할) */
    val email: String,

    /**
     * BCrypt 해시 비밀번호
     *
     * 소셜 로그인(Google/Naver/Kakao)으로만 가입한 경우 null입니다.
     * 로그인 시 [org.springframework.security.crypto.password.PasswordEncoder.matches]로 검증합니다.
     */
    val password: String?,

    /** 사용자 닉네임 (화면 표시 이름, 2~30자) */
    val nickname: String,
)
