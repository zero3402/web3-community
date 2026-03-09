package com.web3community.common.security

import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * CryptoUtils - AES-256-GCM 암호화/복호화 유틸리티
 *
 * 블록체인 지갑의 프라이빗 키를 안전하게 저장하기 위한 암호화 컴포넌트입니다.
 *
 * 알고리즘 선택 이유:
 * - AES-256: 미국 국가안보국(NSA)도 사용하는 군사급 대칭 암호화
 * - GCM 모드: 암호화 + 무결성 인증(AEAD)을 동시에 제공
 *   (기존 CBC 모드 대비 패딩 오라클 공격 방지)
 *
 * 보안 설계:
 * 1. 마스터 키 분리: 서버 환경변수(Kubernetes Secret)에서 마스터 키 주입
 * 2. 사용자별 파생 키: PBKDF2-HMAC-SHA256 + userId를 솔트로 사용하여
 *    사용자마다 다른 암호화 키 생성 (키 하나 유출 시 전체 영향 최소화)
 * 3. 128bit IV: 매 암호화 시 랜덤 IV 생성 (동일 키로 암호화해도 결과 다름)
 * 4. AAD(Additional Authentication Data): userId를 AAD로 설정하여
 *    다른 사용자의 데이터로 복호화 시도 시 인증 실패 (사용자 격리)
 * 5. 출력 형식: Base64(암호화된 데이터):Base64(IV) 형태로 저장
 *
 * 공격 시나리오 및 방어:
 * - DB 탈취: 암호화된 상태로 저장되므로 마스터 키 없이는 복호화 불가
 * - 마스터 키 유출: userId 솔트로 파생된 키이므로 모든 키가 다름
 * - 다른 사용자 키 복호화: AAD로 인해 인증 태그 검증 실패
 * - 재생 공격: 랜덤 IV로 인해 동일 입력도 매번 다른 암호문 생성
 */
@Component
class CryptoUtils {

    private val log = LoggerFactory.getLogger(CryptoUtils::class.java)

    companion object {
        /** AES 알고리즘 + GCM 모드 + No Padding (GCM은 스트림 암호화이므로 패딩 불필요) */
        private const val ALGORITHM = "AES/GCM/NoPadding"

        /** GCM 인증 태그 길이 (128bit = 최대 보안) */
        private const val GCM_TAG_LENGTH_BITS = 128

        /** IV(초기화 벡터) 길이 (128bit = 16바이트, GCM 표준 권장) */
        private const val IV_LENGTH_BYTES = 16

        /** 파생 키 길이 (256bit = 32바이트, AES-256) */
        private const val KEY_LENGTH_BITS = 256

        /** PBKDF2 반복 횟수 (많을수록 브루트포스 어려움, 개발환경 기준) */
        private const val PBKDF2_ITERATIONS = 10_000

        /** 출력 형식 구분자 */
        private const val SEPARATOR = ":"
    }

