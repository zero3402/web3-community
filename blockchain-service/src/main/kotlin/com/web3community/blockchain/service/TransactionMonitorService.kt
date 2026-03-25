package com.web3community.blockchain.service

import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.domain.document.TransactionStatus
import com.web3community.blockchain.domain.document.UtxoStatus
import com.web3community.blockchain.domain.repository.TransactionHistoryRepository
import com.web3community.blockchain.domain.repository.UtxoSetRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.web3j.protocol.Web3j
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigInteger
import java.time.Duration
import java.time.LocalDateTime

/**
 * ETH 트랜잭션 상태 확인 결과 분류
 *
 * - [Confirmed]: receipt 존재 — 블록에 포함됨
 * - [InMempool]: receipt 없지만 eth_getTransactionByHash에서 발견 — 정상 PENDING
 * - [Dropped]: receipt 없고 mempool에도 없음 — drop 가능성 있음
 */
private sealed class EthTxCheckResult {
    data class Confirmed(
        val confirmations: Int,
        val isSuccess: Boolean,
        val blockNumber: BigInteger
    ) : EthTxCheckResult()
    object InMempool : EthTxCheckResult()
    object Dropped : EthTxCheckResult()
}

/**
 * 블록체인 트랜잭션 상태 모니터링 서비스
 *
 * ## 개요
 * PENDING 상태의 트랜잭션을 주기적으로 확인하여 블록에 포함되었는지,
 * 실패했는지 상태를 업데이트하는 스케줄러 서비스.
 * Mempool drop 감지 시 ETH RBF / BTC 재브로드캐스트로 자동 복구한다.
 *
 * ## 모니터링 흐름
 * ```
 * @Scheduled(fixedDelay = 60000) checkPendingTransactions()
 *     ↓ 5분 이상 경과한 PENDING 트랜잭션 조회
 *     ↓ ETH: eth_getTransactionReceipt → 없으면 eth_getTransactionByHash
 *         → 없으면(drop) 나이 확인 → RBF 재전송 또는 FAILED
 *     ↓ BTC: gettransaction → -1(미발견) → 나이 확인 → 재브로드캐스트 또는 FAILED
 *     ↓ 충분한 confirmation → CONFIRMED, UTXO SPENT 전환
 * ```
 *
 * ## 확인 수 기준
 * - ETH: 12 블록 이상이면 CONFIRMED (약 2.5분)
 * - BTC: 6 블록 이상이면 CONFIRMED (약 60분)
 *
 * @param txHistoryRepository 트랜잭션 히스토리 리포지토리
 * @param web3j Ethereum JSON-RPC 클라이언트
 * @param ethTransactionService ETH RBF 재전송 위임
 */
