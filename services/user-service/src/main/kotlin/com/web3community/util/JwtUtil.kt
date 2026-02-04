package com.web3community.util

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtUtil {

    @Value("\${jwt.secret:mySecretKey}")
    private lateinit var secret: String

    @Value("\${jwt.expiration:86400}")
    private val expiration: Long = 86400

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration * 1000)

        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getSubjectFromToken(token: String): String? {
        return try {
            val claims = getClaimsFromToken(token)
            claims.subject
        } catch (e: Exception) {
            null
        }
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}