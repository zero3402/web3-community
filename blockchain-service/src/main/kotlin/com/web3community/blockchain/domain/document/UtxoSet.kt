package com.web3community.blockchain.domain.document

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

/**
 * BTC UTXO(Unspent Transaction Output) Set MongoDB Document
 *
 * ## UTXO 개념
 * Bitcoin은 계좌 잔액 대신 UTXO 집합으로 잔액을 표현한다.
 * 잔액 = AVAILABLE 상태인 모든 UTXO의 amount 합산
 *
 * ## UTXO 생명주기
 * ```
 * Bitcoin 노드 listunspent
 *     ↓ UtxoManager.syncUtxos()
 * AVAILABLE (사용 가능)
 *     ↓ UtxoSelector.select() + UtxoLockManager.lock()
 * RESERVED (출금 처리 중, Redis 락 보유)
 *     ↓ BtcTransactionService.broadcastAndConfirm()
 * SPENT (소비 완료, 논리적 삭제)
 * ```
 *
 * ## 이중 지불 방지 전략
 * 1. Redis 분산 락 (UtxoLockManager): 동시 출금 요청 시 같은 UTXO 선택 방지
 * 2. RESERVED 상태: DB 수준의 소프트 락
 * 3. 트랜잭션 브로드캐스트 성공 후 SPENT 처리
 *
 * ## 인덱스 전략
 * - walletId + status 복합 인덱스: AVAILABLE UTXO 조회 (잔액 계산, UTXO 선택)
 * - txid + vout 복합 유니크 인덱스: UTXO 고유성 보장 (txid:vout이 UTXO의 전역 식별자)
 *
 * @property id MongoDB ObjectId
 * @property walletId 소속 지갑 ID ([Wallet.id])
 * @property address Bitcoin 수신 주소 (이 UTXO를 잠그는 주소)
 * @property txid UTXO를 생성한 트랜잭션 해시 (32바이트 hex)
 * @property vout 트랜잭션 내 출력 인덱스 (0부터 시작)
 * @property amount UTXO 금액 (satoshi 단위, 1 BTC = 100,000,000 satoshi)
 * @property scriptPubKey 잠금 스크립트 (hex 인코딩)
 * @property confirmations 현재 확인 수 (6 이상이면 안전)
 * @property status UTXO 상태 [UtxoStatus]
 * @property lockedBy 이 UTXO를 예약한 트랜잭션 요청 ID (RESERVED 시 설정)
 * @property lockedAt RESERVED 상태로 변경된 시각
 * @property createdAt 도큐먼트 생성 시각
 * @property updatedAt 마지막 수정 시각
 */
@Document(collection = "utxo_sets")
@CompoundIndexes(
    // walletId + status 복합 인덱스: findByWalletIdAndStatus() 쿼리 최적화
    // 잔액 계산: db.utxo_sets.find({walletId: X, status: "AVAILABLE"})
    CompoundIndex(
        name = "idx_utxo_walletId_status",
        def = "{'walletId': 1, 'status': 1}"
    ),
    // txid + vout 복합 유니크 인덱스: UTXO 식별자 고유성 보장
    // Bitcoin 프로토콜에서 txid:vout은 전 네트워크에서 유일
    CompoundIndex(
        name = "idx_utxo_txid_vout_unique",
        def = "{'txid': 1, 'vout': 1}",
        unique = true
    )
)
data class UtxoSet(

    /** MongoDB 기본 키 */
    @Id
    val id: String? = null,

    /**
     * 소속 지갑 ID
     * Wallet.id와 연결되는 참조 키 (MongoDB는 FK 없으므로 애플리케이션 수준 관리)
     */
    @Field("walletId")
    val walletId: String,

    /**
     * Bitcoin 수신 주소
     * 이 UTXO를 잠그는(lock) 주소. 해당 주소의 개인키로만 서명 가능.
     * 주소 형식: P2PKH(1...), P2SH-P2WPKH(3...), Bech32(bc1q...)
     */
    @Field("address")
    val address: String,

    /**
     * 트랜잭션 해시 (Transaction ID)
     * 이 UTXO를 생성한 트랜잭션의 고유 식별자 (64자 hex 문자열)
     * 예: "a1b2c3d4e5f6...0a1b2c3d4e5f6"
     */
    @Field("txid")
    val txid: String,

    /**
     * 트랜잭션 출력 인덱스 (vout)
     * 동일 트랜잭션에서 여러 출력이 있을 때 구분하는 인덱스 (0부터 시작)
     * txid + vout 조합이 UTXO의 전역 식별자
     */
    @Field("vout")
    val vout: Int,

    /**
     * UTXO 금액 (satoshi 단위)
     * 1 BTC = 100,000,000 satoshi
     * Long 타입 사용: satoshi는 정수이며 최대 21,000,000 BTC * 10^8 = 2.1 * 10^15 (Long 범위 내)
     */
    @Field("amount")
    val amount: Long,  // satoshi 단위

    /**
     * 잠금 스크립트 (locking script / scriptPubKey)
     * UTXO를 사용하기 위한 조건을 정의하는 스크립트 (hex 인코딩)
     * P2WPKH 예시: "0014{20바이트 pubKeyHash}"
     */
    @Field("scriptPubKey")
    val scriptPubKey: String,

    /**
     * 확인 수 (confirmations)
     * 이 UTXO가 포함된 블록 이후 추가된 블록 수
     * 일반적으로 6 이상이면 불변으로 간주 (51% 공격 방지)
     * 0: 미확인(mempool), 1: 1개 블록 확인, ...
     */
    @Field("confirmations")
    val confirmations: Int = 0,

    /**
     * UTXO 상태
     * AVAILABLE: 사용 가능 / RESERVED: 출금 처리 중 / SPENT: 소비 완료
     */
    @Field("status")
    val status: UtxoStatus = UtxoStatus.AVAILABLE,

    /**
     * 이 UTXO를 예약(RESERVED)한 트랜잭션 요청 ID
     * RESERVED 상태일 때만 설정되며, 어떤 출금 요청이 이 UTXO를 사용하는지 추적
     * AVAILABLE/SPENT 상태에서는 null
     */
    @Field("lockedBy")
    val lockedBy: String? = null,

    /**
     * RESERVED 상태로 변경된 시각
     * TTL 기반 락 만료 감지에 사용 (Redis 락과 별개로 DB 수준 타임아웃 처리)
     */
    @Field("lockedAt")
    val lockedAt: LocalDateTime? = null,

    /** 도큐먼트 생성 시각 (MongoDB Auditing 자동 설정) */
    @CreatedDate
    @Field("createdAt")
    val createdAt: LocalDateTime? = null,

    /** 마지막 수정 시각 (MongoDB Auditing 자동 설정) */
    @LastModifiedDate
    @Field("updatedAt")
    val updatedAt: LocalDateTime? = null
)

/**
 * UTXO 상태 열거형
 *
 * @property AVAILABLE 출금에 사용 가능한 상태 (잔액 계산에 포함)
 * @property RESERVED 출금 처리 중 예약된 상태 (잔액 계산에서 제외, 다른 출금에 사용 불가)
 * @property SPENT 이미 소비된 상태 (트랜잭션 브로드캐스트 완료 후 설정)
 */
enum class UtxoStatus {
    AVAILABLE,  // 사용 가능 (잔액 계산 포함)
    RESERVED,   // 출금 처리 중 (락 보유)
    SPENT       // 소비 완료 (논리적 삭제)
}
