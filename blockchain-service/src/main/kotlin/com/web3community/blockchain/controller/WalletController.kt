package com.web3community.blockchain.controller

import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.dto.WalletResponse
import com.web3community.blockchain.service.WalletService
import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import com.web3community.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * 블록체인 지갑 관련 API 컨트롤러
 *
 * ## 개요
 * BTC/ETH 지갑 생성 및 조회 API를 제공한다.
 * API Gateway에서 JWT 인증 후 `X-User-Id` 헤더를 통해 사용자 ID를 전달받는다.
 *
 * ## 엔드포인트
 * | Method | Path                    | 설명                      |
 * |--------|-------------------------|--------------------------|
 * | POST   | /wallets/{chain}        | 새 지갑 생성 (BTC 또는 ETH) |
 * | GET    | /wallets                | 사용자 지갑 목록 전체 조회    |
 * | GET    | /wallets/{chain}        | 특정 체인 지갑 단건 조회      |
 *
 * ## 보안
 * - 모든 엔드포인트는 JWT 인증 필요 (API Gateway에서 처리)
 * - `X-User-Id` 헤더: API Gateway가 JWT에서 추출하여 전달
 * - 개인키는 어떤 응답에도 포함되지 않음
 *
 * @param walletService 지갑 생성 및 조회 서비스
 */
@RestController
@RequestMapping("/wallets")
class WalletController(
    private val walletService: WalletService
) {

    private val logger = LoggerFactory.getLogger(WalletController::class.java)

    /**
     * 새 블록체인 지갑 생성
     *
     * 지정된 체인(BTC 또는 ETH)의 지갑을 생성한다.
     * 이미 해당 체인의 지갑이 있으면 409 CONFLICT를 반환한다.
     *
     * ### 요청 예시
     * ```
     * POST /wallets/ETH
     * X-User-Id: 42
     * ```
     *
     * ### 응답 예시 (201 Created)
     * ```json
     * {
     *   "success": true,
     *   "code": 201,
     *   "message": "지갑 생성 완료",
     *   "data": {
     *     "id": "65a1...",
     *     "userId": "42",
     *     "chain": "ETH",
     *     "address": "0x1234...",
     *     "status": "ACTIVE",
     *     "createdAt": "2024-01-01T00:00:00"
     *   }
     * }
     * ```
     *
     * @param userId API Gateway가 주입하는 인증된 사용자 ID
     * @param chainStr 생성할 체인 타입 ("BTC" 또는 "ETH", 대소문자 무관)
     * @return 201 Created: 생성된 지갑 정보 / 409: 이미 존재
     */
    @PostMapping("/{chain}")
    fun createWallet(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable("chain") chainStr: String
    ): Mono<ResponseEntity<ApiResponse<WalletResponse>>> {
        logger.info("[WalletController] 지갑 생성 요청: userId={}, chain={}", userId, chainStr)

        val chain = parseChain(chainStr)

        return walletService.createWallet(userId, chain)
            .map { wallet ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(wallet, 201, "지갑 생성 완료"))
            }
    }

    /**
     * 사용자의 모든 지갑 목록 조회
     *
     * 사용자가 보유한 BTC, ETH 등 모든 체인의 지갑 목록을 반환한다.
     *
     * ### 요청 예시
     * ```
     * GET /wallets
     * X-User-Id: 42
     * ```
     *
     * @param userId API Gateway가 주입하는 인증된 사용자 ID
     * @return 200 OK: 지갑 목록 (빈 배열 가능)
     */
    @GetMapping
    fun getWallets(
        @RequestHeader("X-User-Id") userId: String
    ): Mono<ResponseEntity<ApiResponse<List<WalletResponse>>>> {
        logger.debug("[WalletController] 지갑 목록 조회: userId={}", userId)

        return walletService.getWallets(userId)
            .collectList()
            .map { wallets ->
                ResponseEntity.ok(ApiResponse.success(wallets, "지갑 목록 조회 완료"))
            }
    }

    /**
     * 특정 체인 지갑 단건 조회
     *
     * 사용자의 특정 체인(BTC 또는 ETH) 지갑을 조회한다.
     *
     * ### 요청 예시
     * ```
     * GET /wallets/BTC
     * X-User-Id: 42
     * ```
     *
     * @param userId API Gateway가 주입하는 인증된 사용자 ID
     * @param chainStr 조회할 체인 타입 ("BTC" 또는 "ETH", 대소문자 무관)
     * @return 200 OK: 지갑 정보 / 404: 지갑 없음
     */
    @GetMapping("/{chain}")
    fun getWallet(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable("chain") chainStr: String
    ): Mono<ResponseEntity<ApiResponse<WalletResponse>>> {
        logger.debug("[WalletController] 지갑 단건 조회: userId={}, chain={}", userId, chainStr)

        val chain = parseChain(chainStr)

        return walletService.getWallet(userId, chain)
            .map { wallet ->
                ResponseEntity.ok(ApiResponse.success(wallet, "지갑 조회 완료"))
            }
    }

    // ─── 내부 유틸리티 ────────────────────────────────────────────────────────────

    /**
     * 문자열을 [Chain] enum으로 변환한다. (내부 사용)
     *
     * 대소문자 구분 없이 변환하며, 지원하지 않는 값이면 [BusinessException]을 발생시킨다.
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
