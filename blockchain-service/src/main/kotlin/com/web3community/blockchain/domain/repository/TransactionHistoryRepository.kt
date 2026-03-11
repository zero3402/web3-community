package com.web3community.blockchain.domain.repository

import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.domain.document.TransactionHistory
import com.web3community.blockchain.domain.document.TransactionStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * 트랜잭션 히스토리 MongoDB 리포지토리
 *
 * ## 개요
 * [TransactionHistory] 도큐먼트에 대한 CRUD 및 비즈니스 쿼리를 제공하는 리액티브 리포지토리.
 * 트랜잭션 이력 조회, 상태 모니터링, 중복 트랜잭션 감지 등에 활용된다.
 *
 * ## 인덱스 활용
 * - `findByWalletIdOrderByCreatedAtDesc`: `idx_tx_walletId_createdAt` 복합 인덱스
 * - `findByUserIdAndChainOrderByCreatedAtDesc`: `idx_tx_userId_chain` 복합 인덱스
 * - `findByStatusAndCreatedAtBefore`: `idx_tx_status_createdAt` 복합 인덱스
 * - `findByTxHash`: `txHash` 단일 인덱스
 *
 * @see TransactionHistory
 * @see TransactionStatus
 */
@Repository
interface TransactionHistoryRepository : ReactiveMongoRepository<TransactionHistory, String> {

    /**
     * 지갑 ID 기준 트랜잭션 히스토리 최신순 조회 (페이징)
     *
     * 특정 지갑의 출입금 내역을 최신순으로 페이징하여 반환한다.
     * [Pageable]을 통해 페이지 크기, 정렬, 오프셋을 제어할 수 있다.
     *
     * @param walletId 지갑 ID ([com.web3community.blockchain.domain.document.Wallet.id])
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 최신순 정렬된 [TransactionHistory] [Flux]
     */
    fun findByWalletIdOrderByCreatedAtDesc(walletId: String, pageable: Pageable): Flux<TransactionHistory>

    /**
     * 사용자 ID와 체인 기준 트랜잭션 히스토리 최신순 조회 (페이징)
     *
     * 사용자의 특정 체인(BTC 또는 ETH) 트랜잭션 내역을 최신순으로 반환한다.
     *
     * @param userId 사용자 ID
     * @param chain 블록체인 네트워크 타입 [Chain]
     * @param pageable 페이징 정보
     * @return 최신순 정렬된 [TransactionHistory] [Flux]
     */
    fun findByUserIdAndChainOrderByCreatedAtDesc(
        userId: String,
        chain: Chain,
        pageable: Pageable
    ): Flux<TransactionHistory>

    /**
     * 특정 상태 및 생성 시각 이전의 트랜잭션 조회
     *
     * 주로 PENDING 상태이면서 일정 시간이 지난 트랜잭션을 찾아
     * 확인 수 체크 또는 타임아웃 처리하는 스케줄러에서 사용된다.
     *
     * @param status 조회할 트랜잭션 상태 [TransactionStatus]
     * @param cutoff 이 시각 이전에 생성된 트랜잭션만 반환
     * @return 조건에 맞는 [TransactionHistory] [Flux]
     */
    fun findByStatusAndCreatedAtBefore(
        status: TransactionStatus,
        cutoff: LocalDateTime
    ): Flux<TransactionHistory>

    /**
     * 트랜잭션 해시로 단건 조회
     *
     * 블록체인 노드에서 txHash 기반으로 트랜잭션을 조회한 후,
     * 해당 txHash를 가진 내부 기록을 찾아 상태를 업데이트할 때 사용한다.
     * 중복 트랜잭션 감지에도 활용된다.
     *
     * @param txHash 블록체인 트랜잭션 해시 (BTC: 64자 hex, ETH: 0x + 64자 hex)
     * @return 조회된 [TransactionHistory] 또는 빈 [Mono]
     */
    fun findByTxHash(txHash: String): Mono<TransactionHistory>
}
