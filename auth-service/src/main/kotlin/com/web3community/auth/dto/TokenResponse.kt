package com.web3community.auth.dto

/**
 * TokenResponse - JWT 토큰 발급 응답 DTO
 *
 * 로그인, 회원가입, 토큰 갱신 성공 시 클라이언트에게 반환되는 응답 데이터 클래스입니다.
 * HTTP Response Body의 `data` 필드에 포함됩니다.
 *
 * 클라이언트 사용 가이드:
 * - `accessToken`: API 요청 시 `Authorization: Bearer {accessToken}` 헤더로 전달
 * - `refreshToken`: 안전한 저장소(HttpOnly 쿠키 권장, 또는 메모리)에 저장
 *   LocalStorage/SessionStorage에 저장하면 XSS 공격에 취약합니다.
 * - `expiresIn`: Access Token 만료까지 남은 시간(ms). 만료 전 미리 갱신 요청 권장
 *
 * 토큰 갱신 전략 (Refresh Token Rotation):
 * - Access Token 만료 시 `POST /auth/refresh` 에 Refresh Token을 전달
 * - 서버는 새 Access Token + 새 Refresh Token을 발급 (이전 Refresh Token 무효화)
 * - Refresh Token이 재사용되면 탈취로 간주하여 모든 세션을 강제 로그아웃
 *
 * 응답 예시:
 * ```json
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "expiresIn": 1800000
 * }
 * ```
 *
 * @property accessToken HMAC-SHA256 서명된 JWT Access Token (만료: [expiresIn]ms 후)
 * @property refreshToken HMAC-SHA256 서명된 JWT Refresh Token (만료: 7일, Redis에 저장됨)
 * @property expiresIn Access Token 만료 시간 (밀리초, 일반적으로 1800000 = 30분)
 */
data class TokenResponse(
    /** JWT Access Token 문자열 (API 요청 시 Authorization 헤더에 포함) */
    val accessToken: String,

    /** JWT Refresh Token 문자열 (Access Token 재발급 시 사용, 안전한 저장소에 보관) */
    val refreshToken: String,

    /**
     * Access Token 만료 시간 (밀리초)
     *
     * 클라이언트는 이 값을 이용해 토큰 만료 전 갱신 요청 타이밍을 결정할 수 있습니다.
     * 예: 만료 1분 전에 자동으로 `/auth/refresh` 호출
     */
    val expiresIn: Long,
)
