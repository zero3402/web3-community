package com.web3community.common.security

import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Date

/**
 * JwtProvider - JWT 토큰 생성 및 검증 컴포넌트
 *
 * RS256(RSA + SHA-256) 비대칭 키 알고리즘을 사용하여 JWT를 생성하고 검증합니다.
 *
 * 비대칭 키를 사용하는 이유:
 * - auth-service만 프라이빗 키를 보유하여 토큰 생성 (위조 불가)
 * - 다른 서비스들은 퍼블릭 키로 검증만 가능 (키 노출되어도 생성 불가)
 * - MSA 환경에서 각 서비스에 퍼블릭 키만 배포하면 됨
 *
 * 토큰 구조:
 * - Access Token: 15분 유효, API 요청 시 Authorization 헤더에 포함
 * - Refresh Token: 7일 유효, Redis에 저장, Access Token 재발급에 사용
 *
 * 키 관리:
 * - 프라이빗 키: auth-service의 application.yml (Kubernetes Secret으로 주입)
 * - 퍼블릭 키: 모든 서비스의 application.yml (ConfigMap으로 배포)
 *
 * 설정 예시 (application.yml):
 * ```yaml
 * jwt:
 *   private-key: MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEA... (PKCS8 Base64)
 *   public-key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA... (X.509 Base64)
 *   access-token-expiration: 900000     # 15분 (밀리초)
 *   refresh-token-expiration: 604800000 # 7일 (밀리초)
 * ```
 */
