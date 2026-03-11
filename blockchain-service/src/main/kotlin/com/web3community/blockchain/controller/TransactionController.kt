package com.web3community.blockchain.controller

import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.domain.repository.TransactionHistoryRepository
import com.web3community.blockchain.dto.SendTransactionRequest
import com.web3community.blockchain.dto.TransactionResponse
import com.web3community.blockchain.service.BtcTransactionService
import com.web3community.blockchain.service.EthTransactionService
import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import com.web3community.common.response.ApiResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * 블록체인 트랜잭션 관련 API 컨트롤러
 *
 * ## 개요
 * BTC/ETH 트랜잭션 전송 및 히스토리 조회 API를 제공한다.
 * API Gateway에서 JWT 인증 후 `X-User-Id` 헤더를 통해 사용자 ID를 전달받는다.
 *
 * ## 엔드포인트
 * | Method | Path                          | 설명                   |
 * |--------|-------------------------------|------------------------|
 * | POST   | /transactions/{chain}/send    | 트랜잭션 전송             |
 * | GET    | /transactions/{chain}/history | 트랜잭션 히스토리 조회     |
 *
 * ## 보안
 * - 모든 엔드포인트는 JWT 인증 필요 (API Gateway에서 처리)
 * - 블록체인 전송은 Rate Limit이 더 엄격하게 적용됨 (API Gateway 설정 참조)
 *
 * @param btcService BTC 트랜잭션 처리 서비스
 * @param ethService ETH 트랜잭션 처리 서비스
 * @param txHistoryRepository 트랜잭션 히스토리 리포지토리 (히스토리 조회)
 */
@RestController
@RequestMapping("/transactions")
class TransactionController(
    private val btcService: BtcTransactionService,
    private val ethService: EthTransactionService,
    private val txHistoryRepository: TransactionHistoryRepository
) {

    private val logger = LoggerFactory.getLogger(TransactionController::class.java)

    /**
     * 블록체인 트랜잭션 전송
     *
     * 지정된 체인(BTC 또는 ETH)으로 코인을 전송한다.
     * 요청 본문의 `walletId`는 사용자 소유의 지갑이어야 하며,
     * 서비스 레이어에서 소유자 검증을 수행한다.
     *
     * ### 요청 예시 (ETH 전송)
     * ```
     * POST /transactions/ETH/send
     * X-User-Id: 42
     * Content-Type: application/json
     *
     * {
     *   "toAddress": "0xRecipientAddress...",
     *   "amount": "1000000000000000",
     *   "tokenAddress": null
     * }
     * ```
     *
     * ### 요청 예시 (BTC 전송)
     * ```
     * POST /transactions/BTC/send
     * X-User-Id: 42
     *
     * {
     *   "toAddress": "bc1qRecipientAddress...",
     *   "amount": "100000"
     * }
     * ```
     *
     * @param userId API Gateway가 주입하는 인증된 사용자 ID
     * @param walletId 출금에 사용할 지갑 ID (쿼리 파라미터)
     * @param chainStr 전송할 체인 타입 ("BTC" 또는 "ETH")
     * @param request 전송 요청 정보 (수신 주소, 금액, 토큰 주소)
     * @return 202 Accepted: 트랜잭션 정보 (PENDING 상태)
     */
    @PostMapping("/{chain}/send")
    fun sendTransaction(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam("walletId") walletId: String,
        @PathVariable("chain") chainStr: String,
        @Valid @RequestBody request: SendTransactionRequest
    ): Mono<ResponseEntity<ApiResponse<TransactionResponse>>> {
        logger.info(
            "[TransactionController] 트랜잭션 전송 요청: userId={}, chain={}, to={}, amount={}",
            userId, chainStr, request.toAddress, request.amount
        )

        val chain = parseChain(chainStr)

        val txMono = when (chain) {
            Chain.ETH -> ethService.sendEth(
                userId = userId,
                walletId = walletId,
                toAddress = request.toAddress,
                amount = request.amount,
                tokenAddress = request.tokenAddress
            )
            Chain.BTC -> btcService.sendBtc(
                userId = userId,
                walletId = walletId,
                toAddress = request.toAddress,
                amount = request.amount
            )
        }

        return txMono.map { txResponse ->
            ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(txResponse, 202, "트랜잭션 전송 완료 (PENDING)"))
        }
    }

    /**
     * 트랜잭션 히스토리 조회 (페이징)
     *
     * 사용자의 특정 체인 트랜잭션 내역을 최신순으로 반환한다.
     *
     * ### 요청 예시
     * ```
     * GET /transactions/ETH/history?userId=42&page=0&size=20
     * X-User-Id: 42
     * ```
     *
     * @param userId API Gateway가 주입하는 인증된 사용자 ID
     * @param chainStr 조회할 체인 타입 ("BTC" 또는 "ETH")
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 200 OK: 트랜잭션 히스토리 목록
     */
    @GetMapping("/{chain}/history")
    fun getTransactionHistory(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable("chain") chainStr: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Mono<ResponseEntity<ApiResponse<List<TransactionResponse>>>> {
        logger.debug(
            "[TransactionController] 트랜잭션 히스토리 조회: userId={}, chain={}, page={}, size={}",
            userId, chainStr, page, size
        )

        val chain = parseChain(chainStr)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        return txHistoryRepository.findByUserIdAndChainOrderByCreatedAtDesc(userId, chain, pageable)
            .map { TransactionResponse.from(it) }
            .collectList()
            .map { txList ->
                ResponseEntity.ok(ApiResponse.success(txList, "트랜잭션 히스토리 조회 완료"))
            }
    }

    // ─── 내부 유틸리티 ────────────────────────────────────────────────────────────

    /**
     * 문자열을 [Chain] enum으로 변환한다. (내부 사용)
     *
     * @param chainStr "BTC" 또는 "ETH" (대소문자 무관)
     * @return [Chain] enum 값
     * @throws BusinessException BLOCKCHAIN_001: 지원하지 않는 체인 타입
     */
    private fun parseChain(chainStr: String): Chain {
        return try {
            Chain.valueOf(chainStr.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BusinessException(ErrorCode.BLOCKCHAIN_001, "지원하지 않는 체인: $chainStr (허용값: BTC, ETH)")
        }
    }
}
