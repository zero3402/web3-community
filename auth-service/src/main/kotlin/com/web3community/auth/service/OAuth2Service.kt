package com.web3community.auth.service

import com.web3community.auth.client.UserServiceClient
import com.web3community.auth.dto.TokenResponse
import com.web3community.auth.kafka.AuthEventProducer
import com.web3community.common.dto.UserCreatedEvent
import com.web3community.common.security.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

/**
 * OAuth2 소셜 로그인 통합 서비스
 *
 * Spring Security OAuth2 Client의 [DefaultOAuth2UserService]를 확장하여
 * Google/Naver/Kakao 인증 후 사용자 정보를 처리한다.
 *
 * 역할:
 * 1. OAuth2 제공자별 사용자 정보 파싱 (각 제공자마다 응답 구조가 다름)
 * 2. 기존 회원 조회 또는 신규 회원 자동 생성 (소셜 계정 연동)
 * 3. JWT 토큰 발급
 *
 * Spring Security OAuth2 연동:
 * - [SecurityConfig]에서 `.oauth2Login { .userInfoEndpoint { .userService(this) } }`로 등록
 * - OAuth2 Access Token 교환은 Spring Security가 자동 처리
 * - 이 서비스는 사용자 정보 엔드포인트 응답 처리만 담당
 *
 * 제공자별 사용자 정보 구조:
 * - Google: { "sub", "email", "name", "picture" }
 * - Naver: { "resultcode", "message", "response": { "id", "email", "name", "profile_image" } }
 * - Kakao: { "id", "kakao_account": { "email", "profile": { "nickname", "profile_image_url" } } }
 */
