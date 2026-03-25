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
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

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
    private val objectMapper: ObjectMapper,
    private val bitcoinRpcClient: BitcoinRpcClient
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
     * Lua 스크립트: lock value가 일치할 때만 삭제 (안전한 lock 해제)
     *
     * Redis의 단순 DEL은 lock 보유자 확인 없이 삭제하므로 다른 프로세스의 lock을
     * 삭제할 수 있다. GET + DEL을 원자적으로 실행하여 자신이 설정한 lock만 삭제.
     *
     * KEYS[1] = lock key
     * ARGV[1] = 이 프로세스가 설정한 lock value (requestId)
     * Returns: 1 = 삭제 성공, 0 = 다른 프로세스 소유 또는 이미 만료
     */
    private val safeUnlockScript = RedisScript.of<Long>(
        """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """.trimIndent(),
        Long::class.java
    )

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

                // 출금 전 blockchain listunspent와 DB UTXO 동기화:
                // DB에만 있고 blockchain에 없는 UTXO(외부 사용됨)를 SPENT 처리 후 코인 선택.
                // ETH nonce와 동일한 원칙: DB만 믿지 않고 blockchain이 진실의 원천.
                syncUtxosFromBlockchain(walletId, wallet.address)
                    .then(utxoRepository.findByWalletIdAndStatus(walletId, UtxoStatus.AVAILABLE)
                    .collectList())
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

                        // P2WPKH 트랜잭션 크기 계산 (SegWit discount 반영)
                        // - 입력(P2WPKH): 41 base + 107 witness/4 ≈ 68 vbytes  (legacy P2PKH는 148)
                        // - 출력(P2WPKH): 31 vbytes
                        // - 오버헤드: version(4) + marker(1) + flag(1) + locktime(4) + varint(2) = 12 vbytes ≈ 11 vbytes
                        // 잔돈 출력 유무를 고려: 잔돈 있으면 2 outputs, 없으면 1 output
                        val numInputs = selectedUtxos.size
                        val feeWith2Outputs = (numInputs * 68L + 2L * 31 + 11) * effectiveFeeRate
                        val changeWith2Outputs = totalInput - amountSatoshi - feeWith2Outputs

                        // 잔돈이 dust limit(546 sat) 이하면 change output 없이 재계산
                        val (fee, change) = if (changeWith2Outputs > 546) {
                            Pair(feeWith2Outputs, changeWith2Outputs)
                        } else {
                            val feeWith1Output = (numInputs * 68L + 1L * 31 + 11) * effectiveFeeRate
                            val changeWith1Output = totalInput - amountSatoshi - feeWith1Output
                            // 잔돈이 음수(잔액 부족)는 상위에서 처리
                            Pair(feeWith1Output, changeWith1Output)
                        }

                        if (change < 0) {
                            return@flatMap Mono.error<TransactionResponse>(
                                BusinessException(ErrorCode.BLOCKCHAIN_004)
                            )
                        }

                        // 요청별 고유 ID — lock value로 사용하여 다른 프로세스 lock 오삭제 방지
                        val requestId = UUID.randomUUID().toString()

                        // Redis 분산 락으로 선택된 UTXO 잠금
                        lockUtxos(selectedUtxos, requestId)
                            .flatMap { locked ->
                                if (!locked) {
                                    return@flatMap Mono.error<TransactionResponse>(
                                        BusinessException(ErrorCode.BLOCKCHAIN_009)
                                    )
                                }

                                // 1단계: 개인키 복호화 + 트랜잭션 빌드/서명 (blocking CPU 작업)
                                Mono.fromCallable {
                                    val privateKeyHex = cryptoUtils.decrypt(
                                        wallet.encryptedPrivateKey, masterKey, userId.toLong()
                                    )
                                    val ecKey = ECKey.fromPrivate(
                                        java.math.BigInteger(privateKeyHex, 16)
                                    )
                                    buildBtcTransaction(
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
                                    // 2단계: Bitcoin Core RPC broadcast (non-blocking)
                                    // rawTxHex를 Pair로 유지: broadcast 후 TransactionHistory 저장 시 필요
                                    .flatMap { (localTxHash, rawTxHex) ->
                                        bitcoinRpcClient.sendRawTransaction(rawTxHex)
                                            .map { broadcastTxHash ->
                                                if (broadcastTxHash != localTxHash) {
                                                    logger.error(
                                                        "[BtcTransactionService] txid 불일치: local={}, rpc={}",
                                                        localTxHash, broadcastTxHash
                                                    )
                                                    throw BusinessException(ErrorCode.BLOCKCHAIN_007, "txid 불일치")
                                                }
                                                Pair(broadcastTxHash, rawTxHex) // rawTxHex 스코프 유지
                                            }
                                    }
                                    .onErrorResume { error ->
                                        // 브로드캐스트 실패 시 자신이 획득한 lock만 안전하게 해제
                                        unlockUtxos(selectedUtxos, requestId)
                                            .then(Mono.error(
                                                if (error is BusinessException) error
                                                else BusinessException(ErrorCode.BLOCKCHAIN_007, cause = error)
                                            ))
                                    }
                                    .flatMap { (txHash, rawTxHex) ->
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
                                            .thenReturn(Triple(txHash, fee, rawTxHex)) // rawTxHex 스코프 유지
                                    }
                                    .flatMap { (txHash, feeSatoshi, rawTxHex) ->
                                        // TransactionHistory 저장
                                        // rawTxHex 저장: mempool drop 시 동일 서명 tx 재브로드캐스트에 사용
                                        // (BTC는 UTXO 기반이라 동일 서명 tx를 그대로 재전송 가능)
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
                                            status = TransactionStatus.PENDING,
                                            rawTxHex = rawTxHex
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

            // P2WPKH 기준 예상 수수료 계산 (change output 2개 가정 — 보수적 추정)
            val estimatedSize = selected.size * 68L + 2L * 31 + 11
            val estimatedFee = estimatedSize * feeRate

            if (accumulated >= amountSatoshi + estimatedFee) {
                break
            }
        }

        // 최종 확인: 잔액이 충분한지 검사 (change output 있는 경우 기준)
        val finalSize = selected.size * 68L + 2L * 31 + 11
        val finalFee = finalSize * feeRate
        return if (accumulated >= amountSatoshi + finalFee) selected else emptyList()
    }

    /**
     * Redis 분산 락으로 선택된 UTXO를 순차적으로 잠근다. (내부 사용)
     *
     * ## 핵심 설계
     * 1. lock value = requestId (UUID): 어떤 요청이 잠갔는지 추적
     *    → 다른 프로세스의 lock을 오삭제하는 사고 방지
     * 2. 순차 실행 (flatMapSequential): 하나 실패 시 즉시 중단
     *    → 불필요한 lock 획득 최소화, 롤백 대상 명확화
     * 3. 실패 시 자신이 실제로 획득한 lock만 해제
     *    → 이전 구현의 "전체 해제" 버그 수정
     *
     * ## 이전 구현 버그
     * ```
     * A: lock(UTXO1)✓, lock(UTXO2)✗(B가 보유), lock(UTXO3)✓
     * A: 실패 → unlockUtxos([1,2,3]) → UTXO2 key 삭제 = B의 lock 삭제!
     * C: UTXO2 lock 성공 → B,C 동시에 UTXO2 사용 = 이중 지불
     * ```
     *
     * Redis key: `utxo:lock:{utxoId}`
     *
     * @param utxos 잠금할 UTXO 목록
     * @param requestId 이 요청의 고유 ID (lock value로 사용)
     * @return 모두 잠금 성공 여부 [Mono]
     */
    private fun lockUtxos(utxos: List<UtxoSet>, requestId: String): Mono<Boolean> {
        if (utxos.isEmpty()) return Mono.just(false)

        // 각 UTXO에 대해 (utxo, 획득 여부) 쌍을 순차적으로 수집
        // flatMapSequential: 순서 보장하면서 하나씩 실행
        return Flux.fromIterable(utxos)
            .flatMapSequential { utxo ->
                val lockKey = "utxo:lock:${utxo.id}"
                // lock value = requestId로 설정 — 누가 잠갔는지 식별 가능
                redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, requestId, Duration.ofSeconds(utxoLockTtlSeconds))
                    .map { acquired -> Pair(utxo, acquired == true) }
            }
            .collectList()
            .flatMap { results ->
                val allLocked = results.all { it.second }
                if (allLocked) {
                    Mono.just(true)
                } else {
                    // 자신이 실제로 획득한 lock만 해제 (second=true인 것만)
                    val acquiredUtxos = results.filter { it.second }.map { it.first }
                    if (acquiredUtxos.isNotEmpty()) {
                        logger.debug(
                            "[BtcTransactionService] 부분 lock 실패, 획득한 {}개 lock 롤백: requestId={}",
                            acquiredUtxos.size, requestId
                        )
                    }
                    unlockUtxos(acquiredUtxos, requestId).thenReturn(false)
                }
            }
    }

    /**
     * Redis에서 UTXO 잠금을 안전하게 해제한다. (내부 사용)
     *
     * Lua 스크립트로 lock value(requestId)를 확인 후 일치할 때만 삭제.
     * 단순 DEL 대신 이 방식을 쓰는 이유:
     * - TTL 만료 후 다른 프로세스가 재획득한 lock을 삭제하는 사고 방지
     * - 자신이 설정하지 않은 lock을 삭제하는 사고 방지
     *
     * @param utxos 잠금 해제할 UTXO 목록
     * @param requestId 잠금 시 사용한 requestId (lock value)
     * @return 완료 [Mono]
     */
    private fun unlockUtxos(utxos: List<UtxoSet>, requestId: String): Mono<Void> {
        if (utxos.isEmpty()) return Mono.empty()

        return Flux.fromIterable(utxos)
            .flatMap { utxo ->
                val lockKey = "utxo:lock:${utxo.id}"
                redisTemplate.execute(
                    safeUnlockScript,
                    listOf(lockKey),
                    listOf(requestId)
                ).next()
                    .doOnNext { result ->
                        if (result == 0L) {
                            logger.warn(
                                "[BtcTransactionService] UTXO lock 해제 스킵 (다른 소유자 또는 만료): utxoId={}, requestId={}",
                                utxo.id, requestId
                            )
                        }
                    }
            }
            .then()
    }

    /**
     * BitcoinJ로 트랜잭션을 빌드하고 서명한다. (내부 사용)
     *
     * broadcast는 호출자가 `bitcoinRpcClient.sendRawTransaction(rawTxHex)`로 수행.
     * 이 메서드는 순수하게 트랜잭션 구성 + 서명만 담당한다.
     *
     * @return Pair(txHash, rawTxHex) — txHash는 로컬 계산값, broadcast 후 노드 반환값과 일치해야 함
     */
    private fun buildBtcTransaction(
        ecKey: ECKey,
        selectedUtxos: List<UtxoSet>,
        toAddress: String,
        fromAddress: String,
        amountSatoshi: Long,
        feeSatoshi: Long,
        changeSatoshi: Long
    ): Pair<String, String> {
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
        val txHash = tx.txId.toString()

        logger.debug("[BtcTransactionService] 트랜잭션 빌드/서명 완료: txHash={}, size={}bytes, fee={}sat",
            txHash, tx.messageSize, feeSatoshi)

        return Pair(txHash, rawTxHex)
    }

    /**
     * Bitcoin Core listunspent 결과로 DB UTXO를 동기화한다. (내부 사용)
     *
     * ## 동기화 정책
     * 1. blockchain에만 있고 DB에 없는 UTXO → 신규 수신, DB에 AVAILABLE로 추가
     * 2. DB에 AVAILABLE이지만 blockchain에 없는 UTXO → 외부 사용됨, SPENT로 업데이트
     * 3. DB에 RESERVED → 변경하지 않음 (진행 중인 출금)
     * 4. DB에 SPENT → 변경하지 않음
     *
     * ## 왜 출금 직전에 호출하는가
     * DB UTXO와 blockchain 상태가 불일치하면:
     * - 실제 spent UTXO를 선택 → Bitcoin Core가 "Input already spent" 거부
     * - 선택된 UTXO는 Redis lock 획득 후 broadcast 실패 → lock 롤백 필요
     * - 잔액이 실제보다 크게 표시됨
     *
     * @param walletId 지갑 ID
     * @param address Bitcoin 주소
     * @return 동기화 완료 [Mono]
     */
    private fun syncUtxosFromBlockchain(walletId: String, address: String): Mono<Void> {
        return bitcoinRpcClient.listUnspent(address)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { chainUtxos ->
                // blockchain의 txid:vout 집합
                val chainUtxoKeys = chainUtxos.associateBy { "${it.txid}:${it.vout}" }

                // DB의 현재 UTXO 전체 조회 (AVAILABLE + RESERVED 포함)
                utxoRepository.findByWalletIdAndStatus(walletId, UtxoStatus.AVAILABLE)
                    .mergeWith(utxoRepository.findByWalletIdAndStatus(walletId, UtxoStatus.RESERVED))
                    .collectList()
                    .flatMap { dbUtxos ->
                        val dbUtxoKeys = dbUtxos.associateBy { "${it.txid}:${it.vout}" }

                        // 1. blockchain에는 있고 DB에 없는 UTXO → 신규 수신
                        val toAdd = chainUtxos.filter { !dbUtxoKeys.containsKey("${it.txid}:${it.vout}") }
                            .map { unspent ->
                                UtxoSet(
                                    walletId = walletId,
                                    address = address,
                                    txid = unspent.txid,
                                    vout = unspent.vout,
                                    amount = unspent.amountSatoshi,
                                    scriptPubKey = unspent.scriptPubKey,
                                    confirmations = unspent.confirmations,
                                    status = UtxoStatus.AVAILABLE
                                )
                            }

                        // 2. DB에 AVAILABLE인데 blockchain에 없는 UTXO → 외부 사용됨
                        val toMarkSpent = dbUtxos
                            .filter { it.status == UtxoStatus.AVAILABLE }
                            .filter { !chainUtxoKeys.containsKey("${it.txid}:${it.vout}") }
                            .map { it.copy(status = UtxoStatus.SPENT) }

                        if (toAdd.isNotEmpty()) {
                            logger.info("[BtcTransactionService] 신규 UTXO {}개 추가: walletId={}", toAdd.size, walletId)
                        }
                        if (toMarkSpent.isNotEmpty()) {
                            logger.warn(
                                "[BtcTransactionService] 외부 사용된 UTXO {}개 SPENT 처리: walletId={}",
                                toMarkSpent.size, walletId
                            )
                        }

                        val addOp = if (toAdd.isNotEmpty()) utxoRepository.saveAll(toAdd).then() else Mono.empty()
                        val spentOp = if (toMarkSpent.isNotEmpty()) utxoRepository.saveAll(toMarkSpent).then() else Mono.empty()

                        addOp.then(spentOp)
                    }
            }
            .onErrorResume { e ->
                // RPC 장애 시 동기화 실패 → 경고 로그 후 기존 DB로 진행
                // 완전 중단 대신 경고: RPC 장애가 출금을 막아서는 안 됨 (이미 정상 확인된 UTXO는 사용 가능)
                logger.warn("[BtcTransactionService] blockchain UTXO 동기화 실패, DB 기준으로 진행: walletId={}", walletId, e)
                Mono.empty()
            }
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
