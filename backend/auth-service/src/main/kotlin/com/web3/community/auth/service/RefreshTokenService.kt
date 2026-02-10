package com.web3.community.auth.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RefreshTokenService(
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        private const val REFRESH_TOKEN_PREFIX = "refresh_token:"
        private const val BLACKLIST_PREFIX = "blacklist:"
    }

    fun saveRefreshToken(userId: Long, refreshToken: String, expirationMs: Long) {
        redisTemplate.opsForValue().set(
            "$REFRESH_TOKEN_PREFIX$userId",
            refreshToken,
            expirationMs,
            TimeUnit.MILLISECONDS
        )
    }

    fun getRefreshToken(userId: Long): String? {
        return redisTemplate.opsForValue().get("$REFRESH_TOKEN_PREFIX$userId")
    }

    fun deleteRefreshToken(userId: Long) {
        redisTemplate.delete("$REFRESH_TOKEN_PREFIX$userId")
    }

    fun blacklistAccessToken(token: String, remainingExpirationMs: Long) {
        if (remainingExpirationMs > 0) {
            redisTemplate.opsForValue().set(
                "$BLACKLIST_PREFIX$token",
                "true",
                remainingExpirationMs,
                TimeUnit.MILLISECONDS
            )
        }
    }

    fun isTokenBlacklisted(token: String): Boolean {
        return redisTemplate.hasKey("$BLACKLIST_PREFIX$token")
    }
}