    /**
     * 평문 암호화 (블록체인 프라이빗 키 저장 시 사용)
     *
     * 암호화 과정:
     * 1. PBKDF2(masterKey + userId)로 사용자별 AES 키 파생
     * 2. 128bit 랜덤 IV 생성
     * 3. AES-256-GCM으로 암호화 (AAD: userId 바이트)
     * 4. "Base64(암호문):Base64(IV)" 형태로 반환
     *
     * @param plainText 암호화할 평문 (블록체인 프라이빗 키 등)
     * @param masterKey 서버 마스터 암호화 키 (환경변수에서 주입)
     * @param userId 사용자 ID (키 파생 솔트 및 AAD로 사용)
     * @return "Base64(암호문):Base64(IV)" 형태의 암호화된 문자열
     * @throws BusinessException BLOCKCHAIN_011 암호화 실패 시
     */
    fun encrypt(plainText: String, masterKey: String, userId: Long): String {
        return try {
            // 1. userId를 솔트로 사용하여 사용자별 파생 키 생성
            val derivedKey = deriveKey(masterKey, userId)

            // 2. 128bit 랜덤 IV 생성 (매 암호화마다 새로 생성 - 재생 공격 방지)
            val iv = ByteArray(IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)

            // 3. GCM 파라미터 설정
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

            // 4. 암호화 Cipher 초기화
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, derivedKey, gcmSpec)

            // 5. AAD 설정: userId를 추가 인증 데이터로 설정
            //    이를 통해 다른 userId로 복호화 시도 시 GCM 인증 태그 검증 실패
            cipher.updateAAD(userId.toString().toByteArray(Charsets.UTF_8))

            // 6. 암호화 수행
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 7. Base64 인코딩하여 저장 가능한 문자열로 변환
            //    형식: "Base64(암호문):Base64(IV)"
            val encoder = Base64.getEncoder()
            "${encoder.encodeToString(encryptedBytes)}$SEPARATOR${encoder.encodeToString(iv)}"

        } catch (e: Exception) {
            log.error("암호화 실패: userId={}", userId, e)
            throw BusinessException(ErrorCode.BLOCKCHAIN_011, cause = e)
        }
    }

    /**
     * 암호문 복호화 (블록체인 트랜잭션 서명 시 프라이빗 키 사용)
     *
     * 복호화 과정:
     * 1. 저장된 문자열에서 암호문과 IV 분리
     * 2. PBKDF2(masterKey + userId)로 동일한 AES 키 재파생
     * 3. AES-256-GCM으로 복호화 (AAD: userId 바이트)
     * 4. GCM 인증 태그 자동 검증 (userId 불일치 시 자동 실패)
     *
     * @param encryptedText "Base64(암호문):Base64(IV)" 형태의 암호화된 문자열
     * @param masterKey 서버 마스터 암호화 키
     * @param userId 복호화를 요청하는 사용자 ID (AAD 검증에 사용)
     * @return 복호화된 평문 (블록체인 프라이빗 키)
     * @throws BusinessException BLOCKCHAIN_012 복호화 실패 시 (키 불일치, 데이터 위변조 등)
     */
    fun decrypt(encryptedText: String, masterKey: String, userId: Long): String {
        return try {
            // 1. 암호문과 IV 분리
            val parts = encryptedText.split(SEPARATOR)
            require(parts.size == 2) { "암호화 데이터 형식이 올바르지 않습니다." }

            val decoder = Base64.getDecoder()
            val encryptedBytes = decoder.decode(parts[0])
            val iv = decoder.decode(parts[1])

            // 2. 동일한 마스터 키 + userId로 동일한 파생 키 재생성
            val derivedKey = deriveKey(masterKey, userId)

            // 3. GCM 파라미터 설정 (저장된 IV 사용)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

            // 4. 복호화 Cipher 초기화
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, derivedKey, gcmSpec)

            // 5. AAD 설정 (암호화 시와 동일해야 GCM 인증 태그 검증 통과)
            //    userId가 다르면 AEADBadTagException 발생 → 사용자 격리 보장
            cipher.updateAAD(userId.toString().toByteArray(Charsets.UTF_8))

            // 6. 복호화 수행 (GCM 인증 태그 자동 검증 포함)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)

        } catch (e: javax.crypto.AEADBadTagException) {
            // GCM 인증 태그 검증 실패 - 데이터 위변조 또는 잘못된 userId
            log.error("GCM 인증 태그 검증 실패: userId={} - 데이터 위변조 또는 잘못된 사용자 접근", userId)
            throw BusinessException(ErrorCode.BLOCKCHAIN_012, cause = e)
        } catch (e: Exception) {
            log.error("복호화 실패: userId={}", userId, e)
            throw BusinessException(ErrorCode.BLOCKCHAIN_012, cause = e)
        }
    }

    /**
     * PBKDF2-HMAC-SHA256으로 사용자별 AES 파생 키 생성 (내부 사용)
     *
     * 왜 PBKDF2를 사용하는가?
     * - 마스터 키를 직접 AES 키로 사용하면 모든 사용자가 동일한 키를 사용
     * - userId를 솔트로 PBKDF2 적용 시 사용자마다 다른 암호화 키 파생
     * - 브루트포스 공격 대비를 위해 반복 횟수(PBKDF2_ITERATIONS) 설정
     *
     * @param masterKey 마스터 암호화 키 (환경변수에서 주입)
     * @param userId 키 파생에 사용할 솔트 (사용자 고유 식별자)
     * @return 256bit AES 비밀 키
     */
    private fun deriveKey(masterKey: String, userId: Long): SecretKeySpec {
        // userId를 솔트로 변환 (각 사용자마다 다른 키 생성)
        val salt = "web3community-user-$userId".toByteArray(Charsets.UTF_8)

        // PBKDF2WithHmacSHA256: 패스워드 기반 키 파생 함수
        val spec = PBEKeySpec(
            masterKey.toCharArray(), // 마스터 키 (비밀번호 역할)
            salt,                    // 솔트 (userId 기반)
            PBKDF2_ITERATIONS,       // 반복 횟수 (높을수록 안전)
            KEY_LENGTH_BITS          // 출력 키 길이 (256bit)
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded

        return SecretKeySpec(keyBytes, "AES")
    }
}
