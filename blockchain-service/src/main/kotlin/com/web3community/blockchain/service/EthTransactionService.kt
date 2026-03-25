package com.web3community.blockchain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.domain.document.TransactionHistory
import com.web3community.blockchain.domain.document.TransactionStatus
import com.web3community.blockchain.domain.document.TransactionType
import com.web3community.blockchain.domain.repository.TransactionHistoryRepository
import com.web3community.blockchain.domain.repository.WalletRepository
import com.web3community.blockchain.dto.TransactionResponse
import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import com.web3community.common.kafka.KafkaTopics
import com.web3community.common.security.CryptoUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Ethereum 트랜잭션 처리 서비스
 *
 * ## 개요
 * ETH 네이티브 코인 전송을 처리하는 리액티브 서비스.
 * Redis를 통한 원자적 Nonce 관리, Web3j를 통한 트랜잭션 서명 및 브로드캐스트,
 * Kafka를 통한 완료 이벤트 발행을 담당한다.
 *
 * ## ETH 트랜잭션 처리 흐름
 * ```
 * sendEth(userId, walletId, toAddress, amount)
 *     ↓ 지갑 조회 및 개인키 복호화
 *     ↓ Redis에서 Nonce 원자적 획득/증가
 *     ↓ RawTransaction 빌드 (EIP-1559 legacy fallback)
 *     ↓ 트랜잭션 서명 (Credentials.create)
 *     ↓ eth_sendRawTransaction으로 브로드캐스트
 *     ↓ TransactionHistory 저장 (PENDING 상태)
 *     ↓ Kafka 이벤트 발행 (transaction.completed)
 *     ↓ TransactionResponse 반환
 * ```
 *
 * ## Nonce 관리 전략
 * Redis key: `eth:nonce:{address}`
 * - 최초 요청: 블록체인에서 현재 Nonce 조회 후 Redis에 캐시
 * - 이후 요청: Redis에서 원자적 INCR로 Nonce 발급
 * - 트랜잭션 실패 시: Redis Nonce 감소하여 반환
 *
 * @param web3j Ethereum JSON-RPC 클라이언트
 * @param walletRepository 지갑 리포지토리
 * @param txHistoryRepository 트랜잭션 히스토리 리포지토리
 * @param cryptoUtils 개인키 복호화 유틸리티
 * @param redisTemplate Redis 리액티브 템플릿 (Nonce 관리)
 * @param kafkaTemplate Kafka 프로듀서 (이벤트 발행)
 * @param objectMapper JSON 직렬화
 */
