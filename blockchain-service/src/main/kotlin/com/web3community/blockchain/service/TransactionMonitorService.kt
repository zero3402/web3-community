package com.web3community.blockchain.service

import com.web3community.blockchain.domain.document.TransactionStatus
import com.web3community.blockchain.domain.repository.TransactionHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigInteger
import java.time.LocalDateTime

/**
 * 블록체인 트랜잭션 상태 모니터링 서비스
 *
 * ## 개요
 * PENDING 상태의 트랜잭션을 주기적으로 확인하여 블록에 포함되었는지,
 * 실패했는지 상태를 업데이트하는 스케줄러 서비스.
 *
 * ## 모니터링 흐름
 * ```
 * @Scheduled(fixedDelay = 60000) checkPendingTransactions()
 *     ↓ 5분 이상 경과한 PENDING 트랜잭션 조회
 *     ↓ ETH: eth_getTransactionReceipt RPC 호출
 *     ↓ 확인 수 계산 (현재 블록 - 트랜잭션 블록)
 *     ↓ 12 이상이면 CONFIRMED, receipt 없으면 유지, 오류면 FAILED
 *     ↓ 상태 업데이트 및 저장
 * ```
 *
 * ## BTC 모니터링 (미구현)
 * BTC 트랜잭션 모니터링은 Bitcoin Core RPC 연동 후 구현 예정.
 * 현재는 ETH 트랜잭션 모니터링만 지원한다.
 *
 * ## 확인 수 기준
 * - ETH: 12 블록 이상이면 CONFIRMED (약 2.5분)
 * - BTC: 6 블록 이상이면 CONFIRMED (약 60분) - 향후 구현
 *
 * @param txHistoryRepository 트랜잭션 히스토리 리포지토리
 * @param web3j Ethereum JSON-RPC 클라이언트
 */
