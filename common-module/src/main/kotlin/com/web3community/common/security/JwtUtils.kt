package com.web3community.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

/**
 * JwtUtils - HMAC-SHA256 기반 JWT 토큰 생성 및 검증 유틸리티
 *
 * HS256(HMAC + SHA-256) 대칭 키 알고리즘으로 JWT를 생성하고 검증합니다.
 * auth-service에서 토큰을 발급하고, 각 서비스의 필터에서 검증에 사용합니다.
 *
 * 설계 결정:
 * - [JwtProvider]와 달리 userId를 String으로 처리합니다.
 *   auth-service는 UUID(String) 기반 userId를 사용하기 때문입니다.
 * - Refresh Token에도 email 클레임을 포함하여, 갱신 시 DB 조회 없이 이메일을 추출할 수 있습니다.
 *
 * 설정 (application.yml):
 * ```yaml
 * jwt:
 *   secret: ${JWT_SECRET}                   # HMAC-SHA256 서명 키 (256비트 이상, Base64 인코딩)
 *   access-token-expiration: 1800000         # Access Token 만료: 30분 (ms)
 *   refresh-token-expiration: 604800000      # Refresh Token 만료: 7일 (ms)
 * ```
 *
 * 토큰 클레임 구조:
 * - Access Token: sub(userId), email, type("ACCESS"), iat, exp
 * - Refresh Token: sub(userId), email, type("REFRESH"), iat, exp
 *
 * @property secret HMAC-SHA256 서명 키 (환경변수 JWT_SECRET에서 주입)
 * @property accessTokenExpiration Access Token 만료 시간 (ms, 기본값: 30분)
 * @property refreshTokenExpiration Refresh Token 만료 시간 (ms, 기본값: 7일)
 */
