package com.web3community.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * RegisterRequest - 일반 회원가입 요청 DTO
 *
 * 이메일/비밀번호 방식으로 신규 회원가입을 요청할 때 HTTP Request Body로 전달되는 데이터 클래스입니다.
 * `POST /auth/register` 엔드포인트에서 `@Valid`와 함께 사용됩니다.
 *
 * 처리 흐름:
 * 1. Bean Validation으로 이 DTO의 형식 유효성 검사
 * 2. [com.web3community.auth.service.AuthService.register]에서 비즈니스 규칙 검증
 *    (이메일 중복 여부를 User Service에 동기 Feign 호출로 확인)
 * 3. Kafka에 [com.web3community.common.dto.UserCreatedEvent] 발행
 *
 * 비밀번호 정책:
 * - 최소 8자, 최대 100자
 * - 추가 복잡도 규칙(대문자, 숫자, 특수문자 포함)은 프론트엔드 또는 별도 커스텀 검증기에서 처리합니다.
 *
 * 요청 예시:
 * ```json
 * {
 *   "email": "user@example.com",
 *   "password": "securePass123!",
 *   "nickname": "web3fan"
 * }
 * ```
 *
 * @property email 회원가입에 사용할 이메일 주소 (RFC 5322 형식, 고유 값)
 * @property password 비밀번호 (8~100자, BCrypt 해싱 후 Kafka 이벤트로 전달)
 * @property nickname 화면 표시 이름 (2~30자)
 */
data class RegisterRequest(
    /**
     * 이메일 주소
     *
     * - `@Email`: RFC 5322 이메일 형식 검증
     * - `@NotBlank`: null, 빈 문자열, 공백만 있는 문자열 거부
     */
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:NotBlank(message = "이메일을 입력해 주세요.")
    val email: String,

    /**
     * 비밀번호 (평문)
     *
     * - `@NotBlank`: null, 빈 문자열, 공백 거부
     * - `@Size(min=8, max=100)`: 최소 8자, 최대 100자
     *   (최대값 제한: BCrypt는 입력 길이가 길수록 연산 비용 증가 → DoS 방지)
     * - 저장 전 반드시 BCrypt로 해싱해야 합니다.
     */
    @field:NotBlank(message = "비밀번호를 입력해 주세요.")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    val password: String,

    /**
     * 닉네임 (화면 표시 이름)
     *
     * - `@NotBlank`: null, 빈 문자열, 공백 거부
     * - `@Size(min=2, max=30)`: 최소 2자, 최대 30자
     */
    @field:NotBlank(message = "닉네임을 입력해 주세요.")
    @field:Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
    val nickname: String,
)
