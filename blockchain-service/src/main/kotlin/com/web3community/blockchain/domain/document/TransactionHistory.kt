package com.web3community.blockchain.domain.document

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 트랜잭션 히스토리 MongoDB Document
 *
 * ## 개요
 * BTC/ETH 트랜잭션의 발생부터 확인까지 전체 이력을 기록한다.
 * 트랜잭션 상태는 브로드캐스트 후 블록 확인 수에 따라 업데이트된다.
 *
 * ## 트랜잭션 상태 전이
 * ```
 * [출금 요청 수신]
 *     ↓ 서명 및 브로드캐스트
 * PENDING (txHash 설정, blockNumber = null)
 *     ↓ 1개 블록 확인
 * PENDING → CONFIRMED 진행 중 (confirmations 증가)
 *     ↓ 충분한 확인 수 달성 (BTC: 6, ETH: 12 권장)
 * CONFIRMED
 *     또는
 *     ↓ 노드 거절 / 잔액 부족 / Nonce 충돌
 * FAILED
 * ```
 *
 * ## 금액 단위
 * - BTC: satoshi 단위를 BigDecimal로 표현 (소수점 8자리)
 *   예: 0.001 BTC = "0.00100000"
 * - ETH: wei 단위를 BigDecimal로 표현 (소수점 18자리)
 *   예: 0.01 ETH = "10000000000000000" wei
 * - ERC20: 토큰 컨트랙트의 decimals에 따라 다름
 *
 * @property id MongoDB ObjectId
 * @property walletId 출금 지갑 ID ([Wallet.id])
 * @property userId 트랜잭션 요청자 사용자 ID
 * @property chain 블록체인 네트워크 타입 [Chain]
 * @property type 트랜잭션 유형 (입금/출금) [TransactionType]
 * @property fromAddress 송신 주소
 * @property toAddress 수신 주소
 * @property amount 전송 금액 (BTC: satoshi, ETH: wei 단위)
 * @property fee 트랜잭션 수수료 (BTC: satoshi, ETH: wei 단위)
 * @property txHash 브로드캐스트된 트랜잭션 해시
 * @property tokenAddress ERC20 컨트랙트 주소 (ETH 네이티브 전송 시 null)
 * @property status 트랜잭션 상태 [TransactionStatus]
 * @property blockNumber 포함된 블록 번호 (PENDING 중에는 null)
 * @property confirmations 현재 확인 수
 * @property errorMessage 실패 시 오류 메시지
 * @property createdAt 도큐먼트 생성 시각
 * @property updatedAt 마지막 수정 시각
 */
@Document(collection = "transaction_histories")
@CompoundIndexes(
    // walletId + createdAt 복합 인덱스: 지갑별 트랜잭션 히스토리 시간순 조회
    CompoundIndex(
        name = "idx_tx_walletId_createdAt",
        def = "{'walletId': 1, 'createdAt': -1}"  // -1: 최신순 정렬 최적화
    ),
    // userId + chain 복합 인덱스: 사용자의 특정 체인 트랜잭션 조회
    CompoundIndex(
        name = "idx_tx_userId_chain",
        def = "{'userId': 1, 'chain': 1, 'createdAt': -1}"
    ),
    // status + createdAt 복합 인덱스: PENDING 트랜잭션 모니터링 (컨펌 체크 스케줄러)
    CompoundIndex(
        name = "idx_tx_status_createdAt",
        def = "{'status': 1, 'createdAt': -1}"
    )
)
data class TransactionHistory(

    /** MongoDB 기본 키 */
    @Id
    val id: String? = null,

    /** 송신 지갑 ID (Wallet.id 참조) */
    @Field("walletId")
    val walletId: String,

    /** 트랜잭션 요청자 사용자 ID */
    @Field("userId")
    val userId: String,

    /** 블록체인 네트워크 타입 */
    @Field("chain")
    val chain: Chain,

    /** 트랜잭션 유형 (출금/입금) */
    @Field("type")
    val type: TransactionType,

    /** 송신 주소 */
    @Field("fromAddress")
    val fromAddress: String,

    /** 수신 주소 */
    @Field("toAddress")
    val toAddress: String,

    /**
     * 전송 금액
     * BTC: satoshi (Long을 BigDecimal로 변환), ETH: wei
     * BigDecimal 사용: 소수점 정밀도 보장, 금액 계산 오버플로 방지
     */
    @Field("amount")
    val amount: BigDecimal,

    /**
     * 트랜잭션 수수료
     * BTC: satoshi 단위 수수료 (입력 UTXO - 출력 UTXO = 수수료)
     * ETH: gasUsed * gasPrice (wei 단위)
     */
    @Field("fee")
    val fee: BigDecimal,

    /**
     * 트랜잭션 해시
     * 브로드캐스트 전에는 null, 브로드캐스트 성공 후 설정
     * BTC: 64자 hex 문자열 / ETH: 0x + 64자 hex 문자열
     */
    @Indexed  // txHash 단일 조회 최적화 (트랜잭션 상태 확인 시 사용)
    @Field("txHash")
    val txHash: String? = null,

    /**
     * ERC20 토큰 컨트랙트 주소
     * ETH 네이티브 전송: null
     * ERC20 토큰 전송: 컨트랙트 주소 (0x...)
     */
    @Field("tokenAddress")
    val tokenAddress: String? = null,

    /** 트랜잭션 처리 상태 */
    @Field("status")
    val status: TransactionStatus = TransactionStatus.PENDING,

    /**
     * 포함된 블록 번호
     * PENDING 상태: null (아직 블록에 포함되지 않음)
     * CONFIRMED/FAILED 상태: 블록 번호 설정
     */
    @Field("blockNumber")
    val blockNumber: Long? = null,

    /**
     * 현재 확인 수
     * 0: 미확인(mempool) / 1: 1개 블록 / ...
     * BTC: 6 이상 = 불변으로 간주
     * ETH: 12 이상 = 불변으로 간주
     */
    @Field("confirmations")
    val confirmations: Int = 0,

    /**
     * 실패 사유 메시지
     * FAILED 상태일 때만 설정 (노드 오류 메시지, 잔액 부족 등)
     */
    @Field("errorMessage")
    val errorMessage: String? = null,

    /** 도큐먼트 생성 시각 */
    @CreatedDate
    @Field("createdAt")
    val createdAt: LocalDateTime? = null,

    /** 마지막 수정 시각 */
    @LastModifiedDate
    @Field("updatedAt")
    val updatedAt: LocalDateTime? = null
)

/**
 * 트랜잭션 유형
 *
 * @property SEND 출금 (사용자가 요청한 외부 전송)
 * @property RECEIVE 입금 (외부에서 수신된 트랜잭션)
 */
enum class TransactionType {
    SEND,    // 출금: 서비스 지갑 → 외부 주소
    RECEIVE  // 입금: 외부 주소 → 서비스 지갑 (블록체인 스캔으로 감지)
}

/**
 * 트랜잭션 처리 상태
 *
 * @property PENDING 브로드캐스트 완료, 블록 확인 대기 중
 * @property CONFIRMED 충분한 블록 확인 완료 (BTC: 6, ETH: 12)
 * @property FAILED 처리 실패 (잔액 부족, 노드 거절, 타임아웃 등)
 */
enum class TransactionStatus {
    PENDING,    // 처리 중 (mempool 또는 블록 확인 대기)
    CONFIRMED,  // 확인 완료
    FAILED      // 실패
}
