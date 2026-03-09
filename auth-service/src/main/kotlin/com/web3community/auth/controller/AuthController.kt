package com.web3community.auth.controller

import com.web3community.auth.dto.LoginRequest
import com.web3community.auth.dto.RegisterRequest
import com.web3community.auth.dto.TokenResponse
import com.web3community.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 컨트롤러
 *
 * 일반 로그인(이메일/비밀번호) 및 토큰 관리 API를 제공한다.
 * OAuth2 소셜 로그인은 [OAuth2Controller]에서 처리한다.
 *
 * 기본 경로: /auth
 *
 * API 목록:
 * - POST /auth/register   : 회원가입
 * - POST /auth/login      : 일반 로그인 (이메일 + 비밀번호)
 * - POST /auth/logout     : 로그아웃 (Refresh Token Redis에서 삭제)
 * - POST /auth/refresh    : Access Token 재발급 (Refresh Token Rotation)
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    /**
     * 회원가입
     *
     * 이메일 중복 검사 후 비밀번호를 BCrypt로 해싱하여 사용자를 생성한다.
     * 생성 완료 시 Kafka를 통해 [UserCreatedEvent]를 발행하고,
     * User Service가 이를 소비하여 MySQL에 사용자 레코드를 생성한다.
     *
     * @param request 회원가입 요청 DTO (이메일, 비밀번호, 닉네임)
     * @return 201 Created + [TokenResponse] (회원가입 즉시 로그인 처리)
     */
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ResponseEntity<TokenResponse> {
        // 회원가입 처리 후 즉시 토큰 발급 (UX 향상: 가입 후 별도 로그인 불필요)
        val tokenResponse = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse)
    }

    /**
     * 일반 로그인
     *
     * 이메일과 비밀번호를 검증하여 JWT Access Token + Refresh Token을 발급한다.
     * Refresh Token은 Redis에 저장된다 (TTL: 7일).
     *
     * 응답 형식:
     * ```json
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "expiresIn": 1800000
     * }
     * ```
     *
     * @param request 로그인 요청 DTO (이메일, 비밀번호)
     * @return 200 OK + [TokenResponse]
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.login(request)
        return ResponseEntity.ok(tokenResponse)
    }

    /**
     * 로그아웃
     *
     * Redis에 저장된 Refresh Token을 삭제하여 토큰을 무효화한다.
     * Access Token은 만료 시간(30분)이 지나면 자연스럽게 만료된다.
     * (Access Token 블랙리스트 처리는 현재 미구현, 필요 시 Redis에 추가)
     *
     * 요청 헤더:
     * - Authorization: Bearer {accessToken} (누가 로그아웃하는지 식별)
     *
     * @param refreshToken 무효화할 Refresh Token (요청 바디)
     * @return 204 No Content
     */
    @PostMapping("/logout")
    fun logout(
        @RequestBody body: Map<String, String>,
    ): ResponseEntity<Void> {
        val refreshToken = body["refreshToken"]
            ?: return ResponseEntity.badRequest().build()

        // Refresh Token을 Redis에서 삭제하여 이후 토큰 갱신 불가 처리
        authService.logout(refreshToken)
        return ResponseEntity.noContent().build()
    }

    /**
     * Access Token 재발급 (Refresh Token Rotation)
     *
     * 유효한 Refresh Token을 제출하면 새로운 Access Token과 Refresh Token을 발급한다.
     *
     * Refresh Token Rotation 전략:
     * - 매번 Refresh Token도 새로 발급 (보안 강화)
     * - 이전 Refresh Token은 Redis에서 삭제
     * - 새 Refresh Token을 Redis에 저장 (TTL 초기화)
     * - 탈취된 Refresh Token 재사용 시 감지 가능
     *
     * @param body refreshToken을 담은 요청 바디
     * @return 200 OK + 새로운 [TokenResponse]
     */
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: Map<String, String>,
    ): ResponseEntity<TokenResponse> {
        val refreshToken = body["refreshToken"]
            ?: return ResponseEntity.badRequest().build()

        // Refresh Token 검증 및 새 토큰 발급
        val tokenResponse = authService.refreshToken(refreshToken)
        return ResponseEntity.ok(tokenResponse)
    }
}