@Component
class JwtUtils(
    /** HMAC-SHA256 서명에 사용할 비밀 키 문자열 (256비트 이상 권장) */
    @Value("\${jwt.secret}")
    private val secret: String,

    /** Access Token 만료 시간 (밀리초, 기본값: 1800000 = 30분) */
    @Value("\${jwt.access-token-expiration:1800000}")
    private val accessTokenExpiration: Long,

    /** Refresh Token 만료 시간 (밀리초, 기본값: 604800000 = 7일) */
    @Value("\${jwt.refresh-token-expiration:604800000}")
    private val refreshTokenExpiration: Long,
) {

    private val log = LoggerFactory.getLogger(JwtUtils::class.java)

    /**
     * HMAC-SHA256 서명 키 (지연 초기화)
     *
     * Keys.hmacShaKeyFor()는 바이트 배열로부터 안전한 HMAC 키 객체를 생성합니다.
     * 256비트(32바이트) 미만의 키는 HS256에 사용할 수 없으므로 충분히 긴 secret을 제공해야 합니다.
     */
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    }

    // ============================================================
    // 토큰 생성
    // ============================================================

    /**
     * Access Token 생성
     *
     * 사용자 인증 완료 후 발급하는 단기 토큰입니다.
     * API 요청 시 `Authorization: Bearer {token}` 헤더에 포함됩니다.
     *
     * 클레임 구성:
     * - `sub`: userId (UUID 문자열)
     * - `email`: 사용자 이메일
     * - `type`: "ACCESS" (Refresh Token과 구분)
     * - `iat`: 발급 시각 (Unix timestamp)
     * - `exp`: 만료 시각 (발급 시각 + [accessTokenExpiration])
     *
     * @param userId 사용자 고유 식별자 (auth-service에서 생성한 UUID 문자열)
     * @param email 사용자 이메일 주소
     * @return 서명된 JWT Access Token 문자열
     */
    fun generateAccessToken(userId: String, email: String): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            // subject: userId (UUID 문자열)
            .subject(userId)
            // 커스텀 클레임: 이메일 (다운스트림 서비스에서 활용)
            .claim("email", email)
            // 토큰 타입 구분: Refresh Token으로 Access Token API 호출 방지
            .claim("type", "ACCESS")
            // 발급 시각
            .issuedAt(now)
            // 만료 시각
            .expiration(expiry)
            // HMAC-SHA256 서명
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    /**
     * Refresh Token 생성
     *
     * Access Token 만료 시 재발급에 사용하는 장기 토큰입니다.
     * Redis에 저장되어 로그아웃 시 즉시 무효화 가능합니다.
     *
     * 설계 결정:
     * - Refresh Token에도 email을 포함합니다.
     *   재발급 시 [extractEmail]로 이메일을 꺼내 새 Access Token을 발급할 수 있어
     *   User Service Feign 호출을 최소화합니다.
     * - Refresh Token Rotation 전략: 매 갱신마다 새 토큰 발급, 이전 토큰 무효화
     *
     * @param userId 사용자 고유 식별자 (UUID 문자열)
     * @param email 사용자 이메일 주소
     * @return 서명된 JWT Refresh Token 문자열
     */
    fun generateRefreshToken(userId: String, email: String): String {
        val now = Date()
        val expiry = Date(now.time + refreshTokenExpiration)

        return Jwts.builder()
            .subject(userId)
            // Refresh Token에도 email 포함: 재발급 시 DB 조회 없이 이메일 추출 가능
            .claim("email", email)
            .claim("type", "REFRESH")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    // ============================================================
    // 토큰 검증
    // ============================================================

    /**
     * JWT 토큰 유효성 검증
     *
     * 다음 항목을 순서대로 검증합니다:
     * 1. HMAC-SHA256 서명 유효성
     * 2. 토큰 만료 여부 (`exp` 클레임)
     * 3. 토큰 형식 유효성 (JWT 구조 3파트)
     *
     * 유효하지 않은 경우 false를 반환하며 예외를 전파하지 않습니다.
     * 예외가 필요한 경우 직접 [extractUserId] 등을 호출하세요.
     *
     * @param token 검증할 JWT 토큰 문자열
     * @return 서명이 유효하고 만료되지 않은 경우 `true`, 그 외 `false`
     */
    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: ExpiredJwtException) {
            log.warn("만료된 JWT 토큰: {}", e.message)
            false
        } catch (e: SignatureException) {
            log.warn("유효하지 않은 JWT 서명: {}", e.message)
            false
        } catch (e: MalformedJwtException) {
            log.warn("잘못된 JWT 형식: {}", e.message)
            false
        } catch (e: UnsupportedJwtException) {
            log.warn("지원하지 않는 JWT 형식: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            log.warn("JWT 토큰이 비어있거나 null: {}", e.message)
            false
        }
    }

    // ============================================================
    // 클레임 추출
    // ============================================================

    /**
     * 토큰에서 userId(subject) 추출
     *
     * JWT의 `sub` 클레임에서 userId를 추출합니다.
     * auth-service는 UUID 기반 userId를 사용하므로 반환 타입이 String입니다.
     *
     * @param token JWT 토큰 문자열 (유효한 토큰이어야 합니다)
     * @return userId (UUID 문자열)
     * @throws JwtException 토큰이 유효하지 않거나 만료된 경우
     */
    fun extractUserId(token: String): String {
        return parseClaims(token).subject
    }

    /**
     * 토큰에서 이메일 추출
     *
     * JWT 페이로드의 `email` 커스텀 클레임에서 이메일을 추출합니다.
     * Access Token 및 Refresh Token 모두에서 추출 가능합니다.
     *
     * @param token JWT 토큰 문자열 (유효한 토큰이어야 합니다)
     * @return 사용자 이메일 주소
     * @throws JwtException 토큰이 유효하지 않거나 만료된 경우
     * @throws ClassCastException email 클레임이 문자열이 아닌 경우 (비정상 토큰)
     */
    fun extractEmail(token: String): String {
        return parseClaims(token)["email"] as String
    }

    /**
     * Access Token 만료 시간 반환
     *
     * [TokenResponse]의 `expiresIn` 필드에 사용됩니다.
     * 클라이언트는 이 값을 참고하여 토큰 갱신 타이밍을 결정합니다.
     *
     * @return Access Token 만료 시간 (밀리초)
     */
    fun getAccessTokenExpiration(): Long = accessTokenExpiration

    // ============================================================
    // 내부 헬퍼
    // ============================================================

    /**
     * JWT 토큰 파싱 및 클레임 추출 (내부 전용)
     *
     * 서명 키([signingKey])로 토큰 서명을 검증하고, 페이로드 클레임을 반환합니다.
     * 만료된 토큰, 잘못된 서명, 형식 오류 시 각각 다른 [JwtException]이 발생합니다.
     *
     * @param token JWT 토큰 문자열
     * @return 파싱된 [Claims] 객체
     * @throws ExpiredJwtException 토큰 만료
     * @throws SignatureException 서명 검증 실패
     * @throws MalformedJwtException 토큰 형식 오류
     * @throws UnsupportedJwtException 지원하지 않는 JWT 타입
     */
    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            // HMAC-SHA256 서명 검증 키 설정
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
