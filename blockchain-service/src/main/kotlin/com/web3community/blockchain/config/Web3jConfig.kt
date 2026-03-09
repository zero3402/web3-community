package com.web3community.blockchain.config

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.util.concurrent.TimeUnit

/**
 * Web3j Ethereum 클라이언트 설정
 *
 * ## 개요
 * Infura, Alchemy 또는 자체 Ethereum 노드에 HTTP RPC로 연결하는
 * Web3j 클라이언트를 빈으로 등록한다.
 *
 * ## 연결 구조
 * ```
 * EthWalletService / EthTransactionService
 *     ↓
 * Web3j (HTTP RPC 클라이언트)
 *     ↓ OkHttpClient (커넥션 풀, 타임아웃, 재시도)
 * Ethereum 노드 (Infura / 자체 노드)
 *     ↓ JSON-RPC 2.0
 * eth_getBalance, eth_sendRawTransaction, eth_getTransactionCount ...
 * ```
 *
 * ## 타임아웃 전략
 * - 연결 타임아웃(10s): 노드와 TCP 연결 수립 시간
 * - 읽기 타임아웃(30s): eth_estimateGas 등 느린 RPC 호출 고려
 * - 쓰기 타임아웃(30s): 대형 트랜잭션 데이터 전송 고려
 *
 * ## 운영 주의사항
 * - ETH_RPC_URL에는 API 키가 포함되므로 환경변수로 관리
 * - 프로덕션에서는 노드 2개 이상으로 폴오버(failover) 구성 권장
 * - Web3j는 스레드 안전(thread-safe)하므로 싱글톤으로 사용
 */
@Configuration
class Web3jConfig {

    private val logger = LoggerFactory.getLogger(Web3jConfig::class.java)

    /** Ethereum 노드 RPC URL (Infura/Alchemy/자체 노드) */
    @Value("\${blockchain.ethereum.rpc-url}")
    private lateinit var ethRpcUrl: String

    /** HTTP 연결 타임아웃 (밀리초) */
    @Value("\${blockchain.ethereum.connection-timeout-ms:10000}")
    private var connectionTimeoutMs: Long = 10_000L

    /** HTTP 읽기 타임아웃 (밀리초) */
    @Value("\${blockchain.ethereum.read-timeout-ms:30000}")
    private var readTimeoutMs: Long = 30_000L

    /** HTTP 쓰기 타임아웃 (밀리초) */
    @Value("\${blockchain.ethereum.write-timeout-ms:30000}")
    private var writeTimeoutMs: Long = 30_000L

    /**
     * OkHttpClient 빈 등록
     *
     * Web3j 내부 HTTP 통신에 사용되는 클라이언트.
     * 커넥션 풀, 타임아웃, 로깅 인터셉터를 커스터마이징하여 등록한다.
     *
     * ### 커넥션 풀 설정
     * - maxIdleConnections=10: 유휴 상태로 유지할 최대 연결 수
     * - keepAliveDuration=5분: 유휴 연결을 살아있는 상태로 유지하는 시간
     *   (블록체인 노드는 연결 수립 비용이 크므로 재사용이 유리)
     *
     * @return OkHttpClient 인스턴스 (커스텀 설정 적용)
     */
    @Bean(name = ["ethOkHttpClient"])
    fun ethOkHttpClient(): OkHttpClient {
        // HTTP 요청/응답 로깅 인터셉터 (DEBUG 레벨에서만 활성화)
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            logger.debug("[Web3j HTTP] {}", message)
        }.apply {
            // BASIC: 메서드, URL, 상태 코드, 헤더 로깅 (BODY는 RPC 데이터가 크므로 제외)
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            // ── 타임아웃 설정 ──────────────────────────────────────────
            .connectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
            // ── 커넥션 풀 설정 ─────────────────────────────────────────
            .connectionPool(
                okhttp3.ConnectionPool(
                    10,                 // 최대 유휴 연결 수
                    5,                  // keepAlive 시간
                    TimeUnit.MINUTES    // keepAlive 시간 단위
                )
            )
            // ── 로깅 인터셉터 ──────────────────────────────────────────
            .addInterceptor(loggingInterceptor)
            // ── 재시도 설정 ────────────────────────────────────────────
            // OkHttp의 기본 재시도는 연결 실패에만 동작하며
            // 블록체인 노드 과부하 시 재시도는 상위 레이어에서 처리
            .retryOnConnectionFailure(true)
            .build()
            .also { logger.info("[Web3j] OkHttpClient 초기화 완료 (connectTimeout={}ms)", connectionTimeoutMs) }
    }

    /**
     * Web3j 클라이언트 빈 등록
     *
     * Ethereum 노드와 JSON-RPC 2.0 프로토콜로 통신하는 Web3j 인스턴스.
     * HttpService에 커스텀 OkHttpClient를 주입하여 연결 설정을 제어한다.
     *
     * ### 주요 RPC 메서드
     * - `eth_getBalance`: 계정 잔액 조회
     * - `eth_getTransactionCount`: Nonce 조회 (pending 포함)
     * - `eth_sendRawTransaction`: 서명된 트랜잭션 브로드캐스트
     * - `eth_estimateGas`: 가스 한도 추정
     * - `eth_maxPriorityFeePerGas`: EIP-1559 우선순위 수수료 조회
     * - `eth_getBlockByNumber`: 블록 정보 조회 (확인 수 계산)
     *
     * @param ethOkHttpClient 커스텀 설정이 적용된 OkHttpClient
     * @return Web3j 인스턴스 (싱글톤, 스레드 안전)
     */
    @Bean
    fun web3j(ethOkHttpClient: OkHttpClient): Web3j {
        // HttpService: HTTP JSON-RPC 연결 (OkHttp 클라이언트 재사용)
        val httpService = HttpService(
            ethRpcUrl,
            ethOkHttpClient,
            false  // Web3j가 OkHttpClient를 종료하지 않도록 설정 (빈 수명 주기 관리)
        )

        return Web3j.build(httpService).also {
            logger.info("[Web3j] Ethereum 노드 연결 완료: {}", ethRpcUrl.substringBefore("/v3/") + "/v3/***")
        }
    }
}