@Service
class TransactionMonitorService(
    private val txHistoryRepository: TransactionHistoryRepository,
    private val utxoRepository: UtxoSetRepository,
    private val web3j: Web3j,
    private val bitcoinRpcClient: BitcoinRpcClient,
    private val ethTransactionService: EthTransactionService
) {

    private val logger = LoggerFactory.getLogger(TransactionMonitorService::class.java)

    /** ETH 트랜잭션 확정 기준 블록 수 (약 2.5분) */
    private val ethConfirmationThreshold = 12

    /** BTC 트랜잭션 확정 기준 블록 수 (약 60분) */
    private val btcConfirmationThreshold = 6

    /** PENDING 트랜잭션 조회 기준 시간 (분) */
    private val pendingCheckMinutes = 5L

    /** RESERVED UTXO 복구 기준 시간 (분): 이 시간 이상 RESERVED면 복구 처리 */
    @Value("\${blockchain.utxo.stuck-recovery-minutes:30}")
    private var stuckUtxoRecoveryMinutes: Long = 30L

    /**
     * Mempool drop 판정 기준 시간 (분)
     * 이 시간이 경과했는데도 mempool에서 사라지면 drop으로 판정.
     * 네트워크 전파 지연과 구분하기 위해 충분한 여유를 줌.
     */
    @Value("\${blockchain.transaction.drop-detection-minutes:30}")
    private var dropDetectionMinutes: Long = 30L

    /**
     * RBF/재전송 최대 횟수
     * 이 횟수를 초과하면 FAILED로 처리하여 무한 재시도 방지.
     */
    @Value("\${blockchain.transaction.max-retry-count:3}")
    private var maxRetryCount: Int = 3

    /**
     * PENDING 트랜잭션 상태 주기적 확인
     *
     * 5분 이상 경과한 PENDING 상태 트랜잭션에 대해 블록체인 상태를 확인하고
     * CONFIRMED 또는 FAILED로 업데이트한다.
     *
     * ## fixedDelay와 blockLast()
     * subscribe()만 호출하면 메서드가 즉시 반환되므로 fixedDelay가 subscribe 시점부터 측정됨.
     * blockLast()로 reactive chain 완료까지 블록킹하면 실제 완료 후 60초 대기 보장.
     */
    @Scheduled(fixedDelay = 60_000)
    fun checkPendingTransactions() {
        logger.debug("[TransactionMonitorService] PENDING 트랜잭션 상태 확인 시작")

        val cutoffTime = LocalDateTime.now().minusMinutes(pendingCheckMinutes)

        txHistoryRepository.findByStatusAndCreatedAtBefore(TransactionStatus.PENDING, cutoffTime)
            .flatMap { tx ->
                when {
                    tx.txHash == null -> {
                        logger.warn("[TransactionMonitorService] txHash 없는 PENDING 트랜잭션 FAILED 처리: id={}", tx.id)
                        txHistoryRepository.save(tx.copy(status = TransactionStatus.FAILED, errorMessage = "txHash 없음: 브로드캐스트 실패"))
                    }
                    tx.chain == Chain.ETH -> checkEthTransaction(tx)
                    tx.chain == Chain.BTC -> checkBtcTransaction(tx)
                    else -> Mono.just(tx)
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext { tx -> logger.debug("[TransactionMonitorService] 상태 업데이트: id={}, status={}", tx.id, tx.status) }
            .doOnError { error -> logger.error("[TransactionMonitorService] 모니터링 중 오류 발생", error) }
            .onErrorContinue { error, _ -> logger.error("[TransactionMonitorService] 개별 트랜잭션 처리 오류, 계속 진행", error) }
            .blockLast() // fixedDelay가 실제 완료 후 60s 보장하기 위해 블로킹
    }

    /**
     * RESERVED 상태에서 오래 머문 UTXO 복구 스케줄러
     *
     * BTC 트랜잭션이 CONFIRMED/FAILED가 되어도 UTXO 상태가 갱신되지 않았거나,
     * broadcast 전에 서비스가 재시작된 경우 자금이 동결될 수 있음.
     * lockedAt 기준으로 stuckUtxoRecoveryMinutes 이상 RESERVED인 UTXO를 복구.
     */
    @Scheduled(fixedDelay = 300_000) // 5분마다 실행
    fun recoverStuckReservedUtxos() {
        logger.debug("[TransactionMonitorService] stuck UTXO 복구 시작")

        val stuckThreshold = LocalDateTime.now().minusMinutes(stuckUtxoRecoveryMinutes)

        utxoRepository.findByStatusAndLockedAtBefore(UtxoStatus.RESERVED, stuckThreshold)
            .flatMap { utxo ->
                val txHash = utxo.lockedBy
                if (txHash == null) {
                    // lockedBy 없는 RESERVED UTXO → 즉시 AVAILABLE로 복구
                    logger.warn("[TransactionMonitorService] lockedBy 없는 stuck UTXO 복구: utxoId={}", utxo.id)
                    utxoRepository.save(utxo.copy(status = UtxoStatus.AVAILABLE, lockedBy = null, lockedAt = null))
                } else {
                    txHistoryRepository.findByTxHash(txHash)
                        .flatMap { tx ->
                            when (tx.status) {
                                TransactionStatus.CONFIRMED -> {
                                    // 트랜잭션 확정 → UTXO SPENT 처리
                                    logger.info("[TransactionMonitorService] UTXO SPENT 처리: utxoId={}, txHash={}", utxo.id, txHash)
                                    utxoRepository.save(utxo.copy(status = UtxoStatus.SPENT))
                                }
                                TransactionStatus.FAILED -> {
                                    // 트랜잭션 실패 → UTXO AVAILABLE로 복구
                                    logger.warn("[TransactionMonitorService] 실패 tx UTXO 복구: utxoId={}, txHash={}", utxo.id, txHash)
                                    utxoRepository.save(utxo.copy(status = UtxoStatus.AVAILABLE, lockedBy = null, lockedAt = null))
                                }
                                else -> {
                                    // 아직 PENDING → 유지 (다음 주기에 재확인)
                                    logger.debug("[TransactionMonitorService] PENDING tx UTXO 유지: utxoId={}", utxo.id)
                                    Mono.just(utxo)
                                }
                            }
                        }
                        .switchIfEmpty(
                            // DB에 tx 없음 → broadcast 전 서비스 재시작 등의 케이스 → AVAILABLE 복구
                            run {
                                logger.warn("[TransactionMonitorService] tx 없는 stuck UTXO 복구: utxoId={}, txHash={}", utxo.id, txHash)
                                utxoRepository.save(utxo.copy(status = UtxoStatus.AVAILABLE, lockedBy = null, lockedAt = null))
                            }
                        )
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorContinue { error, _ -> logger.error("[TransactionMonitorService] stuck UTXO 복구 중 오류", error) }
            .blockLast()

        logger.debug("[TransactionMonitorService] stuck UTXO 복구 완료")
    }

    // ─── 내부 구현 메서드 ────────────────────────────────────────────────────────

    /**
     * Bitcoin Core RPC로 BTC 트랜잭션 상태를 확인하고 UTXO 상태를 전환한다. (내부 사용)
     *
     * `gettransaction {txid}` RPC로 실제 confirmations를 조회.
     * DB의 confirmations 필드에 의존하지 않음 — blockchain이 진실의 원천.
     *
     * ## Mempool Drop 감지 및 복구
     * `confirmations == -1`(노드 미발견)이면서 tx가 dropDetectionMinutes 이상 경과했을 때:
     * - rawTxHex가 있고 retryCount < maxRetryCount: 동일 서명 tx 재브로드캐스트
     * - 그 외: FAILED 처리 후 UTXO 반환
     *
     * BTC RBF는 ETH와 달리 동일 서명 tx 그대로 재전송 (nonce 개념 없음).
     * mempool에서 drop됐어도 UTXO는 여전히 유효하므로 재브로드캐스트로 복구 가능.
     *
     * @param tx PENDING 상태의 BTC 트랜잭션 히스토리
     */
    private fun checkBtcTransaction(
        tx: com.web3community.blockchain.domain.document.TransactionHistory
    ): Mono<com.web3community.blockchain.domain.document.TransactionHistory> {
        val txHash = tx.txHash ?: return Mono.just(tx)

        return bitcoinRpcClient.getTransactionConfirmations(txHash)
            .flatMap { confirmations ->
                when {
                    confirmations < 0 -> {
                        // txid를 노드에서 찾을 수 없음 → mempool drop 가능성
                        val txAgeMinutes = Duration.between(
                            tx.createdAt ?: LocalDateTime.now(), LocalDateTime.now()
                        ).toMinutes()

                        when {
                            txAgeMinutes < dropDetectionMinutes -> {
                                // 젊은 tx: 네트워크 전파 지연일 수 있음 → PENDING 유지
                                logger.debug(
                                    "[TransactionMonitorService] BTC tx 미발견 (전파 대기): txHash={}, age={}min",
                                    txHash, txAgeMinutes
                                )
                                Mono.just(tx)
                            }
                            tx.retryCount >= maxRetryCount -> {
                                // 최대 재시도 초과 → FAILED + UTXO 반환
                                logger.warn(
                                    "[TransactionMonitorService] BTC tx 최대 재시도 초과 FAILED: txHash={}, retry={}",
                                    txHash, tx.retryCount
                                )
                                val failedTx = tx.copy(
                                    status = TransactionStatus.FAILED,
                                    errorMessage = "mempool drop 후 최대 재시도 횟수(${maxRetryCount}) 초과"
                                )
                                txHistoryRepository.save(failedTx)
                                    .flatMap { releaseReservedUtxos(txHash, it) }
                            }
                            tx.rawTxHex != null -> {
                                // 재브로드캐스트: 동일 서명 tx 그대로 재전송 (BTC는 UTXO 기반, 재서명 불필요)
                                logger.info(
                                    "[TransactionMonitorService] BTC tx mempool drop 감지, 재브로드캐스트: txHash={}, age={}min, retry={}",
                                    txHash, txAgeMinutes, tx.retryCount
                                )
                                bitcoinRpcClient.sendRawTransaction(tx.rawTxHex)
                                    .flatMap { rebroadcastTxHash ->
                                        // 성공: retryCount 증가, txHash는 동일 (서명이 바뀌지 않았으므로)
                                        logger.info(
                                            "[TransactionMonitorService] BTC 재브로드캐스트 성공: txHash={}",
                                            rebroadcastTxHash
                                        )
                                        txHistoryRepository.save(tx.copy(retryCount = tx.retryCount + 1))
                                    }
                                    .onErrorResume { e ->
                                        logger.error(
                                            "[TransactionMonitorService] BTC 재브로드캐스트 실패: txHash={}", txHash, e
                                        )
                                        // 재전송 실패 — retryCount 증가 후 다음 주기에 재시도
                                        txHistoryRepository.save(
                                            tx.copy(
                                                retryCount = tx.retryCount + 1,
                                                errorMessage = "재브로드캐스트 실패: ${e.message}"
                                            )
                                        )
                                    }
                            }
                            else -> {
                                // rawTxHex 없음 → 재전송 불가, FAILED + UTXO 반환
                                logger.warn(
                                    "[TransactionMonitorService] BTC tx drop, rawTxHex 없어 재전송 불가: txHash={}",
                                    txHash
                                )
                                val failedTx = tx.copy(
                                    status = TransactionStatus.FAILED,
                                    errorMessage = "mempool drop 감지: rawTxHex 없어 재전송 불가"
                                )
                                txHistoryRepository.save(failedTx)
                                    .flatMap { releaseReservedUtxos(txHash, it) }
                            }
                        }
                    }
                    confirmations < btcConfirmationThreshold -> {
                        // 아직 충분히 confirm 안됨 → DB만 업데이트
                        val updatedTx = tx.copy(confirmations = confirmations)
                        if (updatedTx.confirmations != tx.confirmations) {
                            txHistoryRepository.save(updatedTx)
                        } else {
                            Mono.just(tx)
                        }
                    }
                    else -> {
                        // 6 confirmations 이상 → CONFIRMED + UTXO SPENT 전환
                        logger.info(
                            "[TransactionMonitorService] BTC tx CONFIRMED: txHash={}, confirmations={}",
                            txHash, confirmations
                        )
                        val confirmedTx = tx.copy(status = TransactionStatus.CONFIRMED, confirmations = confirmations)
                        txHistoryRepository.save(confirmedTx)
                            .flatMap { markUtxosSpent(txHash, it) }
                    }
                }
            }
            .onErrorResume { e ->
                logger.error("[TransactionMonitorService] BTC tx 확인 실패: txHash={}", txHash, e)
                Mono.just(tx)
            }
    }

    /** txHash 기준 RESERVED UTXO를 SPENT로 전환. (내부 사용) */
    private fun markUtxosSpent(
        txHash: String,
        tx: com.web3community.blockchain.domain.document.TransactionHistory
    ): Mono<com.web3community.blockchain.domain.document.TransactionHistory> {
        return utxoRepository.findByStatusAndLockedAtBefore(UtxoStatus.RESERVED, LocalDateTime.now())
            .filter { it.lockedBy == txHash }
            .flatMap { utxo ->
                logger.info("[TransactionMonitorService] UTXO SPENT: utxoId={}, txHash={}", utxo.id, txHash)
                utxoRepository.save(utxo.copy(status = UtxoStatus.SPENT))
            }
            .then(Mono.just(tx))
    }

    /** txHash 기준 RESERVED UTXO를 AVAILABLE로 복구. (내부 사용 - tx FAILED 시) */
    private fun releaseReservedUtxos(
        txHash: String,
        tx: com.web3community.blockchain.domain.document.TransactionHistory
    ): Mono<com.web3community.blockchain.domain.document.TransactionHistory> {
        return utxoRepository.findByStatusAndLockedAtBefore(UtxoStatus.RESERVED, LocalDateTime.now())
            .filter { it.lockedBy == txHash }
            .flatMap { utxo ->
                logger.warn("[TransactionMonitorService] UTXO 복구 (tx FAILED): utxoId={}", utxo.id)
                utxoRepository.save(utxo.copy(status = UtxoStatus.AVAILABLE, lockedBy = null, lockedAt = null))
            }
            .then(Mono.just(tx))
    }

    /**
     * ETH 트랜잭션의 블록체인 확인 상태를 조회하고 업데이트한다. (내부 사용)
     *
     * ## 2단계 RPC 조회
     * 1. `eth_getTransactionReceipt`: 블록 포함 여부 확인
     *    - 있으면 → CONFIRMED/FAILED 처리
     * 2. receipt 없으면 `eth_getTransactionByHash`: mempool 존재 여부 확인
     *    - 있으면 → 정상 PENDING (no-op)
     *    - 없으면 → Dropped 판정 → RBF 재전송 또는 FAILED
     *
     * ## Mempool Drop RBF 흐름
     * - tx 나이가 dropDetectionMinutes 미만: 전파 지연 가능성 → PENDING 유지
     * - tx 나이가 dropDetectionMinutes 이상 AND retryCount < maxRetryCount:
     *   `EthTransactionService.resubmitDroppedEthTx()` 호출 → 같은 nonce + bumped gasPrice
     * - retryCount >= maxRetryCount: FAILED 처리
     *
     * @param tx PENDING 상태의 [com.web3community.blockchain.domain.document.TransactionHistory]
     * @return 상태가 업데이트된 트랜잭션 [Mono]
     */
    private fun checkEthTransaction(
        tx: com.web3community.blockchain.domain.document.TransactionHistory
    ): Mono<com.web3community.blockchain.domain.document.TransactionHistory> {
        val txHash = tx.txHash ?: return Mono.just(tx)

        return Mono.fromCallable<EthTxCheckResult> {
            // 1단계: eth_getTransactionReceipt
            val receipt = web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.orElse(null)

            if (receipt != null) {
                val txBlockNumber = receipt.blockNumber
                    ?: return@fromCallable EthTxCheckResult.InMempool // 방어 코드
                val currentBlock = web3j.ethBlockNumber().send().blockNumber
                val confirmations = currentBlock.subtract(txBlockNumber).toInt().coerceAtLeast(0)
                val isSuccess = receipt.status == "0x1"
                logger.debug(
                    "[TransactionMonitorService] ETH receipt 확인: txHash={}, confirmations={}, success={}",
                    txHash, confirmations, isSuccess
                )
                EthTxCheckResult.Confirmed(confirmations, isSuccess, txBlockNumber)
            } else {
                // 2단계: receipt 없음 → eth_getTransactionByHash로 mempool 확인
                val txInMempool = web3j.ethGetTransactionByHash(txHash).send().transaction.orElse(null)
                if (txInMempool != null) {
                    // 아직 mempool에 있음 → 정상 PENDING
                    logger.debug("[TransactionMonitorService] ETH tx mempool 대기 중: txHash={}", txHash)
                    EthTxCheckResult.InMempool
                } else {
                    // mempool에도 없고 블록에도 없음 → dropped
                    logger.debug("[TransactionMonitorService] ETH tx mempool 미발견 (drop 가능성): txHash={}", txHash)
                    EthTxCheckResult.Dropped
                }
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { result ->
                when (result) {
                    is EthTxCheckResult.Confirmed -> {
                        val newStatus = when {
                            !result.isSuccess -> TransactionStatus.FAILED
                            result.confirmations >= ethConfirmationThreshold -> TransactionStatus.CONFIRMED
                            else -> TransactionStatus.PENDING
                        }
                        val errorMessage = if (!result.isSuccess) "EVM 트랜잭션 실행 실패 (revert)" else null
                        val updatedTx = tx.copy(
                            status = newStatus,
                            confirmations = result.confirmations,
                            blockNumber = result.blockNumber.toLong(),
                            errorMessage = errorMessage
                        )
                        if (updatedTx.status != tx.status || updatedTx.confirmations != tx.confirmations) {
                            logger.info(
                                "[TransactionMonitorService] ETH 상태 변경: txHash={}, {} → {}, confirmations={}",
                                txHash, tx.status, newStatus, result.confirmations
                            )
                            txHistoryRepository.save(updatedTx)
                        } else {
                            Mono.just(updatedTx)
                        }
                    }
                    EthTxCheckResult.InMempool -> Mono.just(tx) // 변경 없음
                    EthTxCheckResult.Dropped -> {
                        val txAgeMinutes = Duration.between(
                            tx.createdAt ?: LocalDateTime.now(), LocalDateTime.now()
                        ).toMinutes()

                        when {
                            txAgeMinutes < dropDetectionMinutes -> {
                                // 아직 젊음 → 전파 지연 가능성, 다음 주기 재확인
                                logger.debug(
                                    "[TransactionMonitorService] ETH tx drop 의심, 아직 전파 대기: txHash={}, age={}min",
                                    txHash, txAgeMinutes
                                )
                                Mono.just(tx)
                            }
                            tx.retryCount >= maxRetryCount -> {
                                // 최대 재시도 초과 → FAILED
                                logger.warn(
                                    "[TransactionMonitorService] ETH tx mempool drop 최대 재시도 초과 FAILED: txHash={}, retry={}",
                                    txHash, tx.retryCount
                                )
                                txHistoryRepository.save(
                                    tx.copy(
                                        status = TransactionStatus.FAILED,
                                        errorMessage = "mempool drop 후 최대 재시도 횟수(${maxRetryCount}) 초과"
                                    )
                                )
                            }
                            else -> {
                                // RBF 재전송: 동일 nonce + bumped gasPrice
                                logger.info(
                                    "[TransactionMonitorService] ETH mempool drop 감지, RBF 재전송: txHash={}, age={}min, retry={}",
                                    txHash, txAgeMinutes, tx.retryCount
                                )
                                ethTransactionService.resubmitDroppedEthTx(tx)
                            }
                        }
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
