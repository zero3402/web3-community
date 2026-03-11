package com.web3community.blockchain.service

import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.domain.document.Wallet
import com.web3community.blockchain.domain.document.WalletStatus
import com.web3community.blockchain.domain.repository.WalletRepository
import com.web3community.blockchain.dto.WalletResponse
import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import com.web3community.common.security.CryptoUtils
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.script.Script
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 블록체인 지갑 생성 및 조회 서비스
 *
 * ## 개요
 * BTC 및 ETH 지갑의 키 쌍 생성, 주소 파생, 개인키 암호화, 저장을 담당하는 리액티브 서비스.
 * 모든 메서드는 [Mono] 또는 [Flux]를 반환하며, 논블로킹 방식으로 동작한다.
 *
 * ## 지갑 생성 흐름
 * ```
 * 요청 수신 (userId, chain)
 *     ↓ 중복 지갑 확인 (existsByUserIdAndChain)
 *     ↓ 체인별 키 쌍 생성 (ECKey / Keys.createEcKeyPair)
 *     ↓ 주소 파생 (P2WPKH Bech32 / Keccak256)
 *     ↓ 개인키 AES-256-GCM 암호화 (CryptoUtils)
 *     ↓ MongoDB 저장 (WalletRepository)
 *     ↓ WalletResponse 반환 (개인키 제외)
 * ```
 *
 * ## 보안 원칙
 * - 개인키는 메모리에서만 사용하고 암호화 후 즉시 해제
 * - 암호화된 개인키만 DB에 저장 (평문 저장 절대 금지)
 * - API 응답에 개인키 또는 암호화된 개인키를 포함하지 않음
 *
 * @param walletRepository 지갑 MongoDB 리포지토리
 * @param cryptoUtils AES-256-GCM 암호화/복호화 유틸리티 (common-module)
 * @param networkParameters BitcoinJ 네트워크 파라미터 (BitcoinConfig)
 */
