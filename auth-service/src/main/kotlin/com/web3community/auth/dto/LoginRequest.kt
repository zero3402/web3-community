package com.web3community.auth.dto

import jakarta.validation.constraints.NotBlank

/**
 * LoginRequest - 일반 로그인 요청 DTO
 *
 * 이메일과 비밀번호로 로그인을 요청할 때 HTTP Request Body로 전달되는 데이터 클래스입니다.
 * `POST /auth/login` 엔드포인트에서 `@Valid`와 함께 사용됩니다.
 *
 * 보안 설계:
 * - 로그인 실패 시 "이메일 없음"과 "비밀번호 틀림"을 구분하지 않습니다.
 *   동일한 오류 메시지를 반환하여 사용자 열거 공격(User Enumeration Attack)을 방지합니다.
 * - 비밀번호는 평문으로 전송되므로 반드시 TLS/HTTPS 환경에서만 사용해야 합니다.
 * - 비밀번호 길이 제한은 DoS 공격(긴 비밀번호로 BCrypt 연산 과부하) 방지를 위해 서비스 레이어에서 처리합니다.
 *
 * 요청 예시:
 * ```json
 * {
 *   "email": "user@example.com",
 *   "password": "myPassword123!"
 * }
 * ```
 *
 * @property email 로그인에 사용할 이메일 주소 (공백 불가)
 * @property password 사용자 비밀번호 (공백 불가, BCrypt 검증)
 */
data class LoginRequest(
    /** 로그인 이메일 주소 (공백 또는 빈 문자열 불가) */
    @field:NotBlank(message = "이메일을 입력해 주세요.")
    val email: String,

    /** 비밀번호 (공백 또는 빈 문자열 불가) */
    @field:NotBlank(message = "비밀번호를 입력해 주세요.")
    val password: String,
)
