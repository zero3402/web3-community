package com.web3community.util

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

/**
 * JWT(JSON Web Token) 유틸리티 클래스
 * 토큰 생성, 검증, 파싱 기능 제공
 * Spring Security와 연동하여 인증/인가 처리
 */
@Component
class JwtUtil {

    @Value("\${jwt.secret:mySecretKey}")
    private lateinit var secret: String

    @Value("\${jwt.expiration:86400}") // 24시간 (초)
    private val expiration: Long = 86400

    @Value("\${jwt.refresh-expiration:604800}") // 7일 (초)
    private val refreshExpiration: Long = 604800

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /**
     * Access Token 생성
     * @param subject 토큰 주체 (일반적으로 사용자 이메일)
     * @param role 사용자 역할
     * @return JWT Access Token
     */
    fun generateToken(subject: String, role: String): String {
        return generateToken(subject, role, expiration)
    }

    /**
     * Refresh Token 생성
     * @param subject 토큰 주체 (일반적으로 사용자 이메일)
     * @return JWT Refresh Token
     */
    fun generateRefreshToken(subject: String): String {
        return generateToken(subject, "REFRESH", refreshExpiration)
    }

    /**
     * JWT 토큰 생성 (내부 메소드)
     * @param subject 토큰 주체
     * @param role 사용자 역할
     * @param expiration 만료 시간 (초)
     * @return JWT 토큰
     */
    private fun generateToken(subject: String, role: String, expiration: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration * 1000)

        return Jwts.builder()
            .setSubject(subject)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * 토큰에서 클레임 추출
     * @param token JWT 토큰
     * @return Claims 객체
     */
    fun getClaimsFromToken(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    /**
     * 토큰에서 주체(subject) 추출
     * @param token JWT 토큰
     * @return 주체 (일반적으로 사용자 이메일)
     */
    fun getSubjectFromToken(token: String): String? {
        return try {
            getClaimsFromToken(token).subject
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 토큰에서 사용자 역할 추출
     * @param token JWT 토큰
     * @return 사용자 역할
     */
    fun getRoleFromToken(token: String): String? {
        return try {
            getClaimsFromToken(token)["role"] as String?
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 토큰 만료 시간 추출
     * @param token JWT 토큰
     * @return 만료 시간 (초)
     */
    fun getExpirationFromToken(token: String): Long {
        return try {
            val claims = getClaimsFromToken(token)
            val expiration = claims.expiration
            val now = Date()
            (expiration.time - now.time) / 1000
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 토큰 만료 시간 반환 (초)
     * @return 만료 시간 (초)
     */
    fun getExpirationTime(): Long {
        return expiration
    }

    /**
     * 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 유효 여부
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            !claims.expiration.before(Date())
        } catch (e: SignatureException) {
            false
        } catch (e: MalformedJwtException) {
            false
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: UnsupportedJwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * 토큰이 만료되었는지 확인
     * @param token JWT 토큰
     * @return 만료 여부
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            claims.expiration.before(Date())
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Refresh Token 유효성 검증
     * @param token JWT Refresh Token
     * @return 유효 여부
     */
    fun validateRefreshToken(token: String): Boolean {
        return try {
            if (!validateToken(token)) return false
            
            val role = getRoleFromToken(token)
            role == "REFRESH"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 토큰에서 토큰 타입 확인
     * @param token JWT 토큰
     * @return 토큰 타입 (ACCESS, REFRESH, 또는 UNKNOWN)
     */
    fun getTokenType(token: String): String {
        return try {
            val role = getRoleFromToken(token) ?: return "UNKNOWN"
            when (role) {
                "REFRESH" -> "REFRESH"
                else -> "ACCESS"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    /**
     * 토큰 갱신
     * 기존 토큰이 유효하면 새로운 토큰 발급
     * @param token 기존 JWT 토큰
     * @return 새로운 JWT 토큰
     */
    fun refreshToken(token: String): String? {
        return try {
            if (!validateToken(token)) return null
            
            val subject = getSubjectFromToken(token) ?: return null
            val role = getRoleFromToken(token) ?: return null
            
            if (role == "REFRESH") {
                generateToken(subject, "USER")
            } else {
                generateToken(subject, role)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 토큰에서 모든 정보 추출
     * @param token JWT 토큰
     * @return 토큰 정보 맵
     */
    fun getTokenInfo(token: String): Map<String, Any?> {
        return try {
            val claims = getClaimsFromToken(token)
            mapOf(
                "subject" to claims.subject,
                "role" to claims["role"],
                "issuedAt" to claims.issuedAt,
                "expiration" to claims.expiration,
                "tokenType" to getTokenType(token),
                "isValid" to validateToken(token),
                "isExpired" to isTokenExpired(token)
            )
        } catch (e: Exception) {
            mapOf(
                "subject" to null,
                "role" to null,
                "issuedAt" to null,
                "expiration" to null,
                "tokenType" to "UNKNOWN",
                "isValid" to false,
                "isExpired" to true,
                "error" to e.message
            )
        }
    }

    /**
     * Bearer 토큰에서 실제 토큰 문자열 추출
     * @param bearerToken Bearer 토큰 ("Bearer " 접두사 포함)
     * @return 실제 토큰 문자열
     */
    fun extractTokenFromBearer(bearerToken: String?): String? {
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}