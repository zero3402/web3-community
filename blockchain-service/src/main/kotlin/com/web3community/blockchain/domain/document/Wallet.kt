package com.web3community.blockchain.domain.document

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

/**
 * 지갑 블록체인 계정 MongoDB Document
 *
 * ## 개요
 * 사용자의 BTC 또는 ETH 지갑 정보를 저장하는 MongoDB 도큐먼트.
 * 개인키는 절대 평문으로 저장되지 않으며, AES-256-GCM으로 암호화된 상태로만 저장된다.
 *
 * ## 보안 설계
 * ```
 * 원본 개인키 (memory only)
 *     ↓ AES-256-GCM 암호화 (마스터 키 + 랜덤 IV)
 * encryptedPrivateKey (MongoDB 저장)
 *     ↓ 복호화 필요 시 CryptoUtils.decrypt()
 * 원본 개인키 (메모리에서 즉시 사용 후 삭제)
 * ```
 *
 * ## HD 지갑 파생 경로 (BIP44)
 * ```
 * BTC: m / 44' / 0' / 0' / 0 / {index}
 * ETH: m / 44' / 60' / 0' / 0 / {index}
 * ```
 * - 44': BIP44 목적
 * - 0': BTC 코인 타입 / 60': ETH 코인 타입
 * - 0': 계정 인덱스
 * - 0: 외부 체인 (수신용)
 * - {index}: 주소 인덱스
 *
 * ## 인덱스 전략
 * - userId: 사용자 지갑 목록 조회 (단일 인덱스)
 * - address: 전역 고유 주소 (unique 인덱스, 이중 지불 방지)
 * - userId + chain: 특정 체인의 지갑 조회 (복합 인덱스)
 *
 * @property id MongoDB ObjectId (자동 생성)
 * @property userId 지갑 소유자의 사용자 ID (user-service의 userId)
 * @property chain 블록체인 네트워크 타입 [Chain]
 * @property address 공개 블록체인 주소 (BTC: Bech32, ETH: 0x 형식)
 * @property encryptedPrivateKey AES-256-GCM으로 암호화된 개인키 (Base64 인코딩)
 * @property derivationPath HD 지갑 파생 경로 (BIP44 표준)
 * @property status 지갑 상태 [WalletStatus]
 * @property createdAt 지갑 생성 시각 (자동 설정)
 * @property updatedAt 마지막 수정 시각 (자동 설정)
 */
@Document(collection = "wallets")  // MongoDB 컬렉션 이름 지정
@CompoundIndexes(
    // userId + chain 복합 인덱스: 사용자의 특정 체인 지갑 조회 최적화
    CompoundIndex(
        name = "idx_wallet_userId_chain",
        def = "{'userId': 1, 'chain': 1}"
    )
)
data class Wallet(

    /** MongoDB 기본 키 (ObjectId 자동 생성) */
    @Id
    val id: String? = null,

    /**
     * 지갑 소유자 사용자 ID
     * user-service의 User.id와 동일한 값 (서비스 간 ID 공유)
     * 단일 인덱스: 사용자의 모든 지갑 목록 조회 시 사용
     */
    @Indexed
    @Field("userId")
    val userId: String,

    /**
     * 블록체인 네트워크 타입
     * BTC: Bitcoin, ETH: Ethereum (ERC20 포함)
     */
    @Field("chain")
    val chain: Chain,

    /**
     * 블록체인 공개 주소
     * - BTC: Bech32 형식 (bc1q...) 또는 P2PKH (1...)
     * - ETH: 0x로 시작하는 20바이트 hex 주소 (체크섬 적용)
     * unique=true: 블록체인 특성상 주소는 전역적으로 유일해야 함
     */
    @Indexed(unique = true)
    @Field("address")
    val address: String,

    /**
     * AES-256-GCM으로 암호화된 개인키
     * 형식: Base64(IV + 암호문 + GCM 태그)
     * CryptoUtils.encrypt()로 암호화, CryptoUtils.decrypt()로 복호화
     *
     * ⚠️ 절대 로그에 출력하거나 API 응답에 포함하지 말 것
     */
    @Field("encryptedPrivateKey")
    val encryptedPrivateKey: String,

    /**
     * HD 지갑 파생 경로 (BIP44 표준)
     * BTC 예시: "m/44'/0'/0'/0/0"
     * ETH 예시: "m/44'/60'/0'/0/0"
     * 같은 Mnemonic으로 동일 주소 복구에 필요
     */
    @Field("derivationPath")
    val derivationPath: String,

    /**
     * 지갑 상태
     * ACTIVE: 정상 사용 가능 / DISABLED: 비활성화 (동결, 분실 신고 등)
     */
    @Field("status")
    val status: WalletStatus = WalletStatus.ACTIVE,

    /** 지갑 생성 시각 (MongoDB Auditing 자동 설정) */
    @CreatedDate
    @Field("createdAt")
    val createdAt: LocalDateTime? = null,

    /** 마지막 수정 시각 (MongoDB Auditing 자동 설정) */
    @LastModifiedDate
    @Field("updatedAt")
    val updatedAt: LocalDateTime? = null
)

/**
 * 지원하는 블록체인 네트워크 타입
 *
 * @property BTC Bitcoin (UTXO 기반, satoshi 단위)
 * @property ETH Ethereum (Account 기반, wei 단위, ERC20 포함)
 */
enum class Chain {
    BTC,  // Bitcoin: P2PKH / P2SH-P2WPKH / Bech32(P2WPKH) 주소 지원
    ETH   // Ethereum: ERC20 토큰도 같은 ETH 주소 사용
}

/**
 * 지갑 상태
 *
 * @property ACTIVE 정상 활성 상태 (출금, 입금 모두 가능)
 * @property DISABLED 비활성화 상태 (출금 불가, 입금은 블록체인 특성상 막을 수 없음)
 */
enum class WalletStatus {
    ACTIVE,    // 정상 사용 가능
    DISABLED   // 비활성화 (지갑 동결, 분실 신고, 사용자 탈퇴 등)
}