@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val cryptoUtils: CryptoUtils,
    private val networkParameters: NetworkParameters
) {

    private val logger = LoggerFactory.getLogger(WalletService::class.java)

    /** 개인키 암호화에 사용하는 마스터 키 (application.yml에서 주입) */
    @Value("\${blockchain.crypto.master-key}")
    private lateinit var masterKey: String

    /**
     * 새 블록체인 지갑 생성
     *
     * 지정된 체인(BTC 또는 ETH)의 새 지갑을 생성하고 MongoDB에 저장한다.
     * 이미 동일 체인의 지갑이 존재하면 [BusinessException] (BLOCKCHAIN_002)을 발생시킨다.
     *
     * ### BTC 지갑 생성
     * - `ECKey.create()`: secp256k1 랜덤 키 쌍 생성
     * - `Address.fromKey()`: P2WPKH (Native SegWit, Bech32) 주소 파생
     * - derivationPath: "m/44'/0'/0'/0/0" (BIP44 표준)
     *
     * ### ETH 지갑 생성
     * - `Keys.createEcKeyPair()`: secp256k1 랜덤 키 쌍 생성
     * - `Keys.toChecksumAddress()`: EIP-55 체크섬 적용 주소 (0x + 40자)
     * - derivationPath: "m/44'/60'/0'/0/0" (BIP44 표준)
     *
     * @param userId 지갑 소유자의 사용자 ID
     * @param chain 생성할 블록체인 네트워크 타입 [Chain]
     * @return 생성된 지갑 정보 [WalletResponse] (개인키 제외)
     * @throws BusinessException BLOCKCHAIN_002: 동일 체인의 지갑이 이미 존재하는 경우
     * @throws BusinessException BLOCKCHAIN_020: 지갑 생성 중 오류 발생
     */
    fun createWallet(userId: String, chain: Chain): Mono<WalletResponse> {
        logger.info("[WalletService] 지갑 생성 요청: userId={}, chain={}", userId, chain)

        return walletRepository.existsByUserIdAndChain(userId, chain)
            .flatMap { exists ->
                if (exists) {
                    logger.warn("[WalletService] 지갑 중복 생성 시도: userId={}, chain={}", userId, chain)
                    Mono.error(BusinessException(ErrorCode.BLOCKCHAIN_002))
                } else {
                    generateAndSaveWallet(userId, chain)
                }
            }
    }

    /**
     * 사용자의 모든 체인 지갑 목록 조회
     *
     * @param userId 지갑 소유자의 사용자 ID
     * @return 사용자가 보유한 모든 지갑의 [Flux]
     */
    fun getWallets(userId: String): Flux<WalletResponse> {
        logger.debug("[WalletService] 지갑 목록 조회: userId={}", userId)
        return walletRepository.findAllByUserId(userId)
            .map { WalletResponse.from(it) }
    }

    /**
     * 사용자의 특정 체인 지갑 단건 조회
     *
     * @param userId 지갑 소유자의 사용자 ID
     * @param chain 조회할 블록체인 네트워크 타입 [Chain]
     * @return 조회된 지갑 정보 [WalletResponse]
     * @throws BusinessException BLOCKCHAIN_003: 지갑을 찾을 수 없는 경우
     */
    fun getWallet(userId: String, chain: Chain): Mono<WalletResponse> {
        logger.debug("[WalletService] 지갑 단건 조회: userId={}, chain={}", userId, chain)
        return walletRepository.findByUserIdAndChain(userId, chain)
            .map { WalletResponse.from(it) }
            .switchIfEmpty(Mono.error(BusinessException(ErrorCode.BLOCKCHAIN_003)))
    }

    // ─── 내부 구현 메서드 ────────────────────────────────────────────────────────

    /**
     * 체인 타입에 따라 키 쌍을 생성하고 지갑을 MongoDB에 저장한다. (내부 사용)
     *
     * @param userId 지갑 소유자 사용자 ID
     * @param chain 생성할 체인 타입
     * @return 저장된 [WalletResponse]
     */
    private fun generateAndSaveWallet(userId: String, chain: Chain): Mono<WalletResponse> {
        return Mono.fromCallable {
            when (chain) {
                Chain.BTC -> generateBtcWallet(userId)
                Chain.ETH -> generateEthWallet(userId)
            }
        }
            .onErrorMap { e ->
                if (e is BusinessException) e
                else {
                    logger.error("[WalletService] 지갑 생성 실패: userId={}, chain={}", userId, chain, e)
                    BusinessException(ErrorCode.BLOCKCHAIN_020, cause = e)
                }
            }
            .flatMap { wallet ->
                walletRepository.save(wallet)
                    .map { saved ->
                        logger.info("[WalletService] 지갑 저장 완료: id={}, address={}", saved.id, saved.address)
                        WalletResponse.from(saved)
                    }
            }
    }

    /**
     * BTC P2WPKH (Native SegWit) 지갑 생성 (내부 사용)
     *
     * BitcoinJ의 [ECKey]를 사용하여 secp256k1 키 쌍을 생성하고
     * Bech32 주소(bc1q...)를 파생한다.
     *
     * @param userId 지갑 소유자 사용자 ID (개인키 암호화 솔트로 사용)
     * @return 암호화된 개인키가 포함된 [Wallet] 도큐먼트 (미저장 상태)
     */
    private fun generateBtcWallet(userId: String): Wallet {
        // secp256k1 랜덤 키 쌍 생성
        val ecKey = ECKey()

        // P2WPKH (Native SegWit, Bech32) 주소 파생
        // 형식: bc1q{20바이트 pubKeyHash} (메인넷) 또는 tb1q... (테스트넷)
        val address = org.bitcoinj.core.Address.fromKey(
            networkParameters,
            ecKey,
            Script.ScriptType.P2WPKH
        ).toString()

        // 개인키를 hex 문자열로 변환 후 AES-256-GCM 암호화
        // userId.toLong()을 솔트로 사용하여 사용자별 고유 암호화 키 파생
        val privateKeyHex = ecKey.privateKeyAsHex
        val encryptedPrivateKey = cryptoUtils.encrypt(privateKeyHex, masterKey, userId.toLong())

        logger.debug("[WalletService] BTC 지갑 생성 완료: address={}", address)

        return Wallet(
            userId = userId,
            chain = Chain.BTC,
            address = address,
            encryptedPrivateKey = encryptedPrivateKey,
            derivationPath = "m/44'/0'/0'/0/0",
            status = WalletStatus.ACTIVE
        )
    }

    /**
     * ETH 지갑 생성 (내부 사용)
     *
     * Web3j의 [Keys]를 사용하여 secp256k1 키 쌍을 생성하고
     * EIP-55 체크섬이 적용된 0x 주소를 파생한다.
     *
     * @param userId 지갑 소유자 사용자 ID (개인키 암호화 솔트로 사용)
     * @return 암호화된 개인키가 포함된 [Wallet] 도큐먼트 (미저장 상태)
     */
    private fun generateEthWallet(userId: String): Wallet {
        // secp256k1 랜덤 키 쌍 생성
        val keyPair = Keys.createEcKeyPair()

        // Keccak256 해시 기반 20바이트 Ethereum 주소 파생 + EIP-55 체크섬 적용
        val address = Keys.toChecksumAddress("0x" + Keys.getAddress(keyPair))

        // 개인키를 hex 문자열로 변환 후 AES-256-GCM 암호화
        val privateKeyHex = Numeric.toHexStringNoPrefix(keyPair.privateKey)
        val encryptedPrivateKey = cryptoUtils.encrypt(privateKeyHex, masterKey, userId.toLong())

        logger.debug("[WalletService] ETH 지갑 생성 완료: address={}", address)

        return Wallet(
            userId = userId,
            chain = Chain.ETH,
            address = address,
            encryptedPrivateKey = encryptedPrivateKey,
            derivationPath = "m/44'/60'/0'/0/0",
            status = WalletStatus.ACTIVE
        )
    }
}
