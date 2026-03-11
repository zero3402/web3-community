package com.web3community.blockchain.dto

import com.web3community.blockchain.domain.document.Wallet
import java.time.LocalDateTime

/**
 * 지갑 조회 응답 DTO
 *
 * ## 개요
 * [Wallet] 도큐먼트를 API 응답으로 변환한 DTO.
 * 클라이언트에게 안전하게 전달할 수 있는 공개 정보만 포함하며,
 * `encryptedPrivateKey` 등 민감 정보는 절대 포함하지 않는다.
 *
 * ## 보안 설계
 * - `encryptedPrivateKey`: 응답에서 제외 (암호화되어 있어도 노출 금지)
 * - `derivationPath`: 응답에서 제외 (지갑 구조 정보 비공개)
 * - `address`: 공개 주소만 노출 (블록체인 탐색기에서도 조회 가능한 공개 정보)
 *
 * @property id MongoDB 도큐먼트 ID
 * @property userId 지갑 소유자 사용자 ID
 * @property chain 블록체인 네트워크 타입 ("BTC" 또는 "ETH")
 * @property address 블록체인 공개 주소 (BTC: Bech32, ETH: 0x 형식)
 * @property status 지갑 상태 ("ACTIVE" 또는 "DISABLED")
 * @property createdAt 지갑 생성 시각
 */
data class WalletResponse(
    /** MongoDB 도큐먼트 ID */
    val id: String,

    /** 지갑 소유자 사용자 ID */
    val userId: String,

    /** 블록체인 네트워크 타입 ("BTC" 또는 "ETH") */
    val chain: String,

    /** 블록체인 공개 주소 */
    val address: String,

    /** 지갑 상태 */
    val status: String,

    /** 지갑 생성 시각 */
    val createdAt: LocalDateTime?
) {
    companion object {

        /**
         * [Wallet] 도큐먼트를 [WalletResponse] DTO로 변환
         *
         * 민감 정보(개인키, 파생 경로)는 제외하고 클라이언트에게 전달할 정보만 포함한다.
         *
         * @param wallet 변환할 [Wallet] 도큐먼트
         * @return 변환된 [WalletResponse]
         * @throws IllegalStateException wallet.id가 null인 경우 (저장되지 않은 도큐먼트)
         */
        fun from(wallet: Wallet): WalletResponse {
            return WalletResponse(
                id = requireNotNull(wallet.id) { "저장되지 않은 지갑 도큐먼트입니다. (id=null)" },
                userId = wallet.userId,
                chain = wallet.chain.name,
                address = wallet.address,
                status = wallet.status.name,
                createdAt = wallet.createdAt
            )
        }
    }
}
