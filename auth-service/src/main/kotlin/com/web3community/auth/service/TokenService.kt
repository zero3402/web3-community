package com.web3community.auth.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Refresh Token Redis CRUD 서비스
 *
 * Redis를 사용하여 Refresh Token을 저장하고 관리한다.
 *
 * Redis 키 설계:
 * - 키 형식: "refresh_token:{userId}"
 * - 값: Refresh Token 문자열 (JWT)
 * - TTL: 7일 (Refresh Token 만료 시간과 동일하게 설정)
 *
 * TTL 기반 자동 만료:
 * - Redis TTL을 Refresh Token 만료 시간과 동일하게 설정
 * - 만료된 토큰은 Redis에서 자동 삭제되어 스토리지 낭비 방지
 * - 명시적 삭제(로그아웃)도 지원
 *
 * Refresh Token Rotation 지원:
 * - saveRefreshToken: 새 토큰으로 덮어쓰기 (이전 토큰 자동 무효화)
 * - getRefreshToken: 저장된 토큰 조회 (불일치 시 재사용 공격 감지)
 * - deleteRefreshToken: 로그아웃 시 명시적 삭제
 */
@Service
class TokenService(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${jwt.refresh-token-expiration:604800000}")
    private val refreshTokenExpiration: Long,   // Refresh Token TTL (밀리초, 기본 7일)
) {

    private val log = LoggerFactory.getLogger(TokenService::class.java)

    companion object {
        /**
         * Redis 키 접두사
         * 다른 용도의 키와 충돌을 방지하기 위해 네임스페이스 접두사를 사용한다.
         */
        private const val REFRESH_TOKEN_PREFIX = "refresh_token:"
    }

    /**
     * Refresh Token을 Redis에 저장
     *
     * 기존에 저장된 토큰이 있으면 덮어쓴다 (Refresh Token Rotation).
     * TTL은 Refresh Token 만료 시간과 동일하게 설정한다.
     *
     * @param userId 사용자 ID (키의 일부)
     * @param refreshToken 저장할 Refresh Token 값
     */
    fun saveRefreshToken(userId: String, refreshToken: String) {
        val key = buildKey(userId)

        // Redis에 Refresh Token 저장 (TTL: refreshTokenExpiration ms → 초 변환)
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            refreshTokenExpiration,
            TimeUnit.MILLISECONDS,  // 밀리초 단위로 TTL 설정
        )

        log.debug("Refresh Token 저장 완료: key={}, ttl={}ms", key, refreshTokenExpiration)
    }

    /**
     * Redis에서 Refresh Token 조회
     *
     * 사용자 ID로 저장된 Refresh Token을 조회한다.
     * 토큰이 없거나 TTL이 만료된 경우 null을 반환한다.
     *
     * @param userId 사용자 ID
     * @return 저장된 Refresh Token, 없으면 null
     */
    fun getRefreshToken(userId: String): String? {
        val key = buildKey(userId)
        val token = redisTemplate.opsForValue().get(key)

        if (token == null) {
            log.debug("Refresh Token 없음 또는 만료: key={}", key)
        }

        return token
    }

    /**
     * Redis에서 Refresh Token 삭제
     *
     * 로그아웃 또는 Refresh Token 재사용 공격 감지 시 호출된다.
     * 삭제 후에는 해당 사용자의 모든 토큰 갱신이 불가능해진다.
     *
     * @param userId 사용자 ID
     */
    fun deleteRefreshToken(userId: String) {
        val key = buildKey(userId)
        val deleted = redisTemplate.delete(key)

        if (deleted) {
            log.info("Refresh Token 삭제 완료: key={}", key)
        } else {
            // 이미 삭제된 경우 (멱등성: 오류 없이 무시)
            log.debug("삭제할 Refresh Token 없음 (이미 삭제됨): key={}", key)
        }
    }

    /**
     * Refresh Token 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 토큰이 Redis에 존재하면 true
     */
    fun existsRefreshToken(userId: String): Boolean {
        val key = buildKey(userId)
        return redisTemplate.hasKey(key)
    }

    /**
     * Refresh Token의 남은 TTL 조회 (초 단위)
     *
     * 디버깅 또는 모니터링 목적으로 사용한다.
     *
     * @param userId 사용자 ID
     * @return 남은 TTL (초), 키가 없으면 -2, TTL이 없으면 -1
     */
    fun getRemainingTtl(userId: String): Long {
        val key = buildKey(userId)
        return redisTemplate.getExpire(key, TimeUnit.SECONDS)
    }

    /**
     * Redis 키 생성
     *
     * 키 형식: "refresh_token:{userId}"
     * 예시: "refresh_token:550e8400-e29b-41d4-a716-446655440000"
     *
     * @param userId 사용자 ID
     * @return Redis 키 문자열
     */
    private fun buildKey(userId: String): String = "$REFRESH_TOKEN_PREFIX$userId"
}
