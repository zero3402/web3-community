package com.web3community.auth.oauth2

import com.web3community.auth.dto.TokenResponse
import com.web3community.auth.service.OAuth2Service
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

/**
 * Google OAuth2 사용자 정보 처리 서비스
 *
 * Google OAuth2 Authorization Code Flow를 수동으로 처리한다.
 * OAuth2Controller에서 인가 코드(code)를 수신하면 이 서비스가 나머지 흐름을 처리한다.
 *
 * 처리 흐름:
 * 1. 인가 코드(Authorization Code) → Google Access Token 교환
 *    - 엔드포인트: POST https://oauth2.googleapis.com/token
 * 2. Google Access Token → 사용자 정보 조회
 *    - 엔드포인트: GET https://www.googleapis.com/oauth2/v3/userinfo
 * 3. 사용자 정보 파싱 (email, name, sub, picture)
 * 4. 신규/기존 회원 처리 및 JWT 발급
 *
 * Google 사용자 정보 응답 구조:
 * ```json
 * {
 *   "sub": "12345678",          // Google 고유 사용자 ID
 *   "email": "user@gmail.com",
 *   "email_verified": true,
 *   "name": "홍길동",
 *   "picture": "https://lh3.googleusercontent.com/...",
 *   "locale": "ko"
 * }
 * ```
 */
@Service
class GoogleOAuth2UserService(
    private val oauth2Service: OAuth2Service,       // 공통 OAuth2 사용자 처리 로직
    private val objectMapper: ObjectMapper,         // JSON 파싱
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val clientId: String,
    @Value("\${spring.security.oauth2.client.registration.google.client-secret}")
    private val clientSecret: String,
    @Value("\${spring.security.oauth2.client.registration.google.redirect-uri}")
    private val redirectUri: String,
) {

    private val log = LoggerFactory.getLogger(GoogleOAuth2UserService::class.java)

    // RestTemplate: 동기 HTTP 클라이언트 (Spring MVC 환경에 적합)
    private val restTemplate = RestTemplate()

    // Google OAuth2 엔드포인트
    companion object {
        private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo"
    }

    /**
     * Google OAuth2 콜백 처리 (인가 코드 → JWT 발급)
     *
     * @param code Google 인가 서버로부터 받은 인가 코드
     * @return [TokenResponse] JWT Access Token + Refresh Token
     */
    fun processOAuth2Callback(code: String): TokenResponse {
        log.info("Google OAuth2 인가 코드 처리 시작")

        // 1단계: 인가 코드로 Google Access Token 교환
        val accessToken = exchangeCodeForToken(code)
        log.debug("Google Access Token 획득 성공")

        // 2단계: Google Access Token으로 사용자 정보 조회
        val userInfo = fetchUserInfo(accessToken)
        log.info("Google 사용자 정보 조회: email={}", userInfo["email"])

        // 3단계: 사용자 정보 파싱
        val email = userInfo["email"] as? String
            ?: throw IllegalStateException("Google 사용자 정보에 이메일이 없습니다.")
        val name = userInfo["name"] as? String ?: "Google 사용자"
        val sub = userInfo["sub"] as? String                     // Google 고유 사용자 ID
            ?: throw IllegalStateException("Google 사용자 정보에 sub가 없습니다.")
        val picture = userInfo["picture"] as? String             // 프로필 이미지 URL

        // 4단계: 기존 회원 조회 또는 신규 회원 생성 후 JWT 발급
        return oauth2Service.processOAuth2User(
            email = email,
            name = name,
            provider = "GOOGLE",
            providerId = sub,
            profileImageUrl = picture,
        )
    }

    /**
     * 인가 코드를 Google Access Token으로 교환
     *
     * Google Token 엔드포인트에 POST 요청을 보내 Access Token을 받는다.
     *
     * 요청 파라미터:
     * - code: 인가 코드
     * - client_id: Google OAuth2 클라이언트 ID
     * - client_secret: Google OAuth2 클라이언트 시크릿
     * - redirect_uri: 등록된 리다이렉트 URI
     * - grant_type: "authorization_code" (고정값)
     *
     * @param code 인가 코드
     * @return Google Access Token 문자열
     */
    private fun exchangeCodeForToken(code: String): String {
        // 요청 파라미터 구성
        val params = LinkedMultiValueMap<String, String>().apply {
            add("code", code)
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("redirect_uri", redirectUri)
            add("grant_type", "authorization_code")
        }

        // HTTP 헤더 설정
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val request = HttpEntity(params, headers)

        // Google Token 엔드포인트 호출
        val response = restTemplate.exchange(
            GOOGLE_TOKEN_URL,
            HttpMethod.POST,
            request,
            String::class.java,
        )

        // 응답 JSON 파싱하여 access_token 추출
        @Suppress("UNCHECKED_CAST")
        val responseBody = objectMapper.readValue(response.body, Map::class.java) as Map<String, Any>

        return responseBody["access_token"] as? String
            ?: throw IllegalStateException("Google로부터 Access Token을 받지 못했습니다.")
    }

    /**
     * Google Access Token으로 사용자 정보 조회
     *
     * Google UserInfo 엔드포인트에 GET 요청을 보내 사용자 정보를 받는다.
     *
     * @param accessToken Google Access Token
     * @return 사용자 정보 맵 (sub, email, name, picture 등)
     */
    @Suppress("UNCHECKED_CAST")
    private fun fetchUserInfo(accessToken: String): Map<String, Any> {
        // Authorization 헤더에 Bearer 토큰 설정
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)  // "Authorization: Bearer {accessToken}"
        }

        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            GOOGLE_USER_INFO_URL,
            HttpMethod.GET,
            request,
            String::class.java,
        )

        // 응답 JSON 파싱
        return objectMapper.readValue(response.body, Map::class.java) as Map<String, Any>
    }
}