@Service
class OAuth2Service(
    private val userServiceClient: UserServiceClient,   // User Service Feign 클라이언트
    private val authEventProducer: AuthEventProducer,   // Kafka 이벤트 발행
    private val authService: AuthService,               // JWT 토큰 발급
) : DefaultOAuth2UserService() {

    private val log = LoggerFactory.getLogger(OAuth2Service::class.java)

    /**
     * OAuth2 사용자 정보 로드 및 처리
     *
     * Spring Security가 OAuth2 Access Token으로 사용자 정보를 조회한 후 이 메서드를 호출한다.
     *
     * 처리 흐름:
     * 1. 부모 클래스(DefaultOAuth2UserService)가 사용자 정보 엔드포인트 호출
     * 2. registrationId로 제공자(google/naver/kakao) 식별
     * 3. 제공자별 사용자 정보 파싱
     * 4. 기존 회원이면 로그인, 신규면 자동 회원가입
     *
     * @param userRequest OAuth2 사용자 요청 (클라이언트 등록 정보 + Access Token 포함)
     * @return [OAuth2User] Spring Security가 인증 컨텍스트에 저장하는 사용자 객체
     */
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        // 부모 클래스가 사용자 정보 엔드포인트를 호출하여 OAuth2User 반환
        val oAuth2User = super.loadUser(userRequest)

        // OAuth2 제공자 식별 (application.yml의 registration 키와 일치)
        val registrationId = userRequest.clientRegistration.registrationId
        log.info("OAuth2 로그인 시도: provider={}", registrationId)

        // 제공자별 사용자 정보 파싱
        val (email, name, providerId, profileImageUrl) = parseOAuth2UserInfo(
            registrationId, oAuth2User.attributes
        )

        // 기존 회원 조회 또는 신규 회원 생성
        processOAuth2User(email, name, registrationId.uppercase(), providerId, profileImageUrl)

        return oAuth2User
    }

    /**
     * OAuth2 제공자별 사용자 정보 파싱
     *
     * 각 제공자마다 응답 구조가 다르므로 제공자 ID로 분기 처리한다.
     *
     * @param registrationId 제공자 ID (google/naver/kakao)
     * @param attributes OAuth2 사용자 속성 맵
     * @return [OAuth2UserInfo] 파싱된 사용자 정보
     */
    private fun parseOAuth2UserInfo(
        registrationId: String,
        attributes: Map<String, Any>,
    ): OAuth2UserInfo {
        return when (registrationId.lowercase()) {
            "google" -> parseGoogleUser(attributes)
            "naver" -> parseNaverUser(attributes)
            "kakao" -> parseKakaoUser(attributes)
            else -> throw IllegalArgumentException("지원하지 않는 OAuth2 제공자: $registrationId")
        }
    }

    /**
     * Google 사용자 정보 파싱
     *
     * Google 응답 구조 (평탄한 구조):
     * ```json
     * {
     *   "sub": "12345678",          ← 제공자 고유 ID
     *   "email": "user@gmail.com",
     *   "name": "홍길동",
     *   "picture": "https://..."
     * }
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseGoogleUser(attributes: Map<String, Any>): OAuth2UserInfo {
        return OAuth2UserInfo(
            email = attributes["email"] as String,
            name = attributes["name"] as? String ?: "Google 사용자",
            providerId = attributes["sub"] as String,  // Google 고유 ID
            profileImageUrl = attributes["picture"] as? String,
        )
    }

    /**
     * Naver 사용자 정보 파싱
     *
     * Naver 응답 구조 ("response" 래퍼 존재):
     * ```json
     * {
     *   "resultcode": "00",
     *   "message": "success",
     *   "response": {              ← 실제 사용자 정보는 이 안에 있음
     *     "id": "12345678",
     *     "email": "user@naver.com",
     *     "name": "홍길동",
     *     "profile_image": "https://..."
     *   }
     * }
     * ```
     *
     * application.yml에서 `user-name-attribute: response`로 설정했으므로
     * attributes의 최상위 키가 "response"이다.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseNaverUser(attributes: Map<String, Any>): OAuth2UserInfo {
        // "response" 키 아래에 실제 사용자 정보가 있음
        val response = attributes["response"] as Map<String, Any>

        return OAuth2UserInfo(
            email = response["email"] as String,
            name = response["name"] as? String ?: "Naver 사용자",
            providerId = response["id"] as String,  // Naver 고유 ID
            profileImageUrl = response["profile_image"] as? String,
        )
    }

    /**
     * Kakao 사용자 정보 파싱
     *
     * Kakao 응답 구조 (중첩 구조):
     * ```json
     * {
     *   "id": 12345678,
     *   "kakao_account": {
     *     "email": "user@kakao.com",
     *     "profile": {
     *       "nickname": "홍길동",             ← kakao_account.profile.nickname
     *       "profile_image_url": "https://..."
     *     }
     *   }
     * }
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseKakaoUser(attributes: Map<String, Any>): OAuth2UserInfo {
        val kakaoAccount = attributes["kakao_account"] as Map<String, Any>
        val profile = kakaoAccount["profile"] as Map<String, Any>

        return OAuth2UserInfo(
            email = kakaoAccount["email"] as String,
            name = profile["nickname"] as? String ?: "Kakao 사용자",
            providerId = attributes["id"].toString(),  // Kakao 고유 ID (Long 타입이므로 toString)
            profileImageUrl = profile["profile_image_url"] as? String,
        )
    }

    /**
     * OAuth2 사용자 처리 (조회 또는 생성)
     *
     * 기존 회원이면 로그인 처리, 신규 회원이면 자동 가입 후 JWT 발급.
     *
     * @param email 이메일
     * @param name 이름/닉네임
     * @param provider 제공자 (GOOGLE/NAVER/KAKAO)
     * @param providerId 제공자 고유 ID
     * @param profileImageUrl 프로필 이미지 URL
     * @return [TokenResponse]
     */
    fun processOAuth2User(
        email: String,
        name: String,
        provider: String,
        providerId: String,
        profileImageUrl: String?,
    ): TokenResponse {
        log.info("OAuth2 사용자 처리: email={}, provider={}", email, provider)

        // 기존 회원 조회
        val existingUser = runCatching {
            userServiceClient.findByProviderAndProviderId(provider, providerId)
        }.getOrNull()

        val userId: String
        val userEmail: String

        if (existingUser != null) {
            // 기존 회원: 로그인 처리
            userId = existingUser.id
            userEmail = existingUser.email
            log.info("기존 소셜 회원 로그인: userId={}", userId)
        } else {
            // 신규 회원: 자동 가입
            userId = java.util.UUID.randomUUID().toString()
            userEmail = email

            // Kafka 이벤트 발행으로 User Service에 사용자 생성 요청
            val event = UserCreatedEvent(
                userId = userId,
                email = email,
                password = null,                // 소셜 로그인은 비밀번호 없음
                nickname = name,
                provider = provider,
                providerId = providerId,
                profileImageUrl = profileImageUrl,
            )
            authEventProducer.publishUserCreatedEvent(event)
            log.info("신규 소셜 회원 가입 처리: userId={}, provider={}", userId, provider)
        }

        // JWT 토큰 발급
        return authService.generateAndSaveTokens(userId, userEmail)
    }

    /**
     * OAuth2 사용자 정보를 담는 내부 데이터 클래스
     *
     * 제공자별로 파싱된 사용자 정보를 통일된 형태로 표현한다.
     * Kotlin의 구조 분해(destructuring)를 활용하기 위해 data class로 정의.
     *
     * @property email 이메일
     * @property name 이름/닉네임
     * @property providerId 제공자 고유 사용자 ID
     * @property profileImageUrl 프로필 이미지 URL (없을 수 있음)
     */
    data class OAuth2UserInfo(
        val email: String,
        val name: String,
        val providerId: String,
        val profileImageUrl: String?,
    )
}
