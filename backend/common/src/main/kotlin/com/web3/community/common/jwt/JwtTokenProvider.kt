package com.web3.community.common.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import java.util.*
import javax.crypto.SecretKey

class JwtTokenProvider(private val properties: JwtProperties) {

    private val key: SecretKey = Keys.hmacShaKeyFor(
        Base64.getDecoder().decode(properties.secret)
    )

    fun generateAccessToken(userId: Long, email: String, role: String, nickname: String): String {
        val claims = mapOf(
            "userId" to userId,
            "email" to email,
            "role" to role,
            "nickname" to nickname
        )
        return buildToken(claims, userId.toString(), properties.accessTokenExpiration)
    }

    fun generateRefreshToken(userId: Long): String {
        return buildToken(emptyMap(), userId.toString(), properties.refreshTokenExpiration)
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = parseClaims(token)
            !claims.expiration.before(Date())
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun getUserIdFromToken(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    fun getEmailFromToken(token: String): String {
        return parseClaims(token)["email"] as String
    }

    fun getRoleFromToken(token: String): String {
        return parseClaims(token)["role"] as String
    }

    fun getNicknameFromToken(token: String): String {
        return parseClaims(token)["nickname"] as? String ?: ""
    }

    fun getExpirationFromToken(token: String): Date {
        return parseClaims(token).expiration
    }

    fun getRemainingExpiration(token: String): Long {
        val expiration = getExpirationFromToken(token)
        return expiration.time - System.currentTimeMillis()
    }

    private fun buildToken(claims: Map<String, Any>, subject: String, expiration: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuer(properties.issuer)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}
