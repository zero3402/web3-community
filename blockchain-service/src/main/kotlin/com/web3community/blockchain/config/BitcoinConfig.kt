package com.web3community.blockchain.config

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.store.SPVBlockStore
import org.bitcoinj.wallet.Wallet
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

/**
 * BitcoinJ 네트워크 및 지갑 설정
 *
 * ## 개요
 * BitcoinJ 라이브러리를 사용한 Bitcoin 네트워크 연결 설정.
 * 네트워크 파라미터(메인넷/테스트넷)와 SPV 경량 노드 설정을 관리한다.
 *
 * ## Bitcoin 네트워크 타입
 * | 타입       | 설명                                  | 용도           |
 * |----------|-------------------------------------|--------------|
 * | MAINNET  | 실제 Bitcoin 네트워크                    | 운영 환경        |
 * | TESTNET3 | 공개 테스트 네트워크 (BTC 가치 없음)           | QA/스테이징      |
 * | REGTEST  | 로컬 회귀 테스트 네트워크 (블록 즉시 생성 가능)      | 개발/단위 테스트    |
 *
 * ## SPV(Simplified Payment Verification) 방식
 * 전체 블록체인을 다운로드하지 않고 블록 헤더만 저장하여
 * 트랜잭션을 검증하는 경량 클라이언트 방식.
 * - 저장 공간: 수 MB (Full Node 대비 수백 GB 절약)
 * - 트레이드오프: 블룸 필터를 통한 프라이버시 감소
 *
 * ## 운영 환경에서는 Bitcoin Core RPC 직접 연결 권장
 * WalletAppKit은 개발/테스트 환경에 적합하며,
 * 운영 환경에서는 Bitcoin Core의 listunspent, sendrawtransaction RPC를 직접 호출 권장.
 */
@Configuration
class BitcoinConfig {

    private val logger = LoggerFactory.getLogger(BitcoinConfig::class.java)

    /**
     * Bitcoin 네트워크 타입
     * application.yml의 `blockchain.bitcoin.network` 값 참조
     * 허용값: MAINNET, TESTNET3, REGTEST
     */
    @Value("\${blockchain.bitcoin.network:MAINNET}")
    private lateinit var networkType: String

    /**
     * WalletAppKit 데이터 저장 디렉토리
     * SPV 블록 체인 파일(.spvchain), 지갑 파일(.wallet) 저장 위치
     */
    @Value("\${blockchain.bitcoin.wallet-directory:/data/bitcoin-wallet}")
    private lateinit var walletDirectory: String

    /**
     * Bitcoin 네트워크 파라미터 빈 등록
     *
     * 네트워크 타입에 따라 올바른 파라미터 인스턴스를 반환한다.
     * NetworkParameters는 네트워크별 고유 값(Genesis 블록, 주소 접두사 등)을 포함한다.
     *
     * ### 네트워크별 주소 접두사
     * - MainNet P2PKH: 1 (예: 1A1zP1eP5QGefi2DMPTfTL5SLmv7Divf...)
     * - TestNet3 P2PKH: m 또는 n
     * - MainNet Bech32: bc1 (SegWit)
     * - TestNet3 Bech32: tb1 (SegWit)
     *
     * @return 선택된 네트워크의 [NetworkParameters]
     * @throws IllegalArgumentException 지원하지 않는 네트워크 타입 입력 시
     */
    @Bean
    fun networkParameters(): NetworkParameters {
        val params = when (networkType.uppercase()) {
            "MAINNET"  -> MainNetParams.get()   // 실제 Bitcoin 메인 네트워크
            "TESTNET3" -> TestNet3Params.get()  // 공개 테스트 네트워크
            "REGTEST"  -> RegTestParams.get()   // 로컬 회귀 테스트 네트워크
            else -> throw IllegalArgumentException(
                "지원하지 않는 Bitcoin 네트워크 타입: $networkType. " +
                "허용 값: MAINNET, TESTNET3, REGTEST"
            )
        }
        logger.info("[BitcoinJ] 네트워크 파라미터 설정: {} (id={})", networkType, params.id)
        return params
    }

    /**
     * SPV 블록 저장소 디렉토리 빈 등록
     *
     * SPVBlockStore가 블록 헤더를 저장할 파일 디렉토리.
     * 애플리케이션 시작 시 디렉토리가 없으면 자동으로 생성한다.
     *
     * ### 저장되는 파일
     * - `blockchain-service.spvchain`: SPV 블록 헤더 체인 파일
     * - `blockchain-service.wallet`: BitcoinJ 지갑 파일 (WalletAppKit 사용 시)
     *
     * @return 지갑 데이터 저장 디렉토리 [File]
     */
    @Bean(name = ["bitcoinWalletDir"])
    fun bitcoinWalletDirectory(): File {
        val dir = File(walletDirectory)
        if (!dir.exists()) {
            // 중간 디렉토리까지 모두 생성 (mkdir -p 와 동일)
            dir.mkdirs()
            logger.info("[BitcoinJ] 지갑 디렉토리 생성: {}", dir.absolutePath)
        } else {
            logger.info("[BitcoinJ] 지갑 디렉토리 사용: {}", dir.absolutePath)
        }
        return dir
    }

    /**
     * BitcoinJ 기본 지갑 빈 등록
     *
     * 서비스 운영에 사용할 기본 BitcoinJ 지갑 인스턴스.
     * HD 지갑 파생(BIP44)은 [com.web3community.blockchain.service.wallet.BtcWalletService]에서
     * 별도로 처리하므로, 이 빈은 주로 트랜잭션 서명 및 네트워크 파라미터 제공 용도로 사용된다.
     *
     * ### 지갑 생성 방식
     * - `Wallet.createDeterministic`: 결정론적 지갑 (BIP32/BIP44 호환)
     * - 스크립트 타입: P2WPKH (Native SegWit, Bech32 주소) - 수수료 효율 최고
     *
     * @param networkParameters Bitcoin 네트워크 파라미터
     * @return BitcoinJ [Wallet] 인스턴스
     */
    @Bean
    fun bitcoinWallet(networkParameters: NetworkParameters): Wallet {
        return Wallet.createDeterministic(
            networkParameters,
            org.bitcoinj.script.Script.ScriptType.P2WPKH  // Native SegWit (Bech32)
        ).also {
            logger.info("[BitcoinJ] 기본 지갑 생성 완료 (ScriptType: P2WPKH)")
        }
    }
}