@Component
class JwtProvider(
    /** RSA 프라이빗 키 (Base64 인코딩된 PKCS8 형식) - auth-service에서만 사용 */
    @Value("\${jwt.private-key:}")
    private val privateKeyBase64: String,

    /** RSA 퍼블릭 키 (Base64 인코딩된 X.509 형식) - 모든 서비스에서 사용 */
    @Value("\${jwt.public-key}")
    private val publicKeyBase64: String,

    /** Access Token 만료 시간 (밀리초, 기본값: 15분) */
    @Value("\${jwt.access-token-expiration:900000}")
    private val accessTokenExpiration: Long,

    /** Refresh Token 만료 시간 (밀리초, 기본값: 7일) */
    @Value("\${jwt.refresh-token-expiration:604800000}")
    private val refreshTokenExpiration: Long
) {

    private val log = LoggerFactory.getLogger(JwtProvider::class.java)

    /** RSA 프라이빗 키 객체 (지연 초기화 - 프라이빗 키가 설정된 서비스에서만 사용) */
    private val privateKey: PrivateKey? by lazy {
        if (privateKeyBase64.isBlank()) null
        else {
            try {
                val keyBytes = Base64.getDecoder().decode(privateKeyBase64)
                val keySpec = PKCS8EncodedKeySpec(keyBytes)
                KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            } catch (e: Exception) {
                log.error("RSA 프라이빗 키 초기화 실패", e)
                throw IllegalStateException("JWT 프라이빗 키 설정이 올바르지 않습니다.", e)
            }
        }
    }

    /** RSA 퍼블릭 키 객체 (지연 초기화) */
    private val publicKey: PublicKey by lazy {
        try {
            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            KeyFactory.getInstance("RSA").generatePublic(keySpec)
        } catch (e: Exception) {
            log.error("RSA 퍼블릭 키 초기화 실패", e)
            throw IllegalStateException("JWT 퍼블릭 키 설정이 올바르지 않습니다.", e)
        }
    }

    // ============================================================
    // 토큰 생성
    // ============================================================

    /**
     * Access Token 생성
     *
     * 사용자 인증 후 발급되는 단기 토큰입니다.
     * API 요청 시 Authorization: Bearer {token} 헤더로 전달됩니다.
     *
     * 포함 클레임:
     * - sub: userId (문자열)
     * - email: 사용자 이메일
     * - roles: 권한 목록 (쉼표 구분 문자열)
     * - type: "ACCESS"
     * - iat: 발급 시간
     * - exp: 만료 시간 (발급 + 15분)
     *
     * @param claims JWT 페이로드에 포함할 사용자 정보
     * @return 서명된 JWT Access Token 문자열
     * @throws IllegalStateException 프라이빗 키가 설정되지 않은 경우
     */
    fun generateAccessToken(claims: JwtClaims): String {
        val key = privateKey
            ?: throw IllegalStateException("프라이빗 키가 설정되지 않았습니다. auth-service에서만 토큰 생성이 가능합니다.")

        val now = Date()
        val expiry = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            // 주체: userId를 문자열로 설정
            .subject(claims.userId.toString())
            // 커스텀 클레임: 이메일
            .claim("email", claims.email)
            // 커스텀 클레임: 권한 목록 (쉼표 구분)
            .claim("roles", claims.roles.joinToString(","))
            // 토큰 타입 구분 (Access / Refresh 혼용 방지)
            .claim("type", "ACCESS")
            // 발급 시간
            .issuedAt(now)
            // 만료 시간 (15분)
            .expiration(expiry)
            // RS256 서명
            .signWith(key, Jwts.SIG.RS256)
            .compact()
    }

    /**
     * Refresh Token 생성
     *
     * Access Token 만료 시 재발급에 사용하는 장기 토큰입니다.
     * Redis에 저장되며, 로그아웃 시 즉시 무효화됩니다.
     *
     * 보안 설계:
     * - Refresh Token에는 userId만 포함 (최소한의 정보)
     * - 실제 권한 검증은 Access Token 재발급 시 DB에서 다시 조회
     * - Redis에 저장된 토큰과 일치 여부 확인 (탈취 감지)
     *
     * @param claims JWT 페이로드에 포함할 사용자 정보
     * @return 서명된 JWT Refresh Token 문자열
     */
    fun generateRefreshToken(claims: JwtClaims): String {
        val key = privateKey
            ?: throw IllegalStateException("프라이빗 키가 설정되지 않았습니다.")

        val now = Date()
        val expiry = Date(now.time + refreshTokenExpiration)

        return Jwts.builder()
            .subject(claims.userId.toString())
            // Refresh Token은 userId만 포함 (최소 정보 원칙)
            .claim("type", "REFRESH")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key, Jwts.SIG.RS256)
            .compact()
    }

    // ============================================================
    // 토큰 검증
    // ============================================================

    /**
     * JWT 토큰 유효성 검증
     *
     * 다음 항목을 검증합니다:
     * 1. 서명 유효성 (RSA 퍼블릭 키로 검증)
     * 2. 토큰 만료 여부
     * 3. 토큰 형식 유효성
     *
     * @param token 검증할 JWT 토큰 문자열
     * @return 유효한 토큰이면 true, 그렇지 않으면 false
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
            log.warn("JWT 클레임이 비어있음: {}", e.message)
            false
        }
    }

    /**
     * JWT 토큰 유효성 검증 (예외 발생 방식)
     *
     * 게이트웨이 필터 등에서 유효하지 않은 토큰 시 즉시 BusinessException을 발생시킬 때 사용합니다.
     *
     * @param token 검증할 JWT 토큰 문자열
     * @throws BusinessException AUTH_003 (토큰 만료) 또는 AUTH_004 (유효하지 않은 토큰)
     */
    fun validateTokenOrThrow(token: String) {
        try {
            parseClaims(token)
        } catch (e: ExpiredJwtException) {
            throw BusinessException(ErrorCode.AUTH_003)
        } catch (e: JwtException) {
            throw BusinessException(ErrorCode.AUTH_004)
        }
    }

    // ============================================================
    // 클레임 추출
    // ============================================================

    /**
     * 토큰에서 userId 추출
     *
     * @param token JWT 토큰 문자열
     * @return userId (Long)
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    fun getUserIdFromToken(token: String): Long {
        return try {
            parseClaims(token).subject.toLong()
        } catch (e: ExpiredJwtException) {
            throw BusinessException(ErrorCode.AUTH_003)
        } catch (e: JwtException) {
            throw BusinessException(ErrorCode.AUTH_004)
        }
    }

    /**
     * 토큰에서 이메일 추출
     *
     * @param token JWT 토큰 문자열
     * @return 이메일 주소
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    fun getEmailFromToken(token: String): String {
        return try {
            parseClaims(token)["email"] as String
        } catch (e: ExpiredJwtException) {
            throw BusinessException(ErrorCode.AUTH_003)
        } catch (e: JwtException) {
            throw BusinessException(ErrorCode.AUTH_004)
        }
    }

    /**
     * 토큰에서 JwtClaims 객체 추출
     *
     * 토큰에서 모든 클레임 정보를 한 번에 추출합니다.
     * API Gateway의 인증 필터에서 SecurityContext를 구성할 때 사용합니다.
     *
     * @param token JWT 토큰 문자열
     * @return JwtClaims 데이터 클래스 인스턴스
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    fun getClaimsFromToken(token: String): JwtClaims {
        return try {
            val claims = parseClaims(token)
            JwtClaims(
                userId = claims.subject.toLong(),
                email = claims["email"] as? String ?: "",
                roles = (claims["roles"] as? String)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?: emptyList()
            )
        } catch (e: ExpiredJwtException) {
            throw BusinessException(ErrorCode.AUTH_003)
        } catch (e: JwtException) {
            throw BusinessException(ErrorCode.AUTH_004)
        }
    }

    /**
     * 만료된 토큰에서 userId 추출 (Refresh Token 갱신 시 사용)
     *
     * Access Token이 만료되었더라도 Refresh Token을 통한 재발급 과정에서
     * 만료된 Access Token의 userId를 추출해야 할 때 사용합니다.
     *
     * @param token 만료된 JWT 토큰 문자열
     * @return userId (Long)
     */
    fun getUserIdFromExpiredToken(token: String): Long {
        return try {
            parseClaims(token).subject.toLong()
        } catch (e: ExpiredJwtException) {
            // 만료 예외에서도 클레임 정보는 추출 가능
            e.claims.subject.toLong()
        }
    }

    // ============================================================
    // 내부 헬퍼 메서드
    // ============================================================

    /**
     * JWT 토큰 파싱 (내부 사용)
     *
     * @param token JWT 토큰 문자열
     * @return 파싱된 Claims 객체
     * @throws JwtException 토큰이 유효하지 않은 경우 (다양한 하위 예외 포함)
     */
    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(publicKey)  // 퍼블릭 키로 서명 검증
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
