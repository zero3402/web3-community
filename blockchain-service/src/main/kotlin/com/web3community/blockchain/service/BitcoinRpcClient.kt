package com.web3community.blockchain.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Bitcoin Core RPC 클라이언트
 *
 * ## 개요
 * Bitcoin Core 노드의 JSON-RPC API를 호출한다.
 * 현재 구현된 메서드:
 * - `listUnspent(address)`: 주소의 UTXO 목록 조회
 * - `sendRawTransaction(rawTxHex)`: 서명된 트랜잭션 브로드캐스트
 * - `getTransaction(txid)`: 트랜잭션 확인 수 조회
 *
 * ## 인증
 * Bitcoin Core는 HTTP Basic Auth (rpcuser:rpcpassword) 사용.
 * 운영 환경에서는 반드시 환경변수로 관리.
 *
 * ## 호출 형식 (JSON-RPC 1.0)
 * ```json
 * {"jsonrpc":"1.0","id":"req","method":"listunspent","params":[6,9999999,["bc1q..."]]}
 * ```
 */
@Component
class BitcoinRpcClient(
    @Value("\${blockchain.bitcoin.rpc-url:http://localhost:8332}")
    private val rpcUrl: String,

    @Value("\${blockchain.bitcoin.rpc-user:bitcoin}")
    private val rpcUser: String,

    @Value("\${blockchain.bitcoin.rpc-password:}")
    private val rpcPassword: String
) {

    private val logger = LoggerFactory.getLogger(BitcoinRpcClient::class.java)

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(rpcUrl)
        .defaultHeaders { headers ->
            headers.setBasicAuth(rpcUser, rpcPassword)
            headers.set("Content-Type", "application/json")
        }
        .build()

    /**
     * 주소의 UTXO 목록 조회 (listunspent)
     *
     * confirmations 기준: minConf=1 (unconfirmed 제외, 최소 1 confirmation 요구)
     * unconfirmed UTXO 포함 시 reorg로 roll-back될 수 있으므로 제외.
     *
     * @param address Bitcoin 주소
     * @param minConf 최소 confirmation 수 (기본 1)
     * @return UTXO 목록 [List<UnspentOutput>]
     */
    fun listUnspent(address: String, minConf: Int = 1): Mono<List<UnspentOutput>> {
        val body = mapOf(
            "jsonrpc" to "1.0",
            "id" to "listunspent",
            "method" to "listunspent",
            "params" to listOf(minConf, 9_999_999, listOf(address))
        )

        return webClient.post()
            .bodyValue(body)
            .retrieve()
            .bodyToMono(RpcResponse::class.java)
            .subscribeOn(Schedulers.boundedElastic())
            .map { response ->
                if (response.error != null) {
                    throw RuntimeException("Bitcoin RPC listunspent 오류: ${response.error}")
                }
                @Suppress("UNCHECKED_CAST")
                val result = response.result as? List<Map<String, Any>> ?: emptyList()
                result.map { utxo ->
                    UnspentOutput(
                        txid = utxo["txid"] as String,
                        vout = (utxo["vout"] as Number).toInt(),
                        // amount는 BTC 단위 → satoshi 변환 (1 BTC = 100,000,000 sat)
                        amountSatoshi = ((utxo["amount"] as Number).toDouble() * 100_000_000).toLong(),
                        scriptPubKey = utxo["scriptPubKey"] as? String ?: "",
                        confirmations = (utxo["confirmations"] as Number).toInt()
                    )
                }
            }
            .doOnError { e ->
                logger.error("[BitcoinRpcClient] listunspent 실패: address={}", address, e)
            }
    }

    /**
     * 서명된 raw transaction 브로드캐스트 (sendrawtransaction)
     *
     * @param rawTxHex 서명된 트랜잭션 hex
     * @return 브로드캐스트된 txid
     */
    fun sendRawTransaction(rawTxHex: String): Mono<String> {
        val body = mapOf(
            "jsonrpc" to "1.0",
            "id" to "sendrawtransaction",
            "method" to "sendrawtransaction",
            "params" to listOf(rawTxHex)
        )

        return webClient.post()
            .bodyValue(body)
            .retrieve()
            .bodyToMono(RpcResponse::class.java)
            .subscribeOn(Schedulers.boundedElastic())
            .map { response ->
                if (response.error != null) {
                    throw RuntimeException("Bitcoin RPC sendrawtransaction 오류: ${response.error}")
                }
                response.result as? String
                    ?: throw RuntimeException("sendrawtransaction: txid 반환값 없음")
            }
            .doOnNext { txid ->
                logger.info("[BitcoinRpcClient] 브로드캐스트 성공: txid={}", txid)
            }
            .doOnError { e ->
                logger.error("[BitcoinRpcClient] sendrawtransaction 실패", e)
            }
    }

    /**
     * 트랜잭션 confirmations 조회 (gettransaction)
     *
     * @param txid 트랜잭션 ID
     * @return confirmations 수 (-1이면 txid 미존재)
     */
    fun getTransactionConfirmations(txid: String): Mono<Int> {
        val body = mapOf(
            "jsonrpc" to "1.0",
            "id" to "gettransaction",
            "method" to "gettransaction",
            "params" to listOf(txid)
        )

        return webClient.post()
            .bodyValue(body)
            .retrieve()
            .bodyToMono(RpcResponse::class.java)
            .subscribeOn(Schedulers.boundedElastic())
            .map { response ->
                if (response.error != null) return@map -1
                @Suppress("UNCHECKED_CAST")
                val result = response.result as? Map<String, Any> ?: return@map -1
                (result["confirmations"] as? Number)?.toInt() ?: 0
            }
            .onErrorReturn(-1)
    }

    /** JSON-RPC 응답 래퍼 */
    data class RpcResponse(
        val result: Any? = null,
        val error: Any? = null,
        val id: String? = null
    )

    /** listunspent 결과 항목 */
    data class UnspentOutput(
        val txid: String,
        val vout: Int,
        val amountSatoshi: Long,
        val scriptPubKey: String,
        val confirmations: Int
    )
}
