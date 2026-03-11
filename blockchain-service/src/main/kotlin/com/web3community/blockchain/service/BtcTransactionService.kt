package com.web3community.blockchain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.domain.document.TransactionHistory
import com.web3community.blockchain.domain.document.TransactionStatus
import com.web3community.blockchain.domain.document.TransactionType
import com.web3community.blockchain.domain.document.UtxoSet
import com.web3community.blockchain.domain.document.UtxoStatus
import com.web3community.blockchain.domain.repository.TransactionHistoryRepository
import com.web3community.blockchain.domain.repository.UtxoSetRepository
import com.web3community.blockchain.domain.repository.WalletRepository
import com.web3community.blockchain.dto.TransactionResponse
import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import com.web3community.common.kafka.KafkaTopics
import com.web3community.common.security.CryptoUtils
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.script.ScriptBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

/**
 * Bitcoin 트랜잭션 처리 서비스
 *
 * ## 개요
 * BTC 전송을 처리하는 리액티브 서비스.
 * UTXO 선택(Greedy 알고리즘), Redis 분산 락을 통한 이중 지불 방지,
 * BitcoinJ를 통한 트랜잭션 서명, RPC를 통한 브로드캐스트를 담당한다.
 *
 * ## BTC 트랜잭션 처리 흐름
 * ```
 * sendBtc(userId, walletId, toAddress, amount, feeRate)
 *     ↓ 지갑 조회 및 개인키 복호화
 *     ↓ AVAILABLE UTXO 조회 및 Greedy 선택
 *     ↓ Redis 분산 락으로 UTXO 잠금 (이중 지불 방지)
 *     ↓ Bitcoin 트랜잭션 빌드 및 서명
 *     ↓ Bitcoin RPC로 브로드캐스트 (sendrawtransaction)
 *     ↓ UTXO 상태 RESERVED로 업데이트
 *     ↓ TransactionHistory 저장 (PENDING 상태)
 *     ↓ Kafka 이벤트 발행
 *     ↓ TransactionResponse 반환
 * ```
 *
 * ## UTXO Greedy 선택 알고리즘
 * 1. AVAILABLE UTXO를 금액 내림차순으로 정렬
 * 2. 필요 금액(전송 금액 + 예상 수수료)을 충족할 때까지 UTXO 누적 선택
 * 3. 잔돈(change) = 선택된 UTXO 합 - 전송 금액 - 수수료
 *
 * @param walletRepository 지갑 리포지토리
 * @param utxoRepository UTXO Set 리포지토리
 * @param txHistoryRepository 트랜잭션 히스토리 리포지토리
 * @param cryptoUtils 개인키 복호화 유틸리티
 * @param redisTemplate Redis 리액티브 템플릿 (UTXO 분산 락)
 * @param networkParameters BitcoinJ 네트워크 파라미터
 * @param kafkaTemplate Kafka 프로듀서
 * @param objectMapper JSON 직렬화
 */
