package com.web3community.blockchain.dto

import com.web3community.blockchain.domain.document.TransactionHistory
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 트랜잭션 조회/생성 응답 DTO
 *
 * ## 개요
 * [TransactionHistory] 도큐먼트를 API 응답으로 변환한 DTO.
 * 트랜잭션 전송 결과 및 히스토리 조회에 모두 사용된다.
 *
 * ## 응답 예시
 * ```json
 * {
 *   "id": "65a1b2c3d4e5f6...",
 *   "txHash": "0xabc123...",
 *   "chain": "ETH",
 *   "fromAddress": "0x1234...",
 *   "toAddress": "0x5678...",
 *   "amount": "1000000000000000",
 *   "fee": "21000000000000",
 *   "status": "PENDING",
 *   "createdAt": "2024-01-01T00:00:00"
 * }
 * ```
 *
 * @property id MongoDB 도큐먼트 ID
 * @property txHash 블록체인 트랜잭션 해시 (브로드캐스트 전 null 가능)
 * @property chain 블록체인 네트워크 타입 ("BTC" 또는 "ETH")
 * @property fromAddress 송신 주소
 * @property toAddress 수신 주소
 * @property amount 전송 금액 (BTC: satoshi, ETH: wei 단위)
 * @property fee 수수료 (BTC: satoshi, ETH: wei 단위)
 * @property status 트랜잭션 상태 ("PENDING", "CONFIRMED", "FAILED")
 * @property createdAt 트랜잭션 요청 시각
 */
data class TransactionResponse(
    /** MongoDB 도큐먼트 ID */
    val id: String,

    /**
     * 블록체인 트랜잭션 해시
     * PENDING 상태에서도 txHash가 설정되어 있으면 브로드캐스트된 상태이다.
     * null이면 아직 브로드캐스트 전 (예: 서명 실패, 네트워크 오류)
     */
    val txHash: String?,

    /** 블록체인 네트워크 타입 ("BTC" 또는 "ETH") */
    val chain: String,

    /** 송신 주소 */
    val fromAddress: String,

    /** 수신 주소 */
    val toAddress: String,

    /**
     * 전송 금액
     * - BTC: satoshi 단위 (예: "100000" = 0.001 BTC)
     * - ETH: wei 단위 (예: "1000000000000000" = 0.001 ETH)
     */
    val amount: BigDecimal,

    /**
     * 트랜잭션 수수료
     * - BTC: satoshi 단위 (입력 UTXO 합 - 출력 UTXO 합)
     * - ETH: gasUsed * gasPrice (wei 단위)
     */
    val fee: BigDecimal,

    /** 트랜잭션 상태 ("PENDING", "CONFIRMED", "FAILED") */
    val status: String,

    /** 트랜잭션 요청 생성 시각 */
    val createdAt: LocalDateTime?
) {
    companion object {

        /**
         * [TransactionHistory] 도큐먼트를 [TransactionResponse] DTO로 변환
         *
         * 클라이언트에게 전달할 트랜잭션 정보를 표준 응답 형식으로 변환한다.
         *
         * @param tx 변환할 [TransactionHistory] 도큐먼트
         * @return 변환된 [TransactionResponse]
         * @throws IllegalStateException tx.id가 null인 경우 (저장되지 않은 도큐먼트)
         */
        fun from(tx: TransactionHistory): TransactionResponse {
            return TransactionResponse(
                id = requireNotNull(tx.id) { "저장되지 않은 트랜잭션 도큐먼트입니다. (id=null)" },
                txHash = tx.txHash,
                chain = tx.chain.name,
                fromAddress = tx.fromAddress,
                toAddress = tx.toAddress,
                amount = tx.amount,
                fee = tx.fee,
                status = tx.status.name,
                createdAt = tx.createdAt
            )
        }
    }
}
