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
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
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

    /** 기본 가스 한도 (ETH 네이티브 전송) */
    private val ethTransferGasLimit = BigInteger.valueOf(21_000L)

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
                            fromAddress = fromAddress
                        )
                            .onErrorResume { error ->
                                // 브로드캐스트 실패 시 Nonce 반환
                                logger.error(
                                    "[EthTransactionService] 브로드캐스트 실패, Nonce 반환: address={}, nonce={}",
                                    fromAddress, nonce, error
                                )
                                decrementNonce(fromAddress)
                                    .then(Mono.error(BusinessException(ErrorCode.BLOCKCHAIN_007, cause = error)))
                            }
                    }
                    .flatMap { (txHash, gasPrice) ->
                        // 수수료 계산: gasLimit * gasPrice (wei 단위)
                        val fee = ethTransferGasLimit.multiply(gasPrice).toBigDecimal()

                        // TransactionHistory 저장
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
                            status = TransactionStatus.PENDING
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
     * Redis에서 Nonce를 원자적으로 획득한다. (내부 사용)
     *
     * Redis에 Nonce가 없으면 블록체인에서 현재 Nonce를 조회하여 초기화한다.
     * INCR 명령으로 원자적 증가를 보장하여 동시 요청 시 Nonce 중복 발급을 방지한다.
     *
     * Redis key: `eth:nonce:{address}`
     *
     * @param address ETH 지갑 주소
     * @return 이 트랜잭션에 사용할 Nonce [BigInteger]
     */
    private fun getNonceForAddress(address: String): Mono<BigInteger> {
        val nonceKey = "eth:nonce:$address"

        return redisTemplate.opsForValue().get(nonceKey)
            .flatMap { cachedNonce ->
                // Redis에 Nonce가 있으면 INCR로 원자적 증가 후 증가 전 값 반환
                redisTemplate.opsForValue().increment(nonceKey)
                    .map { newNonce ->
                        // INCR 후 반환값은 증가된 값이므로, 이 트랜잭션에는 증가 전 값 사용
                        BigInteger.valueOf(newNonce - 1)
                    }
            }
            .switchIfEmpty(
                // Redis에 Nonce가 없으면 블록체인에서 조회 후 초기화
                Mono.fromCallable {
                    web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                        .send()
                        .transactionCount
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { chainNonce ->
                        // 현재 Nonce를 Redis에 저장하고, 이 트랜잭션에는 현재 값 사용
                        // 다음 요청을 위해 chainNonce + 1을 저장
                        val nextNonce = chainNonce.add(BigInteger.ONE)
                        redisTemplate.opsForValue().set(nonceKey, nextNonce.toString())
                            .thenReturn(chainNonce)
                    }
            )
    }

    /**
     * Redis의 Nonce를 1 감소시킨다. (내부 사용 - 트랜잭션 실패 복구)
     *
     * 브로드캐스트 실패 시 증가된 Nonce를 반환하여 다음 트랜잭션이 올바른 Nonce를 사용하도록 한다.
     *
     * @param address ETH 지갑 주소
     * @return 완료 [Mono]
     */
    private fun decrementNonce(address: String): Mono<Long> {
        val nonceKey = "eth:nonce:$address"
        return redisTemplate.opsForValue().decrement(nonceKey)
            .doOnNext { value -> logger.debug("[EthTransactionService] Nonce 반환: address={}, value={}", address, value) }
    }

    /**
     * 서명된 ETH 트랜잭션을 빌드하고 브로드캐스트한다. (내부 사용)
     *
     * Legacy 트랜잭션 방식(gasPrice 기반)을 사용한다.
     * 현재 네트워크의 gasPrice를 조회하여 트랜잭션에 적용한다.
     *
     * @param credentials 서명에 사용할 자격증명 (개인키 포함)
     * @param nonce 트랜잭션 Nonce
     * @param toAddress 수신 주소
     * @param amountWei 전송 금액 (wei)
     * @param fromAddress 송신 주소
     * @return txHash와 gasPrice의 [Pair] [Mono]
     */
    private fun broadcastEthTransaction(
        credentials: Credentials,
        nonce: BigInteger,
        toAddress: String,
        amountWei: BigInteger,
        fromAddress: String
    ): Mono<Pair<String, BigInteger>> {
        return Mono.fromCallable {
            // 현재 네트워크 gasPrice 조회 (eth_gasPrice RPC)
            val gasPrice = web3j.ethGasPrice().send().gasPrice

            // Legacy 트랜잭션 빌드
            val rawTransaction = RawTransaction.createEtherTransaction(
                nonce,
                gasPrice,
                ethTransferGasLimit,
                toAddress,
                amountWei
            )

            // 트랜잭션 서명 (EIP-155: chainId를 포함하여 리플레이 공격 방지)
            val signedTx = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
            val hexSignedTx = Numeric.toHexString(signedTx)

            // 브로드캐스트 (eth_sendRawTransaction)
            val sendResult = web3j.ethSendRawTransaction(hexSignedTx).send()

            if (sendResult.hasError()) {
                val errorMsg = sendResult.error?.message ?: "알 수 없는 오류"
                logger.error(
                    "[EthTransactionService] 브로드캐스트 오류: from={}, error={}",
                    fromAddress, errorMsg
                )
                throw BusinessException(ErrorCode.BLOCKCHAIN_007, errorMsg)
            }

            val txHash = sendResult.transactionHash
            logger.info("[EthTransactionService] 브로드캐스트 성공: txHash={}", txHash)

            Pair(txHash, gasPrice)
        }.subscribeOn(Schedulers.boundedElastic())
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