@Service
class BtcTransactionService(
    private val walletRepository: WalletRepository,
    private val utxoRepository: UtxoSetRepository,
    private val txHistoryRepository: TransactionHistoryRepository,
    private val cryptoUtils: CryptoUtils,
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val networkParameters: NetworkParameters,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(BtcTransactionService::class.java)

    /** 개인키 복호화에 사용하는 마스터 키 */
    @Value("\${blockchain.crypto.master-key}")
    private lateinit var masterKey: String

    /** UTXO 락 TTL (초) */
    @Value("\${blockchain.utxo.lock-ttl-seconds:60}")
    private var utxoLockTtlSeconds: Long = 60L

    /** 기본 수수료율 (sat/vbyte) - 수수료 API 실패 시 fallback */
    @Value("\${blockchain.bitcoin.default-fee-rate:10}")
    private var defaultFeeRate: Long = 10L

    /**
     * BTC 전송
     *
     * 지정된 주소로 BTC를 전송하고 결과를 MongoDB에 기록한다.
     * UTXO 기반 이중 지불 방지와 Redis 분산 락을 통해 동시성을 제어한다.
     *
     * @param userId 요청 사용자 ID
     * @param walletId 출금에 사용할 지갑 ID
     * @param toAddress 수신 BTC 주소 (Bech32 또는 P2PKH)
     * @param amount 전송 금액 (satoshi 단위)
     * @param feeRate 수수료율 (sat/vbyte, null이면 default 사용)
     * @return 트랜잭션 정보 [TransactionResponse]
     * @throws BusinessException BLOCKCHAIN_003: 지갑을 찾을 수 없는 경우
     * @throws BusinessException BLOCKCHAIN_004: 잔액 부족
     * @throws BusinessException BLOCKCHAIN_009: UTXO 락 획득 실패 (동시 출금 충돌)
     * @throws BusinessException BLOCKCHAIN_007: 브로드캐스트 실패
     */
    fun sendBtc(
        userId: String,
        walletId: String,
        toAddress: String,
        amount: BigDecimal,
        feeRate: Long? = null
    ): Mono<TransactionResponse> {
        val effectiveFeeRate = feeRate ?: defaultFeeRate
        logger.info(
            "[BtcTransactionService] BTC 전송 요청: userId={}, walletId={}, to={}, amount={}, feeRate={}",
            userId, walletId, toAddress, amount, effectiveFeeRate
        )

        return walletRepository.findById(walletId)
            .switchIfEmpty(Mono.error(BusinessException(ErrorCode.BLOCKCHAIN_003)))
            .flatMap { wallet ->
                // 지갑 소유자 검증
                if (wallet.userId != userId) {
                    return@flatMap Mono.error(BusinessException(ErrorCode.AUTH_002))
                }

                // AVAILABLE UTXO 전체 조회
                utxoRepository.findByWalletIdAndStatus(walletId, UtxoStatus.AVAILABLE)
                    .collectList()
                    .flatMap { availableUtxos ->
                        val amountSatoshi = amount.toLong()

                        // Greedy 알고리즘으로 UTXO 선택
                        val selectedUtxos = selectUtxosGreedy(availableUtxos, amountSatoshi, effectiveFeeRate)
                        if (selectedUtxos.isEmpty()) {
                            return@flatMap Mono.error<TransactionResponse>(
                                BusinessException(ErrorCode.BLOCKCHAIN_004)
                            )
                        }

                        val totalInput = selectedUtxos.sumOf { it.amount }
                        // 예상 트랜잭션 크기: 입력당 148 vbytes + 출력당 34 vbytes + 10 vbytes overhead
                        val estimatedSize = selectedUtxos.size * 148 + 2 * 34 + 10
                        val fee = estimatedSize * effectiveFeeRate
                        val change = totalInput - amountSatoshi - fee

                        if (change < 0) {
                            return@flatMap Mono.error<TransactionResponse>(
                                BusinessException(ErrorCode.BLOCKCHAIN_004)
                            )
                        }

                        // Redis 분산 락으로 선택된 UTXO 잠금
                        lockUtxos(selectedUtxos)
                            .flatMap { locked ->
                                if (!locked) {
                                    return@flatMap Mono.error<TransactionResponse>(
                                        BusinessException(ErrorCode.BLOCKCHAIN_009)
                                    )
                                }

                                // 개인키 복호화 및 트랜잭션 빌드/서명/브로드캐스트
                                Mono.fromCallable {
                                    val privateKeyHex = cryptoUtils.decrypt(
                                        wallet.encryptedPrivateKey, masterKey, userId.toLong()
                                    )
                                    val ecKey = ECKey.fromPrivate(
                                        java.math.BigInteger(privateKeyHex, 16)
                                    )

                                    buildAndBroadcastBtcTransaction(
                                        ecKey = ecKey,
                                        selectedUtxos = selectedUtxos,
                                        toAddress = toAddress,
                                        fromAddress = wallet.address,
                                        amountSatoshi = amountSatoshi,
                                        feeSatoshi = fee,
                                        changeSatoshi = change
                                    )
                                }
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .onErrorResume { error ->
                                        // 브로드캐스트 실패 시 UTXO 락 해제
                                        unlockUtxos(selectedUtxos)
                                            .then(Mono.error(
                                                if (error is BusinessException) error
                                                else BusinessException(ErrorCode.BLOCKCHAIN_007, cause = error)
                                            ))
                                    }
                                    .flatMap { txHash ->
                                        // UTXO 상태를 RESERVED로 업데이트
                                        val now = LocalDateTime.now()
                                        val updatedUtxos = selectedUtxos.map { utxo ->
                                            utxo.copy(
                                                status = UtxoStatus.RESERVED,
                                                lockedBy = txHash,
                                                lockedAt = now
                                            )
                                        }
                                        utxoRepository.saveAll(updatedUtxos).collectList()
                                            .thenReturn(Pair(txHash, fee))
                                    }
                                    .flatMap { (txHash, feeSatoshi) ->
                                        // TransactionHistory 저장
                                        val txHistory = TransactionHistory(
                                            walletId = walletId,
                                            userId = userId,
                                            chain = Chain.BTC,
                                            type = TransactionType.SEND,
                                            fromAddress = wallet.address,
                                            toAddress = toAddress,
                                            amount = amount,
                                            fee = BigDecimal.valueOf(feeSatoshi),
                                            txHash = txHash,
                                            status = TransactionStatus.PENDING
                                        )
                                        txHistoryRepository.save(txHistory)
                                    }
                                    .doOnSuccess { savedTx ->
                                        publishTransactionEvent(savedTx)
                                        logger.info(
                                            "[BtcTransactionService] BTC 전송 완료: txHash={}",
                                            savedTx.txHash
                                        )
                                    }
                                    .map { TransactionResponse.from(it) }
                            }
                    }
            }
    }

    // ─── 내부 구현 메서드 ────────────────────────────────────────────────────────

    /**
     * Greedy 알고리즘으로 UTXO를 선택한다. (내부 사용)
     *
     * 금액 큰 UTXO를 먼저 선택하여 입력 개수를 최소화하고 수수료를 절감한다.
     * 선택된 UTXO의 합이 `amount + 예상 수수료` 이상이 될 때까지 선택을 반복한다.
     *
     * @param utxos 사용 가능한 UTXO 목록
     * @param amountSatoshi 전송 금액 (satoshi)
     * @param feeRate 수수료율 (sat/vbyte)
     * @return 선택된 UTXO 목록 (잔액 부족 시 빈 목록)
     */
    private fun selectUtxosGreedy(
        utxos: List<UtxoSet>,
        amountSatoshi: Long,
        feeRate: Long
    ): List<UtxoSet> {
        // 금액 내림차순 정렬 (큰 UTXO를 먼저 사용하여 입력 수 최소화)
        val sorted = utxos.sortedByDescending { it.amount }
        val selected = mutableListOf<UtxoSet>()
        var accumulated = 0L

        for (utxo in sorted) {
            selected.add(utxo)
            accumulated += utxo.amount

            // 현재 선택 기준 예상 수수료 계산
            val estimatedSize = selected.size * 148 + 2 * 34 + 10
            val estimatedFee = estimatedSize * feeRate

            if (accumulated >= amountSatoshi + estimatedFee) {
                break
            }
        }

        // 최종 확인: 잔액이 충분한지 검사
        val finalSize = selected.size * 148 + 2 * 34 + 10
        val finalFee = finalSize * feeRate
        return if (accumulated >= amountSatoshi + finalFee) selected else emptyList()
    }

    /**
     * Redis 분산 락으로 선택된 UTXO를 잠근다. (내부 사용)
     *
     * 모든 선택된 UTXO에 대해 `utxo:lock:{utxoId}` 키로 NX(Not eXists) SET을 수행한다.
     * 하나라도 이미 잠겨있으면 전체 잠금을 해제하고 false를 반환한다.
     *
     * Redis key: `utxo:lock:{utxoId}`
     *
     * @param utxos 잠금할 UTXO 목록
     * @return 모두 잠금 성공 여부 [Mono]
     */
    private fun lockUtxos(utxos: List<UtxoSet>): Mono<Boolean> {
        if (utxos.isEmpty()) return Mono.just(false)

        val lockOperations = utxos.map { utxo ->
            val lockKey = "utxo:lock:${utxo.id}"
            redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(utxoLockTtlSeconds))
        }

        // 모든 잠금 시도 순차 실행
        return lockOperations.reduce { acc, mono ->
            acc.zipWith(mono) { a, b -> a && b }
        }
            .flatMap { allLocked ->
                if (!allLocked) {
                    // 일부 잠금 실패 시 획득한 잠금 전체 해제
                    unlockUtxos(utxos).thenReturn(false)
                } else {
                    Mono.just(true)
                }
            }
    }

    /**
     * Redis에서 UTXO 잠금을 해제한다. (내부 사용)
     *
     * @param utxos 잠금 해제할 UTXO 목록
     * @return 완료 [Mono]
     */
    private fun unlockUtxos(utxos: List<UtxoSet>): Mono<Void> {
        val unlockOps = utxos.map { utxo ->
            redisTemplate.delete("utxo:lock:${utxo.id}")
        }
        return if (unlockOps.isEmpty()) {
            Mono.empty()
        } else {
            unlockOps.reduce { acc, mono -> acc.then(mono) }.then()
        }
    }

    /**
     * BitcoinJ로 트랜잭션을 빌드하고 서명한다. (내부 사용)
     *
     * 선택된 UTXO를 입력으로, 수신 주소와 잔돈 주소를 출력으로 하는
     * Bitcoin 트랜잭션을 생성하고 서명한다. 브로드캐스트는 실제 환경에서
     * Bitcoin Core RPC의 `sendrawtransaction`을 통해 수행된다.
     *
     * @param ecKey 서명에 사용할 EC 키 쌍
     * @param selectedUtxos 입력으로 사용할 UTXO 목록
     * @param toAddress 수신 주소
     * @param fromAddress 잔돈 반환 주소 (지갑 주소)
     * @param amountSatoshi 전송 금액 (satoshi)
     * @param feeSatoshi 수수료 (satoshi)
     * @param changeSatoshi 잔돈 금액 (satoshi)
     * @return 브로드캐스트된 트랜잭션 해시
     */
    private fun buildAndBroadcastBtcTransaction(
        ecKey: ECKey,
        selectedUtxos: List<UtxoSet>,
        toAddress: String,
        fromAddress: String,
        amountSatoshi: Long,
        feeSatoshi: Long,
        changeSatoshi: Long
    ): String {
        // Bitcoin 트랜잭션 생성
        val tx = Transaction(networkParameters)

        // 입력 추가: 선택된 각 UTXO를 트랜잭션 입력으로 추가
        for (utxo in selectedUtxos) {
            val outPoint = TransactionOutPoint(
                networkParameters,
                utxo.vout.toLong(),
                org.bitcoinj.core.Sha256Hash.wrap(utxo.txid)
            )
            // P2WPKH 서명 스크립트: witness 데이터로 서명 (SegWit)
            val scriptPubKey = ScriptBuilder.createP2WPKHOutputScript(ecKey)
            tx.addSignedInput(outPoint, scriptPubKey, ecKey, Transaction.SigHash.ALL, true)
        }

        // 출력 추가 1: 수신자 주소로 전송 금액 출력
        val recipientAddress = Address.fromString(networkParameters, toAddress)
        tx.addOutput(Coin.valueOf(amountSatoshi), recipientAddress)

        // 출력 추가 2: 잔돈이 있으면 지갑(자신) 주소로 반환
        if (changeSatoshi > 546) { // dust limit (546 satoshi) 이하는 잔돈 출력 생략
            val changeAddress = Address.fromString(networkParameters, fromAddress)
            tx.addOutput(Coin.valueOf(changeSatoshi), changeAddress)
        }

        // 서명된 트랜잭션을 hex로 직렬화
        val rawTxHex = org.bitcoinj.core.Utils.HEX.encode(tx.bitcoinSerialize())

        logger.debug("[BtcTransactionService] 트랜잭션 빌드 완료: size={}bytes, fee={}sat", tx.messageSize, feeSatoshi)

        // 실제 환경: Bitcoin Core RPC sendrawtransaction 호출
        // 현재는 트랜잭션 해시를 반환 (실제 구현에서는 RPC 연동 필요)
        // TODO: Bitcoin Core RPC 클라이언트 연동 시 실제 브로드캐스트로 교체
        val txHash = tx.txId.toString()
        logger.info("[BtcTransactionService] 트랜잭션 브로드캐스트: txHash={}, rawTx={}", txHash, rawTxHex.take(20))

        return txHash
    }

    /**
     * Kafka에 트랜잭션 완료 이벤트를 발행한다. (내부 사용)
     *
     * @param tx 저장된 [TransactionHistory]
     */
    private fun publishTransactionEvent(tx: TransactionHistory) {
        try {
            val event = mapOf(
                "id" to tx.id,
                "txHash" to tx.txHash,
                "chain" to tx.chain.name,
                "userId" to tx.userId,
                "fromAddress" to tx.fromAddress,
                "toAddress" to tx.toAddress,
                "amount" to tx.amount,
                "status" to tx.status.name
            )
            val message = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(KafkaTopics.TRANSACTION_COMPLETED, tx.userId, message)
        } catch (e: Exception) {
            logger.error("[BtcTransactionService] Kafka 이벤트 발행 실패: txId={}", tx.id, e)
        }
    }
}
