package com.web3community.auth.service

import com.web3community.auth.client.UserServiceClient
import com.web3community.auth.dto.LoginRequest
import com.web3community.auth.dto.RegisterRequest
import com.web3community.auth.dto.TokenResponse
import com.web3community.auth.kafka.AuthEventProducer
import com.web3community.common.dto.UserCreatedEvent
import com.web3community.common.exception.AuthException
import com.web3community.common.security.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/**
 * 인증 서비스
 *
 * 회원가입, 로그인, 로그아웃, 토큰 갱신의 핵심 비즈니스 로직을 담당한다.
 *
 * 아키텍처 설계:
 * - 사용자 데이터는 User Service(Feign)를 통해 조회/생성한다.
 *   Auth Service가 직접 DB에 접근하지 않아 서비스 분리 원칙을 지킨다.
 * - 회원가입 시 Kafka 이벤트를 발행하고 User Service가 이를 소비하여 DB에 저장한다.
 *   이를 통해 Auth Service와 User Service 간의 강한 결합을 방지한다.
 * - Refresh Token은 Redis에 저장하여 빠른 조회 및 TTL 기반 자동 만료를 활용한다.
 */
@Service
class AuthService(
    private val userServiceClient: UserServiceClient,   // User Service Feign 클라이언트
    private val tokenService: TokenService,             // Redis Refresh Token CRUD
    private val authEventProducer: AuthEventProducer,   // Kafka 이벤트 발행
    private val passwordEncoder: PasswordEncoder,       // BCrypt 비밀번호 해싱
    private val jwtUtils: JwtUtils,                     // JWT 생성 및 검증
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * 회원가입
     *
     * 처리 순서:
     * 1. 이메일 중복 확인 (User Service Feign 호출)
     * 2. 비밀번호 BCrypt 해싱
     * 3. Kafka에 [UserCreatedEvent] 발행 (User Service가 비동기로 DB 저장)
     * 4. JWT Access Token + Refresh Token 발급 (가입 즉시 로그인 처리)
     *
     * 설계 결정:
     * - User Service에 동기적으로 사용자 생성 요청(Feign)을 하지 않고 Kafka를 사용한다.
     *   이유: User Service가 일시적으로 다운되더라도 인증 서비스는 정상 동작하도록 내결함성 확보
     * - 단, 이메일 중복 확인은 동기(Feign)로 처리한다. 이는 즉각적인 피드백이 필요하기 때문.
     *
     * @param request 회원가입 요청 DTO
     * @return JWT 토큰 응답
     * @throws AuthException 이메일 중복 시
     */
    fun register(request: RegisterRequest): TokenResponse {
        log.info("회원가입 요청: email={}", request.email)

        // 1. 이메일 중복 확인
        val existingUser = runCatching {
            userServiceClient.findByEmail(request.email)
        }.getOrNull()

        if (existingUser != null) {
            // 이미 존재하는 이메일이면 예외 발생
            throw AuthException("이미 사용 중인 이메일입니다: ${request.email}")
        }

        // 2. 비밀번호 BCrypt 해싱
        // BCrypt는 매번 랜덤 salt를 생성하므로 같은 비밀번호도 다른 해시값 생성
        val encodedPassword = passwordEncoder.encode(request.password)

        // 3. 사용자 ID 생성 (UUID 사용)
        val userId = java.util.UUID.randomUUID().toString()

        // 4. Kafka 이벤트 발행 (User Service가 비동기로 DB에 저장)
        val event = UserCreatedEvent(
            userId = userId,
            email = request.email,
            password = encodedPassword,      // 해싱된 비밀번호 전달
            nickname = request.nickname,
            provider = "LOCAL",              // 일반 회원가입은 LOCAL 제공자
            providerId = null,
            profileImageUrl = null,
        )
        authEventProducer.publishUserCreatedEvent(event)
        log.info("UserCreatedEvent 발행 완료: userId={}", userId)

        // 5. JWT 토큰 발급 (가입 즉시 로그인 처리)
        return generateAndSaveTokens(userId, request.email)
    }

    /**
     * 일반 로그인
     *
     * 처리 순서:
     * 1. User Service에서 이메일로 사용자 조회 (Feign)
     * 2. 비밀번호 BCrypt 검증
     * 3. Access Token + Refresh Token 발급
     * 4. Refresh Token Redis에 저장
     *
     * @param request 로그인 요청 DTO (이메일, 비밀번호)
     * @return JWT 토큰 응답
     * @throws AuthException 사용자 없음 또는 비밀번호 불일치 시
     */
    fun login(request: LoginRequest): TokenResponse {
        log.info("로그인 요청: email={}", request.email)

        // 1. 이메일로 사용자 조회
        val user = runCatching {
            userServiceClient.findByEmail(request.email)
        }.getOrElse {
            // Feign 호출 실패 = 사용자 없음으로 처리 (보안상 구체적인 오류 숨김)
            throw AuthException("이메일 또는 비밀번호가 올바르지 않습니다.")
        } ?: throw AuthException("이메일 또는 비밀번호가 올바르지 않습니다.")

        // 2. 비밀번호 검증
        // passwordEncoder.matches()는 평문 비밀번호와 저장된 BCrypt 해시를 비교
        if (user.password == null || !passwordEncoder.matches(request.password, user.password)) {
            // 보안상 "이메일 없음"과 "비밀번호 틀림"을 구분하지 않는다 (사용자 열거 공격 방지)
            throw AuthException("이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        // 3. JWT 토큰 발급 및 Redis 저장
        return generateAndSaveTokens(user.id, user.email)
    }

    /**
     * 로그아웃
     *
     * Redis에서 Refresh Token을 삭제하여 이후 토큰 갱신을 불가능하게 한다.
     * Access Token은 만료 시간이 짧으므로 (30분) 별도 처리하지 않는다.
     *
     * @param refreshToken 무효화할 Refresh Token
     */
    fun logout(refreshToken: String) {
        // Refresh Token에서 사용자 ID 추출 (유효성 검증 포함)
        val userId = runCatching {
            jwtUtils.extractUserId(refreshToken)
        }.getOrElse {
            // 이미 만료된 토큰이면 Redis에 없으니 무시 (멱등성 보장)
            log.warn("로그아웃 시 유효하지 않은 토큰 수신")
            return
        }

        // Redis에서 Refresh Token 삭제
        tokenService.deleteRefreshToken(userId)
        log.info("로그아웃 완료: userId={}", userId)
    }

    /**
     * Access Token 재발급 (Refresh Token Rotation)
     *
     * Refresh Token Rotation 전략으로 보안을 강화한다:
     * - 매 갱신마다 새로운 Refresh Token을 발급
     * - 이전 Refresh Token은 즉시 무효화
     * - 탈취된 Refresh Token이 재사용되면 Redis 불일치로 감지 가능
     *
     * 처리 순서:
     * 1. Refresh Token JWT 서명 검증
     * 2. Redis에서 저장된 Refresh Token과 비교 (재사용 공격 감지)
     * 3. 새 Access Token + Refresh Token 발급
     * 4. Redis에 새 Refresh Token 저장 (이전 것 덮어쓰기)
     *
     * @param refreshToken 클라이언트가 제출한 Refresh Token
     * @return 새로운 JWT 토큰 응답
     * @throws AuthException 유효하지 않은 Refresh Token 또는 Redis 불일치 시
     */
    fun refreshToken(refreshToken: String): TokenResponse {
        // 1. JWT 서명 검증 및 클레임 추출
        if (!jwtUtils.validateToken(refreshToken)) {
            throw AuthException("유효하지 않거나 만료된 Refresh Token입니다.")
        }

        val userId = jwtUtils.extractUserId(refreshToken)

        // 2. Redis에 저장된 Refresh Token과 비교
        val storedToken = tokenService.getRefreshToken(userId)
            ?: throw AuthException("로그아웃된 사용자입니다. 다시 로그인해주세요.")

        if (storedToken != refreshToken) {
            // Refresh Token 불일치: 탈취 후 재사용 시도로 판단
            // 저장된 토큰도 삭제하여 해당 사용자의 모든 세션을 강제 로그아웃
            tokenService.deleteRefreshToken(userId)
            log.warn("Refresh Token 재사용 공격 감지! userId={}", userId)
            throw AuthException("유효하지 않은 Refresh Token입니다. 보안을 위해 재로그인이 필요합니다.")
        }

        // 3. 사용자 이메일 조회 (JWT 클레임에서 추출)
        val email = jwtUtils.extractEmail(refreshToken)

        // 4. 새 토큰 발급 및 Redis 업데이트 (Rotation)
        return generateAndSaveTokens(userId, email)
    }

    /**
     * JWT 토큰 쌍 생성 및 Redis 저장 (공통 헬퍼)
     *
     * Access Token과 Refresh Token을 생성하고 Refresh Token을 Redis에 저장한다.
     * 로그인, 회원가입, 토큰 갱신 시 공통으로 사용된다.
     *
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @return [TokenResponse] (accessToken, refreshToken, expiresIn)
     */
    fun generateAndSaveTokens(userId: String, email: String): TokenResponse {
        // Access Token 생성 (만료: 30분)
        val accessToken = jwtUtils.generateAccessToken(userId, email)

        // Refresh Token 생성 (만료: 7일)
        val refreshToken = jwtUtils.generateRefreshToken(userId, email)

        // Refresh Token Redis에 저장 (TTL 7일)
        tokenService.saveRefreshToken(userId, refreshToken)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtUtils.getAccessTokenExpiration(),  // 만료 시간 (ms)
        )
    }
}
