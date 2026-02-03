package com.web3community.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * 비밀번호 암호화 유틸리티 클래스
 * BCrypt 알고리즘을 사용하여 비밀번호 해싱 및 검증
 * Spring Security의 PasswordEncoder를 구현
 */
@Component
class PasswordEncoderUtil {

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    /**
     * 비밀번호 암호화 (해싱)
     * BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 해싱
     * @param rawPassword 원본 비밀번호
     * @return 암호화된 비밀번호
     */
    fun encode(rawPassword: String): String {
        return passwordEncoder.encode(rawPassword)
    }

    /**
     * 비밀번호 검증
     * 원본 비밀번호와 해시된 비밀번호를 비교하여 일치 여부 확인
     * @param rawPassword 원본 비밀번호
     * @param encodedPassword 암호화된 비밀번호
     * @return 일치 여부
     */
    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }

    /**
     * 비밀번호 강도 검증
     * 최소 8자, 대소문자, 숫자, 특수문자 포함 여부 확인
     * @param password 검증할 비밀번호
     * @return 강도 검증 결과
     */
    fun validatePasswordStrength(password: String): PasswordStrengthResult {
        val requirements = mutableListOf<String>()
        
        // 길이 검증
        if (password.length < 8) {
            requirements.add("최소 8자 이상")
        }
        
        // 대문자 포함 여부
        if (!password.any { it.isUpperCase() }) {
            requirements.add("대문자 1개 이상")
        }
        
        // 소문자 포함 여부
        if (!password.any { it.isLowerCase() }) {
            requirements.add("소문자 1개 이상")
        }
        
        // 숫자 포함 여부
        if (!password.any { it.isDigit() }) {
            requirements.add("숫자 1개 이상")
        }
        
        // 특수문자 포함 여부
        if (!password.any { !it.isLetterOrDigit() }) {
            requirements.add("특수문자 1개 이상")
        }
        
        // 흔한 비밀번호 체크 (간단한 예시)
        val commonPasswords = setOf(
            "password", "123456", "123456789", "qwerty", "abc123",
            "password123", "admin", "letmein", "welcome", "monkey"
        )
        if (password.lowercase() in commonPasswords) {
            requirements.add("흔한 비밀번호 사용 금지")
        }
        
        val strength = when {
            password.length >= 12 && requirements.isEmpty() -> PasswordStrength.STRONG
            password.length >= 8 && requirements.size <= 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
        
        return PasswordStrengthResult(
            strength = strength,
            isValid = requirements.isEmpty(),
            requirements = requirements
        )
    }

    /**
     * 비밀번호 만료 여부 확인
     * 마지막 변경일로부터 특정 기간이 지났는지 확인
     * @param lastChangedAt 마지막 변경일 (epoch milliseconds)
     * @param maxAgeDays 최대 사용 기간 (일)
     * @return 만료 여부
     */
    fun isPasswordExpired(lastChangedAt: Long, maxAgeDays: Int = 90): Boolean {
        val currentTime = System.currentTimeMillis()
        val maxAgeMillis = maxAgeDays * 24 * 60 * 60 * 1000L
        return (currentTime - lastChangedAt) > maxAgeMillis
    }

    /**
     * 임시 비밀번호 생성
     * 랜덤한 12자리 임시 비밀번호 생성
     * @return 생성된 임시 비밀번호
     */
    fun generateTemporaryPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = java.security.SecureRandom()
        return (1..12)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * 비밀번호 리셋 토큰 생성
     * @return 리셋 토큰
     */
    fun generateResetToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = java.security.SecureRandom()
        return (1..32)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * 두 비밀번호가 유사한지 확인
     * Levenshtein 거리를 사용하여 비밀번호 유사도 계산
     * @param oldPassword 기존 비밀번호
     * @param newPassword 새 비밀번호
     * @return 유사한지 여부 (true: 유사함, false: 유사하지 않음)
     */
    fun arePasswordsSimilar(oldPassword: String, newPassword: String): Boolean {
        if (oldPassword.length < 4 || newPassword.length < 4) {
            return false
        }
        
        val distance = levenshteinDistance(oldPassword, newPassword)
        val similarity = 1.0 - (distance.toDouble() / maxOf(oldPassword.length, newPassword.length))
        
        // 70% 이상 유사하면 비슷한 것으로 간주
        return similarity > 0.7
    }

    /**
     * Levenshtein 거리 계산 (내부 유틸리티)
     * 두 문자열 간의 편집 거리를 계산
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
}

/**
 * 비밀번호 강도 enum
 */
enum class PasswordStrength {
    WEAK,      // 약함
    MEDIUM,    // 보통
    STRONG     // 강함
}

/**
 * 비밀번호 강도 검증 결과
 */
data class PasswordStrengthResult(
    val strength: PasswordStrength,
    val isValid: Boolean,
    val requirements: List<String>
) {
    /**
     * 강도에 따른 메시지 반환
     */
    fun getStrengthMessage(): String {
        return when (strength) {
            PasswordStrength.WEAK -> "비밀번호 강도: 약함"
            PasswordStrength.MEDIUM -> "비밀번호 강도: 보통"
            PasswordStrength.STRONG -> "비밀번호 강도: 강함"
        }
    }
    
    /**
     * 검증 실패 시 요구사항 메시지 반환
     */
    fun getRequirementsMessage(): String {
        return if (requirements.isNotEmpty()) {
            "요구사항: ${requirements.joinToString(", ")}"
        } else {
            "모든 요구사항을 충족합니다."
        }
    }
}