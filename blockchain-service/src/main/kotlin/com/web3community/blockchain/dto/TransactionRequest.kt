package com.web3community.blockchain.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * 단건 트랜잭션 전송 요청 DTO
 *
 * ## 개요
 * BTC 또는 ETH 단건 전송 요청에 사용되는 DTO.
 * 금액 단위는 체인에 따라 다르며, 컨트롤러에서 검증 후 서비스로 전달된다.
 *
 * ## 금액 단위
 * - BTC: satoshi 단위 (1 BTC = 100,000,000 satoshi)
 * - ETH 네이티브: wei 단위 (1 ETH = 10^18 wei)
 * - ERC20: 토큰 컨트랙트의 decimals에 따라 다름
 *
 * @property toAddress 수신 주소 (BTC: Bech32/P2PKH, ETH: 0x 형식)
 * @property amount 전송 금액 (BTC: satoshi, ETH: wei 단위의 BigDecimal)
 * @property tokenAddress ERC20 토큰 컨트랙트 주소 (null이면 네이티브 코인 전송)
 */
data class SendTransactionRequest(

    /**
     * 수신 주소
     * - BTC: Bech32 (bc1q...) 또는 P2PKH (1...) 형식
     * - ETH: 0x로 시작하는 체크섬 적용 주소
     */
    @field:NotBlank(message = "수신 주소는 필수입니다.")
    val toAddress: String,

    /**
     * 전송 금액
     * BTC: satoshi 단위 (예: 100000 = 0.001 BTC)
     * ETH: wei 단위 (예: 1000000000000000 = 0.001 ETH)
     * 최솟값: 1 (0 이하 불가)
     */
    @field:NotNull(message = "전송 금액은 필수입니다.")
    @field:DecimalMin(value = "1", message = "전송 금액은 1 이상이어야 합니다.")
    val amount: BigDecimal,

    /**
     * ERC20 토큰 컨트랙트 주소 (ETH 전용)
     * - null: ETH 네이티브 전송
     * - non-null: ERC20 토큰 전송 (컨트랙트 주소 0x...)
     * BTC 전송 시에는 항상 null이어야 한다.
     */
    val tokenAddress: String? = null
)

/**
 * 다중 수신자 일괄 전송 요청 DTO
 *
 * ## 개요
 * 여러 주소에 동시에 전송하는 배치 출금 요청.
 * - BTC: 하나의 트랜잭션에 여러 출력을 포함하여 수수료 절감 가능
 * - ETH: 각 수신자에게 개별 트랜잭션 전송 (배치 컨트랙트 사용 가능하나 단순화)
 *
 * @property recipients 수신자 및 금액 목록 (최대 100명)
 * @property tokenAddress ERC20 토큰 컨트랙트 주소 (null이면 네이티브 코인)
 */
data class BatchSendRequest(

    /**
     * 수신자 목록
     * 최소 1명, 최대 100명 (ErrorCode.BLOCKCHAIN_016)
     */
    @field:NotEmpty(message = "수신자 목록은 최소 1명 이상이어야 합니다.")
    @field:Size(max = 100, message = "수신자는 최대 100명까지 가능합니다.")
    @field:Valid
    val recipients: List<RecipientAmount>,

    /**
     * ERC20 토큰 컨트랙트 주소 (ETH 전용)
     * null이면 네이티브 코인 일괄 전송
     */
    val tokenAddress: String? = null
)

/**
 * 수신자 및 전송 금액 쌍
 *
 * [BatchSendRequest.recipients] 항목으로 사용된다.
 *
 * @property address 수신 블록체인 주소
 * @property amount 전송 금액 (BTC: satoshi, ETH: wei 단위)
 */
data class RecipientAmount(

    /** 수신 주소 */
    @field:NotBlank(message = "수신 주소는 필수입니다.")
    val address: String,

    /**
     * 전송 금액
     * 최솟값: 1 (0 이하 불가)
     */
    @field:NotNull(message = "전송 금액은 필수입니다.")
    @field:DecimalMin(value = "1", message = "전송 금액은 1 이상이어야 합니다.")
    val amount: BigDecimal
)
