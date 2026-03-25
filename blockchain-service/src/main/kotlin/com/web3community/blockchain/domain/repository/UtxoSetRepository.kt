package com.web3community.blockchain.domain.repository

import com.web3community.blockchain.domain.document.UtxoSet
import com.web3community.blockchain.domain.document.UtxoStatus
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * BTC UTXO Set MongoDB 리포지토리
 *
 * ## 개요
 * [UtxoSet] 도큐먼트에 대한 CRUD 및 비즈니스 쿼리를 제공하는 리액티브 리포지토리.
 * UTXO 선택(코인 선택), 잔액 계산, 이중 지불 방지 등 BTC 트랜잭션 처리의 핵심 쿼리를 담당한다.
 *
 * ## 인덱스 활용
 * - `findByWalletIdAndStatus`: `idx_utxo_walletId_status` 복합 인덱스 사용 (가장 빈번한 쿼리)
 * - `findByTxidAndVout`: `idx_utxo_txid_vout_unique` 복합 유니크 인덱스 사용
 *
 * ## UTXO 선택 알고리즘 (서비스 레이어에서 구현)
 * - Greedy 알고리즘: AVAILABLE UTXO를 금액 큰 순으로 정렬하여 필요한 금액 충족 시까지 선택
 * - 잔돈(change) 처리: 선택된 UTXO 합계 - 전송 금액 - 수수료 = 잔돈 출력
 *
 * @see UtxoSet
 * @see UtxoStatus
 */
@Repository
interface UtxoSetRepository : ReactiveMongoRepository<UtxoSet, String> {

    /**
     * 지갑 ID와 상태로 UTXO 목록 조회
     *
     * BTC 트랜잭션 생성 시 사용 가능한(AVAILABLE) UTXO 목록을 조회하거나,
     * 특정 상태(RESERVED, SPENT)의 UTXO를 조회할 때 사용한다.
     *
     * ### 주요 사용 사례
     * - `status = AVAILABLE`: 잔액 계산, UTXO 선택 (코인 선택 알고리즘)
     * - `status = RESERVED`: 처리 중인 UTXO 목록 조회 (감사, 모니터링)
     * - `status = SPENT`: 소비된 UTXO 정리 작업
     *
     * @param walletId 지갑 ID ([UtxoSet.walletId])
     * @param status UTXO 상태 필터 [UtxoStatus]
     * @return 조건에 맞는 [UtxoSet] [Flux]
     */
    fun findByWalletIdAndStatus(walletId: String, status: UtxoStatus): Flux<UtxoSet>

    /**
     * txid와 vout으로 UTXO 단건 조회
     *
     * Bitcoin 프로토콜에서 `txid:vout` 조합은 전 네트워크에서 UTXO의 전역 식별자이다.
     * 블록체인 노드에서 UTXO를 조회한 후 로컬 DB와 동기화할 때,
     * 또는 특정 UTXO의 상태를 업데이트할 때 사용된다.
     *
     * @param txid UTXO를 생성한 트랜잭션 해시 (64자 hex)
     * @param vout 트랜잭션 내 출력 인덱스 (0부터 시작)
     * @return 조회된 [UtxoSet] 또는 빈 [Mono]
     */
    fun findByTxidAndVout(txid: String, vout: Int): Mono<UtxoSet>

    /**
     * 특정 상태이고 lockedAt이 기준 시각 이전인 UTXO 목록 조회
     *
     * RESERVED 상태에서 lockedAt이 오래된 UTXO를 찾아 복구 처리에 사용.
     * BTC 트랜잭션 broadcast 후 오랫동안 CONFIRMED/FAILED 전환이 없는 경우
     * 자금이 동결되는 것을 방지하기 위해 주기적으로 실행된다.
     *
     * @param status 조회할 UTXO 상태
     * @param lockedAtBefore 이 시각 이전에 잠긴 UTXO만 조회
     * @return 조건에 맞는 [UtxoSet] [Flux]
     */
    fun findByStatusAndLockedAtBefore(status: UtxoStatus, lockedAtBefore: LocalDateTime): Flux<UtxoSet>
}