@Service
class EthTransactionService(
    private val web3j: Web3j,
    private val walletRepository: WalletRepository,
    private val txHistoryRepository: TransactionHistoryRepository,
    private val cryptoUtils: CryptoUtils,
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(EthTransactionService::class.java)

    /** 개인키 복호화에 사용하는 마스터 키 */
    @Value("\${blockchain.crypto.master-key}")
    private lateinit var masterKey: String

    /** Ethereum 체인 ID (메인넷: 1, Sepolia: 11155111) */
    @Value("\${blockchain.ethereum.chain-id:1}")
    private var chainId: Long = 1L

    /** ETH 네이티브 전송 가스 한도 (고정값) */
    private val ethTransferGasLimit = BigInteger.valueOf(21_000L)

    /** ERC20 transfer() 가스 한도 (컨트랙트 실행 비용 포함) */
    private val erc20TransferGasLimit = BigInteger.valueOf(65_000L)

    /**
     * Lua 스크립트: blockchain nonce 기준으로 원자적 조정 후 발급
     *
     * ## 핵심 정책: max(redis, blockchain) 적용
     * - current=nil 또는 current ≤ chainNonce:
     *     Redis가 stale하거나 초기화 전 상태.
     *     blockchain이 앞서 있으므로 blockchain 기준으로 재설정.
     *     → SET key = chainNonce+1, return chainNonce
     *
     * - current > chainNonce:
     *     Redis에 in-flight tx들이 있어 Redis가 앞서 있는 정상 상태.
     *     blockchain 값으로 되돌리면 nonce 중복 발생.
     *     → INCR, return previous value (Redis 흐름 유지)
     *
     * ## 동시성 안전성
     * Lua는 Redis에서 단일 스레드로 원자 실행. 10개 요청이 모두 chainNonce=5로 진입해도:
     * - 1번: SET key=6, return 5
     * - 2번~10번: current > chain → INCR → return 6,7,...14
     * → 중복 nonce 발급 불가
     *
     * ## 처리 불가 케이스: mempool tx drop
     * nonce N tx가 drop되면 N+1 이후 tx들이 stuck. 이 스크립트로는 감지 불가.
     * TransactionMonitor의 stuck tx 감지 + 수동 재처리 필요.
     *
     * KEYS[1] = eth:nonce:{address}
     * ARGV[1] = blockchain eth_getTransactionCount(PENDING) 결과
     */
    private val nonceReconcileScript = RedisScript.of<Long>(
        """
        local current = tonumber(redis.call('GET', KEYS[1]))
        local chain = tonumber(ARGV[1])
        if current == nil or current <= chain then
            redis.call('SET', KEYS[1], chain + 1)
            return chain
        else
            return redis.call('INCR', KEYS[1]) - 1
        end
        """.trimIndent(),
        Long::class.java
    )

    /**
     * ETH 네이티브 코인 전송
     *
     * 지정된 주소로 ETH를 전송하고 결과를 MongoDB에 기록한다.
     * 트랜잭션은 legacy 방식(gasPrice 기반)으로 서명된다.
     *
     * @param userId 요청 사용자 ID
     * @param walletId 출금에 사용할 지갑 ID
     * @param toAddress 수신 주소 (0x 형식)
     * @param amount 전송 금액 (wei 단위)
     * @param tokenAddress ERC20 컨트랙트 주소 (null이면 ETH 네이티브 전송)
     * @return 트랜잭션 정보 [TransactionResponse]
     * @throws BusinessException BLOCKCHAIN_003: 지갑을 찾을 수 없는 경우
     * @throws BusinessException BLOCKCHAIN_007: 트랜잭션 브로드캐스트 실패
     */
    fun sendEth(
        userId: String,
        walletId: String,
        toAddress: String,
        amount: BigDecimal,
        tokenAddress: String? = null
    ): Mono<TransactionResponse> {
        logger.info(
            "[EthTransactionService] ETH 전송 요청: userId={}, walletId={}, to={}, amount={}",
            userId, walletId, toAddress, amount
        )

        return walletRepository.findById(walletId)
            .switchIfEmpty(Mono.error(BusinessException(ErrorCode.BLOCKCHAIN_003)))
            .flatMap { wallet ->
                // 지갑 소유자 검증
                if (wallet.userId != userId) {
                    return@flatMap Mono.error(BusinessException(ErrorCode.AUTH_002))
                }

                // 개인키 복호화 (AES-256-GCM)
                val privateKeyHex = cryptoUtils.decrypt(wallet.encryptedPrivateKey, masterKey, userId.toLong())
                val credentials = Credentials.create(privateKeyHex)
                val fromAddress = wallet.address

                // Nonce 획득 및 트랜잭션 브로드캐스트
                getNonceForAddress(fromAddress)
                    .flatMap { nonce ->
                        broadcastEthTransaction(
                            credentials = credentials,
                            nonce = nonce,
                            toAddress = toAddress,
                            amountWei = amount.toBigInteger(),
                            fromAddress = fromAddress,
                            tokenAddress = tokenAddress
                        )
                            .onErrorResume { error ->
                                // 브로드캐스트 실패 시 blind decrement 대신 blockchain에서 nonce 재동기화.
                                // 노드가 tx를 수신했다가 에러 반환한 경우 decrement 시 nonce gap 발생 위험.
                                logger.error(
                                    "[EthTransactionService] 브로드캐스트 실패, blockchain nonce 재동기화: address={}, nonce={}",
                                    fromAddress, nonce, error
                                )
                                resyncNonceFromBlockchain(fromAddress)
                                    .then(Mono.error(BusinessException(ErrorCode.BLOCKCHAIN_007, cause = error)))
                            }
                    }
                    .flatMap { (txHash, gasPrice, usedNonce) ->
                        // 수수료 계산: ERC20는 gasLimit이 더 높음
                        val gasLimit = if (tokenAddress != null) erc20TransferGasLimit else ethTransferGasLimit
                        val fee = gasLimit.multiply(gasPrice).toBigDecimal()

                        // TransactionHistory 저장
                        // nonce, gasPriceWei 저장: mempool drop 감지 시 RBF 재전송에 필요
                        val txHistory = TransactionHistory(
                            walletId = walletId,
                            userId = userId,
                            chain = Chain.ETH,
                            type = TransactionType.SEND,
                            fromAddress = fromAddress,
                            toAddress = toAddress,
                            amount = amount,
                            fee = fee,
                            txHash = txHash,
                            tokenAddress = tokenAddress,
                            status = TransactionStatus.PENDING,
                            nonce = usedNonce.toLong(),
                            gasPriceWei = gasPrice.toBigDecimal()
                        )
                        txHistoryRepository.save(txHistory)
                    }
                    .doOnSuccess { savedTx ->
                        // Kafka 이벤트 발행 (비동기, 실패해도 트랜잭션 응답에 영향 없음)
                        publishTransactionEvent(savedTx)
                        logger.info(
                            "[EthTransactionService] ETH 전송 완료: txHash={}", savedTx.txHash
                        )
                    }
                    .map { TransactionResponse.from(it) }
            }
    }

    // ─── 내부 구현 메서드 ────────────────────────────────────────────────────────

    /**
     * Blockchain pending nonce를 조회하고 Redis와 원자적으로 조정 후 발급한다. (내부 사용)
     *
     * ## 왜 매번 blockchain을 조회하는가
     * Redis 단독 관리 시 정합성이 깨지는 시나리오:
     * - Redis 재시작/key 증발: 잘못된 nonce로 재초기화
     * - 외부 지갑(MetaMask 등)에서 tx 발송: Redis가 이미 사용된 nonce를 반환
     * - Mempool tx drop: Redis nonce gap 미감지로 이후 tx 전체 stuck
     * - 노드 교체: 노드마다 pending count 다를 수 있음
     *
     * 은행 시스템에서 nonce 중복은 tx 즉시 거부, nonce gap은 자금 동결로 직결.
     * blockchain RPC 1회(~100-500ms) 추가 지연은 정확성 대비 허용 가능한 트레이드오프.
     *
     * ## 조정 로직 (Lua 원자 실행)
     * - redis ≤ blockchain: Redis가 stale → blockchain 기준 재설정 (외부 tx, 재시작 복구)
     * - redis > blockchain: Redis가 앞서 있음 (in-flight tx 정상) → Redis INCR 유지
     *
     * ## 처리 불가 케이스: mempool tx drop
     * nonce N이 drop되면 N+1 이후 tx가 stuck. TransactionMonitor 감지 + 수동 재처리 필요.
     *
     * Redis key: `eth:nonce:{address}`
     *
     * @param address ETH 지갑 주소
     * @return 이 트랜잭션에 사용할 Nonce [BigInteger]
     */
    private fun getNonceForAddress(address: String): Mono<BigInteger> {
        val nonceKey = "eth:nonce:$address"

        // 매번 blockchain pending nonce 조회 → Redis와 비교하여 항상 정합성 보장
        return Mono.fromCallable {
            web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                .send()
                .transactionCount
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { chainNonce ->
                logger.debug("[EthTransactionService] blockchain pending nonce: address={}, chainNonce={}", address, chainNonce)
                redisTemplate.execute(
                    nonceReconcileScript,
                    listOf(nonceKey),
                    listOf(chainNonce.toString())
                )
                    .next()
                    .map { BigInteger.valueOf(it) }
            }
    }

    /**
     * broadcast 실패 후 Redis nonce를 blockchain 기준으로 재동기화한다. (내부 사용)
     *
     * ## 왜 단순 decrement가 아닌가
     * broadcast 실패 원인이 다양하기 때문:
     * 1. 노드가 tx를 수신하고 처리했지만 응답 전 timeout → nonce 이미 소비됨
     *    → decrement 시 다음 tx가 동일 nonce 재사용 → 즉시 거부 (nonce too low)
     * 2. 노드가 tx를 거부 (insufficient funds, gas too low 등) → nonce 미소비
     *    → decrement 필요 없음 (blockchain pending count가 정확)
     *
     * 어느 경우든 blockchain pending count가 진실의 원천.
     * Lua 스크립트로 Redis를 blockchain에 맞게 조정 (단, Redis > blockchain이면 유지).
     *
     * @param address ETH 지갑 주소
     * @return 완료 [Mono]
     */
    private fun resyncNonceFromBlockchain(address: String): Mono<Void> {
        val nonceKey = "eth:nonce:$address"
        return Mono.fromCallable {
            web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                .send()
                .transactionCount
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { chainNonce ->
                redisTemplate.opsForValue().get(nonceKey)
                    .flatMap { current ->
                        val redisNonce = BigInteger(current)
                        if (redisNonce > chainNonce) {
                            // in-flight tx 없는 상태: blockchain 기준으로 재설정
                            logger.warn(
                                "[EthTransactionService] broadcast 실패 후 nonce 재동기화: address={}, redis={} → blockchain={}",
                                address, redisNonce, chainNonce
                            )
                            redisTemplate.opsForValue().set(nonceKey, chainNonce.toString()).then()
                        } else {
                            // Redis ≤ blockchain: 이미 정상 (다음 getNonceForAddress 호출 시 Lua가 처리)
                            Mono.empty<Void>()
                        }
                    }
                    .switchIfEmpty(Mono.empty())
            }
    }

    /**
     * 서명된 ETH/ERC20 트랜잭션을 빌드하고 브로드캐스트한다. (내부 사용)
     *
     * - ETH 네이티브: `createEtherTransaction` — gasLimit 21,000
     * - ERC20: `createFunctionCallTransaction` — transfer(address,uint256) ABI 인코딩,
     *   to=컨트랙트주소, value=0, gasLimit 65,000
     *
     * @param credentials 서명에 사용할 자격증명 (개인키 포함)
     * @param nonce 트랜잭션 Nonce
     * @param toAddress 수신 주소 (ETH: 수신자, ERC20: 토큰 컨트랙트)
     * @param amountWei 전송 금액 (wei / token amount)
     * @param fromAddress 송신 주소 (로깅용)
     * @param tokenAddress ERC20 컨트랙트 주소 (null이면 ETH 네이티브)
     * @return Triple(txHash, gasPrice, nonce) — nonce는 TransactionHistory 저장 및 RBF 재전송에 사용
     */
    private fun broadcastEthTransaction(
        credentials: Credentials,
        nonce: BigInteger,
        toAddress: String,
        amountWei: BigInteger,
        fromAddress: String,
        tokenAddress: String? = null
    ): Mono<Triple<String, BigInteger, BigInteger>> {
        return Mono.fromCallable {
            // 현재 네트워크 gasPrice 조회 (eth_gasPrice RPC)
            val gasPrice = web3j.ethGasPrice().send().gasPrice

            val rawTransaction = if (tokenAddress == null) {
                // ── ETH 네이티브 전송 ──────────────────────────────────────────
                RawTransaction.createEtherTransaction(
                    nonce,
                    gasPrice,
                    ethTransferGasLimit,
                    toAddress,
                    amountWei
                )
            } else {
                // ── ERC20 transfer(address,uint256) ───────────────────────────
                // ABI: transfer(address _to, uint256 _value) → selector a9059cbb
                val function = Function(
                    "transfer",
                    listOf(Address(toAddress), Uint256(amountWei)),
                    emptyList()
                )
                val encodedData = FunctionEncoder.encode(function)

                RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    erc20TransferGasLimit,
                    tokenAddress,   // to = 컨트랙트 주소
                    BigInteger.ZERO, // value = 0 (토큰 전송이므로 ETH 첨부 없음)
                    encodedData
                )
            }

            // 트랜잭션 서명 (EIP-155: chainId 포함하여 리플레이 공격 방지)
            val signedTx = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
            val hexSignedTx = Numeric.toHexString(signedTx)

            // 브로드캐스트 (eth_sendRawTransaction)
            val sendResult = web3j.ethSendRawTransaction(hexSignedTx).send()

            if (sendResult.hasError()) {
                val errorMsg = sendResult.error?.message ?: "알 수 없는 오류"
                logger.error(
                    "[EthTransactionService] 브로드캐스트 오류: from={}, tokenAddress={}, error={}",
                    fromAddress, tokenAddress, errorMsg
                )
                throw BusinessException(ErrorCode.BLOCKCHAIN_007, errorMsg)
            }

            val txHash = sendResult.transactionHash
            logger.info("[EthTransactionService] 브로드캐스트 성공: txHash={}, token={}", txHash, tokenAddress)

            // nonce를 함께 반환 — TransactionHistory 저장 및 RBF 재전송 시 필요
            Triple(txHash, gasPrice, nonce)
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Mempool에서 drop된 ETH 트랜잭션을 Replace-by-Fee(RBF)로 재전송한다.
     *
     * ## RBF 재전송 전략
     * - 동일 nonce로 새 트랜잭션 빌드: mempool은 같은 nonce를 가진 tx 중 gasPrice 높은 것을 채택
     * - 새 gasPrice = max(currentNetworkGasPrice, originalGasPrice * 1.1): 최소 10% 인상 보장
     * - TransactionHistory: 새 txHash, 새 gasPriceWei, retryCount+1로 업데이트
     *
     * ## 왜 getNonceForAddress()를 쓰지 않는가
     * tx가 drop된 시점에 blockchain pending count는 drop 이전 nonce로 돌아가 있음.
     * 새로운 nonce를 받으면 gap이 생겨 이후 tx들이 stuck.
     * 반드시 tx.nonce(원래 사용했던 nonce)를 그대로 재사용해야 함.
     *
     * @param tx drop된 [TransactionHistory] (nonce, gasPriceWei 필드 필요)
     * @return 업데이트된 [TransactionHistory] [Mono]
     */
    fun resubmitDroppedEthTx(tx: TransactionHistory): Mono<TransactionHistory> {
        val txNonce = tx.nonce
        val originalGasPriceWei = tx.gasPriceWei?.toBigInteger()

        if (txNonce == null || originalGasPriceWei == null) {
            logger.warn(
                "[EthTransactionService] RBF 재전송 불가 - nonce/gasPriceWei 없음: txId={}, txHash={}",
                tx.id, tx.txHash
            )
            return txHistoryRepository.save(
                tx.copy(
                    status = TransactionStatus.FAILED,
                    errorMessage = "mempool drop 감지: nonce/gasPriceWei 없어 재전송 불가"
                )
            )
        }

        return walletRepository.findById(tx.walletId)
            .switchIfEmpty(Mono.error(RuntimeException("RBF 재전송 지갑 없음: walletId=${tx.walletId}")))
            .flatMap { wallet ->
                Mono.fromCallable {
                    val privateKeyHex = cryptoUtils.decrypt(wallet.encryptedPrivateKey, masterKey, tx.userId.toLong())
                    val credentials = Credentials.create(privateKeyHex)

                    // 현재 네트워크 gasPrice 조회
                    val currentGasPrice = web3j.ethGasPrice().send().gasPrice

                    // 최소 10% 인상 보장: max(currentGasPrice, originalGasPrice * 1.1)
                    val bumpedGasPrice = currentGasPrice.max(
                        originalGasPriceWei.multiply(BigInteger.valueOf(11)).divide(BigInteger.TEN)
                    )

                    val gasLimit = if (tx.tokenAddress != null) erc20TransferGasLimit else ethTransferGasLimit

                    val rawTransaction = if (tx.tokenAddress == null) {
                        RawTransaction.createEtherTransaction(
                            BigInteger.valueOf(txNonce),
                            bumpedGasPrice,
                            gasLimit,
                            tx.toAddress,
                            tx.amount.toBigInteger()
                        )
                    } else {
                        // ERC20: transfer(address,uint256) ABI 재인코딩
                        val function = Function(
                            "transfer",
                            listOf(Address(tx.toAddress), Uint256(tx.amount.toBigInteger())),
                            emptyList()
                        )
                        val encodedData = FunctionEncoder.encode(function)
                        RawTransaction.createTransaction(
                            BigInteger.valueOf(txNonce),
                            bumpedGasPrice,
                            gasLimit,
                            tx.tokenAddress,
                            BigInteger.ZERO,
                            encodedData
                        )
                    }

                    val signedTx = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
                    val hexSignedTx = Numeric.toHexString(signedTx)

                    val sendResult = web3j.ethSendRawTransaction(hexSignedTx).send()
                    if (sendResult.hasError()) {
                        throw RuntimeException("ETH RBF 재전송 오류: ${sendResult.error?.message}")
                    }

                    Pair(sendResult.transactionHash, bumpedGasPrice)
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { (newTxHash, newGasPrice) ->
                        val gasLimit = if (tx.tokenAddress != null) erc20TransferGasLimit else ethTransferGasLimit
                        val newFee = gasLimit.multiply(newGasPrice).toBigDecimal()

                        logger.info(
                            "[EthTransactionService] ETH RBF 재전송 성공: oldTxHash={}, newTxHash={}, nonce={}, retry={}",
                            tx.txHash, newTxHash, txNonce, tx.retryCount + 1
                        )

                        txHistoryRepository.save(
                            tx.copy(
                                txHash = newTxHash,
                                gasPriceWei = newGasPrice.toBigDecimal(),
                                fee = newFee,
                                retryCount = tx.retryCount + 1
                            )
                        )
                    }
                    .onErrorResume { e ->
                        // 재전송 실패 — retryCount만 증가시켜 다음 주기에 재시도 또는 FAILED 판정
                        logger.error("[EthTransactionService] ETH RBF 재전송 실패: txId={}", tx.id, e)
                        txHistoryRepository.save(
                            tx.copy(
                                retryCount = tx.retryCount + 1,
                                errorMessage = "RBF 재전송 실패: ${e.message}"
                            )
                        )
                    }
            }
            .onErrorResume { e ->
                logger.error("[EthTransactionService] ETH RBF 지갑 조회 실패: walletId={}", tx.walletId, e)
                txHistoryRepository.save(
                    tx.copy(
                        retryCount = tx.retryCount + 1,
                        errorMessage = "RBF 재전송 실패 (지갑 조회 오류): ${e.message}"
                    )
                )
            }
    }

    /**
     * Kafka에 트랜잭션 완료 이벤트를 발행한다. (내부 사용)
     *
     * 비동기로 실행되며, 발행 실패해도 트랜잭션 응답에 영향을 주지 않는다.
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
            logger.error("[EthTransactionService] Kafka 이벤트 발행 실패: txId={}", tx.id, e)
        }
    }
}
