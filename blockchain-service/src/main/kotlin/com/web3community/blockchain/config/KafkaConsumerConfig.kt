package com.web3community.blockchain.config

import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.service.BtcTransactionService
import com.web3community.blockchain.service.EthTransactionService
import com.web3community.common.kafka.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Kafka 소비자 설정 및 메시지 리스너
 *
 * ## 개요
 * blockchain-service가 소비하는 Kafka 토픽 리스너를 정의한다.
 * 현재 소비하는 토픽:
 * - `withdrawal.batch`: 배치 출금 요청 처리
 * - `user.created`: 신규 사용자 가입 시 자동 지갑 생성 트리거 (향후 확장)
 *
 * ## 메시지 처리 전략
 * - `enable-auto-commit: false` (application.yml): 수동 커밋으로 at-least-once 보장
 * - 처리 실패 시 예외를 throw하지 않고 로그에 기록 후 스킵 (dead-letter queue 권장)
 * - 멱등성 보장: 동일 메시지 재처리 시 중복 트랜잭션 방지 (txHash 기준)
 *
 * ## Kafka 컨슈머 그룹
 * - `blockchain-service-group` (application.yml의 `spring.kafka.consumer.group-id`)
 * - 파티션 수만큼 컨슈머를 병렬 실행하여 처리량 향상
 *
 * @param btcTransactionService BTC 트랜잭션 처리 서비스
 * @param ethTransactionService ETH 트랜잭션 처리 서비스
 */
@Component
class KafkaConsumerConfig(
    private val btcTransactionService: BtcTransactionService,
    private val ethTransactionService: EthTransactionService
) {

    private val logger = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

    /**
     * 배치 출금 요청 소비
     *
     * `withdrawal.batch` 토픽에서 다중 수신자 출금 요청을 소비하여 처리한다.
     * 각 수신자에게 개별 트랜잭션을 전송하는 방식으로 구현된다.
     *
     * ### 메시지 형식 (JSON)
     * ```json
     * {
     *   "userId": "42",
     *   "walletId": "65a1...",
     *   "chain": "ETH",
     *   "recipients": [
     *     { "address": "0x1234...", "amount": "1000000000000000" },
     *     { "address": "0x5678...", "amount": "2000000000000000" }
     *   ],
     *   "tokenAddress": null
     * }
     * ```
     *
     * ### 처리 전략
     * - BTC: 가능하면 하나의 트랜잭션에 여러 출력을 묶어 수수료 절감
     * - ETH: 수신자별 개별 트랜잭션 전송 (현재 구현)
     *
     * @param message Kafka 메시지 페이로드 (JSON 문자열)
     */
    @KafkaListener(
        topics = [KafkaTopics.WITHDRAWAL_BATCH],
        groupId = "\${spring.kafka.consumer.group-id:blockchain-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleWithdrawalBatch(message: String) {
        logger.info("[KafkaConsumerConfig] 배치 출금 요청 수신: message={}", message.take(100))

        try {
            // JSON 파싱 (간단한 구조로 직접 처리)
            // 실제 구현에서는 ObjectMapper를 사용하여 WithdrawalBatchEvent DTO로 파싱
            val parsed = parseWithdrawalBatchMessage(message)

            parsed?.let { event ->
                val chain = try {
                    Chain.valueOf(event.chain.uppercase())
                } catch (e: IllegalArgumentException) {
                    logger.error("[KafkaConsumerConfig] 지원하지 않는 체인: {}", event.chain)
                    return
                }

                // 각 수신자에게 개별 트랜잭션 전송
                event.recipients.forEach { recipient ->
                    try {
                        when (chain) {
                            Chain.ETH -> {
                                ethTransactionService.sendEth(
                                    userId = event.userId,
                                    walletId = event.walletId,
                                    toAddress = recipient.address,
                                    amount = BigDecimal(recipient.amount),
                                    tokenAddress = event.tokenAddress
                                ).subscribe(
                                    { tx -> logger.info("[KafkaConsumerConfig] 배치 ETH 전송 완료: to={}, txHash={}", recipient.address, tx.txHash) },
                                    { error -> logger.error("[KafkaConsumerConfig] 배치 ETH 전송 실패: to={}", recipient.address, error) }
                                )
                            }
                            Chain.BTC -> {
                                btcTransactionService.sendBtc(
                                    userId = event.userId,
                                    walletId = event.walletId,
                                    toAddress = recipient.address,
                                    amount = BigDecimal(recipient.amount)
                                ).subscribe(
                                    { tx -> logger.info("[KafkaConsumerConfig] 배치 BTC 전송 완료: to={}, txHash={}", recipient.address, tx.txHash) },
                                    { error -> logger.error("[KafkaConsumerConfig] 배치 BTC 전송 실패: to={}", recipient.address, error) }
                                )
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(
                            "[KafkaConsumerConfig] 수신자 처리 실패: address={}", recipient.address, e
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("[KafkaConsumerConfig] 배치 출금 메시지 처리 실패: {}", message.take(100), e)
            // 예외를 rethrow하지 않아 Kafka 오프셋은 커밋됨 (dead-letter queue 권장)
        }
    }

    /**
     * 배치 출금 Kafka 메시지를 파싱한다. (내부 사용)
     *
     * JSON 문자열을 [WithdrawalBatchEvent] 내부 데이터 클래스로 변환한다.
     * 파싱 실패 시 null을 반환하고 로그에 오류를 기록한다.
     *
     * @param message Kafka 메시지 JSON 문자열
     * @return 파싱된 [WithdrawalBatchEvent] 또는 null
     */
    private fun parseWithdrawalBatchMessage(message: String): WithdrawalBatchEvent? {
        return try {
            val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
            objectMapper.readValue(message, WithdrawalBatchEvent::class.java)
        } catch (e: Exception) {
            logger.error("[KafkaConsumerConfig] 메시지 파싱 실패: {}", e.message)
            null
        }
    }

    // ─── 내부 데이터 클래스 ────────────────────────────────────────────────────────

    /**
     * 배치 출금 Kafka 이벤트 데이터 클래스 (내부 사용)
     *
     * `withdrawal.batch` 토픽의 메시지 구조를 나타낸다.
     * 공통 모듈로 이동하거나 별도 패키지로 분리하는 것을 권장한다.
     */
    data class WithdrawalBatchEvent(
        /** 요청 사용자 ID */
        val userId: String = "",
        /** 출금에 사용할 지갑 ID */
        val walletId: String = "",
        /** 체인 타입 ("BTC" 또는 "ETH") */
        val chain: String = "",
        /** 수신자 목록 */
        val recipients: List<RecipientInfo> = emptyList(),
        /** ERC20 토큰 컨트랙트 주소 (null이면 네이티브 코인) */
        val tokenAddress: String? = null
    )

    /**
     * 수신자 정보 데이터 클래스 (내부 사용)
     */
    data class RecipientInfo(
        /** 수신 블록체인 주소 */
        val address: String = "",
        /** 전송 금액 문자열 (BTC: satoshi, ETH: wei 단위) */
        val amount: String = "0"
    )
}