@Service
class TransactionMonitorService(
    private val txHistoryRepository: TransactionHistoryRepository,
    private val web3j: Web3j
) {

    private val logger = LoggerFactory.getLogger(TransactionMonitorService::class.java)

    /** ETH 트랜잭션 확정 기준 블록 수 */
    private val ethConfirmationThreshold = 12

    /** PENDING 트랜잭션 조회 기준 시간 (분) */
    private val pendingCheckMinutes = 5L

    /**
     * PENDING 트랜잭션 상태 주기적 확인
     *
     * 5분 이상 경과한 PENDING 상태 트랜잭션에 대해 블록체인 상태를 확인하고
     * CONFIRMED 또는 FAILED로 업데이트한다.
     *
     * 스케줄 주기: 이전 실행 완료 후 60초 대기 (fixedDelay)
     * - `fixedDelay`: 이전 실행이 완료된 후 지정 시간만큼 대기 후 재실행
     *   (블록체인 RPC 호출이 느릴 수 있으므로 fixedRate 대신 fixedDelay 사용)
     */
    @Scheduled(fixedDelay = 60_000)
    fun checkPendingTransactions() {
        logger.debug("[TransactionMonitorService] PENDING 트랜잭션 상태 확인 시작")

        val cutoffTime = LocalDateTime.now().minusMinutes(pendingCheckMinutes)

        txHistoryRepository.findByStatusAndCreatedAtBefore(TransactionStatus.PENDING, cutoffTime)
            .flatMap { tx ->
                when {
                    tx.txHash == null -> {
                        // txHash가 없으면 브로드캐스트 자체가 실패한 것으로 FAILED 처리
                        logger.warn("[TransactionMonitorService] txHash 없는 PENDING 트랜잭션 FAILED 처리: id={}", tx.id)
                        txHistoryRepository.save(tx.copy(status = TransactionStatus.FAILED, errorMessage = "txHash 없음: 브로드캐스트 실패"))
                    }
                    tx.chain.name == "ETH" -> checkEthTransaction(tx)
                    else -> {
                        // BTC 트랜잭션은 현재 모니터링 미지원 (향후 구현)
                        logger.debug("[TransactionMonitorService] BTC 트랜잭션 모니터링 스킵: id={}", tx.id)
                        Mono.just(tx)
                    }
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { tx -> logger.debug("[TransactionMonitorService] 트랜잭션 상태 업데이트 완료: id={}, status={}", tx.id, tx.status) },
                { error -> logger.error("[TransactionMonitorService] 모니터링 중 오류 발생", error) },
                { logger.debug("[TransactionMonitorService] PENDING 트랜잭션 상태 확인 완료") }
            )
    }

    // ─── 내부 구현 메서드 ────────────────────────────────────────────────────────

    /**
     * ETH 트랜잭션의 블록체인 확인 상태를 조회하고 업데이트한다. (내부 사용)
     *
     * `eth_getTransactionReceipt` RPC를 통해 트랜잭션의 블록 포함 여부를 확인한다.
     * 현재 블록 번호와 트랜잭션 블록 번호의 차이로 확인 수를 계산한다.
     *
     * @param tx PENDING 상태의 [com.web3community.blockchain.domain.document.TransactionHistory]
     * @return 상태가 업데이트된 트랜잭션 [Mono]
     */
    private fun checkEthTransaction(
        tx: com.web3community.blockchain.domain.document.TransactionHistory
    ): Mono<com.web3community.blockchain.domain.document.TransactionHistory> {
        val txHash = tx.txHash ?: return Mono.just(tx)

        return Mono.fromCallable {
            // eth_getTransactionReceipt: 트랜잭션이 블록에 포함된 경우에만 반환
            val receiptResponse = web3j.ethGetTransactionReceipt(txHash).send()
            val receipt = receiptResponse.transactionReceipt.orElse(null)

            if (receipt == null) {
                // 아직 블록에 포함되지 않음 (mempool에 있거나 dropped)
                logger.debug("[TransactionMonitorService] ETH 트랜잭션 미확인(mempool): txHash={}", txHash)
                null
            } else {
                val txBlockNumber = receipt.blockNumber ?: return@fromCallable null

                // 현재 블록 번호 조회
                val currentBlock = web3j.ethBlockNumber().send().blockNumber

                // 확인 수 = 현재 블록 - 트랜잭션 블록
                val confirmations = currentBlock.subtract(txBlockNumber).toInt()
                    .coerceAtLeast(0)

                // EVM 트랜잭션 성공 여부: status 1 = 성공, 0 = 실패(revert)
                val isSuccess = receipt.status == "0x1"

                logger.debug(
                    "[TransactionMonitorService] ETH 트랜잭션 확인: txHash={}, confirmations={}, success={}",
                    txHash, confirmations, isSuccess
                )

                Triple(confirmations, isSuccess, txBlockNumber)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { result ->
                if (result == null) {
                    Mono.just(tx)
                } else {
                    val (confirmations, isSuccess, txBlockNumber) = result

                    val newStatus = when {
                        !isSuccess -> TransactionStatus.FAILED
                        confirmations >= ethConfirmationThreshold -> TransactionStatus.CONFIRMED
                        else -> TransactionStatus.PENDING
                    }

                    val errorMessage = if (!isSuccess) "EVM 트랜잭션 실행 실패 (revert)" else null

                    val updatedTx = tx.copy(
                        status = newStatus,
                        confirmations = confirmations,
                        blockNumber = txBlockNumber.toLong(),
                        errorMessage = errorMessage
                    )

                    if (updatedTx.status != tx.status || updatedTx.confirmations != tx.confirmations) {
                        logger.info(
                            "[TransactionMonitorService] ETH 트랜잭션 상태 변경: txHash={}, {} → {}, confirmations={}",
                            txHash, tx.status, newStatus, confirmations
                        )
                        txHistoryRepository.save(updatedTx)
                    } else {
                        Mono.just(updatedTx)
                    }
                }
            }
            .onErrorResume { error ->
                logger.error(
                    "[TransactionMonitorService] ETH 트랜잭션 확인 실패: txHash={}", txHash, error
                )
                Mono.just(tx)
            }
    }
}
