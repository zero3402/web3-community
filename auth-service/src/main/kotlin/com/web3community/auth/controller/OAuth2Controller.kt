package com.web3community.auth.controller

import com.web3community.auth.dto.TokenResponse
import com.web3community.auth.oauth2.GoogleOAuth2UserService
import com.web3community.auth.oauth2.KakaoOAuth2UserService
import com.web3community.auth.oauth2.NaverOAuth2UserService
import com.web3community.auth.service.AuthService
import com.web3community.auth.service.TokenService
import com.web3community.common.security.JwtUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * OAuth2 소셜 로그인 콜백 컨트롤러
 *
 * 각 OAuth2 제공자(Google/Naver/Kakao)의 인가 코드(Authorization Code)를
 * 수신하여 JWT 토큰을 발급하는 흐름을 처리한다.
 *
 * OAuth2 Authorization Code Flow:
 * 1. 프론트엔드 → OAuth2 제공자: 로그인 요청 (client_id, redirect_uri 포함)
 * 2. 사용자 → OAuth2 제공자: 로그인 및 권한 동의
 * 3. OAuth2 제공자 → Auth Service: 인가 코드(code) 전달 (redirect_uri 호출)
 * 4. Auth Service → OAuth2 제공자: 인가 코드로 Access Token 교환
 * 5. Auth Service → OAuth2 제공자: Access Token으로 사용자 정보 조회
 * 6. Auth Service: JWT 발급 후 프론트엔드에 반환
 *
 * 기본 경로: /auth/oauth2
 *
 * 엔드포인트:
 * - GET /auth/oauth2/google/callback : Google 콜백
 * - GET /auth/oauth2/naver/callback  : Naver 콜백
 * - GET /auth/oauth2/kakao/callback  : Kakao 콜백
 *
 * 참고:
 * Spring Security OAuth2 Client의 자동 처리(/login/oauth2/code/**)를 사용하지 않고
 * 수동으로 처리하는 이유:
 * - JWT 발급 로직을 컨트롤러 레벨에서 명시적으로 제어하기 위해
 * - 소셜 로그인 후 응답 형식을 REST API 형식으로 통일하기 위해
 */
@RestController
@RequestMapping("/auth/oauth2")
class OAuth2Controller(
    private val googleOAuth2UserService: GoogleOAuth2UserService,   // Google 사용자 정보 파싱
    private val naverOAuth2UserService: NaverOAuth2UserService,     // Naver 사용자 정보 파싱
    private val kakaoOAuth2UserService: KakaoOAuth2UserService,     // Kakao 사용자 정보 파싱
    private val authService: AuthService,                           // JWT 발급 서비스
    private val tokenService: TokenService,                         // Refresh Token Redis 관리
    private val jwtUtils: JwtUtils,                                 // JWT 생성 유틸
) {

    /**
     * Google OAuth2 콜백 처리
     *
     * Google 인가 서버로부터 인가 코드를 수신하여 사용자를 인증한다.
     *
     * 처리 흐름:
     * 1. 인가 코드(code)로 Google Access Token 교환
     * 2. Google Access Token으로 사용자 정보 조회 (email, name, picture)
     * 3. 기존 회원 조회 또는 신규 회원 생성
     * 4. JWT Access Token + Refresh Token 발급
     *
     * Google 사용자 정보 응답 예시:
     * ```json
     * {
     *   "sub": "12345678",        // Google 고유 사용자 ID
     *   "email": "user@gmail.com",
     *   "name": "홍길동",
     *   "picture": "https://..."
     * }
     * ```
     *
     * @param code Google 인가 코드
     * @param state CSRF 방지용 상태값 (옵션)
     * @return 200 OK + [TokenResponse]
     */
    @GetMapping("/google/callback")
    fun googleCallback(
        @RequestParam code: String,
        @RequestParam(required = false) state: String?,
    ): ResponseEntity<TokenResponse> {
        // Google 인가 코드 → 사용자 정보 파싱 → JWT 발급
        val tokenResponse = googleOAuth2UserService.processOAuth2Callback(code)
        return ResponseEntity.ok(tokenResponse)
    }

    /**
     * Naver OAuth2 콜백 처리
     *
     * Naver 인가 서버로부터 인가 코드를 수신하여 사용자를 인증한다.
     *
     * Naver 사용자 정보 응답 구조 (래퍼 있음):
     * ```json
     * {
     *   "resultcode": "00",
     *   "message": "success",
     *   "response": {              // ← 실제 사용자 정보는 "response" 키 내부
     *     "id": "12345678",
     *     "email": "user@naver.com",
     *     "name": "홍길동",
     *     "profile_image": "https://..."
     *   }
     * }
     * ```
     *
     * @param code Naver 인가 코드
     * @param state CSRF 방지용 상태값 (Naver는 필수)
     * @return 200 OK + [TokenResponse]
     */
    @GetMapping("/naver/callback")
    fun naverCallback(
        @RequestParam code: String,
        @RequestParam(required = false) state: String?,
    ): ResponseEntity<TokenResponse> {
        // Naver 인가 코드 → 사용자 정보 파싱 (response 래퍼 처리) → JWT 발급
        val tokenResponse = naverOAuth2UserService.processOAuth2Callback(code, state)
        return ResponseEntity.ok(tokenResponse)
    }

    /**
     * Kakao OAuth2 콜백 처리
     *
     * Kakao 인가 서버로부터 인가 코드를 수신하여 사용자를 인증한다.
     *
     * Kakao 사용자 정보 응답 구조 (중첩 구조):
     * ```json
     * {
     *   "id": 12345678,
     *   "kakao_account": {
     *     "email": "user@kakao.com",
     *     "profile": {
     *       "nickname": "홍길동",           // ← kakao_account.profile.nickname
     *       "profile_image_url": "https://..."
     *     }
     *   }
     * }
     * ```
     *
     * @param code Kakao 인가 코드
     * @return 200 OK + [TokenResponse]
     */
    @GetMapping("/kakao/callback")
    fun kakaoCallback(
        @RequestParam code: String,
    ): ResponseEntity<TokenResponse> {
        // Kakao 인가 코드 → 사용자 정보 파싱 (중첩 구조 처리) → JWT 발급
        val tokenResponse = kakaoOAuth2UserService.processOAuth2Callback(code)
        return ResponseEntity.ok(tokenResponse)
    }
}
