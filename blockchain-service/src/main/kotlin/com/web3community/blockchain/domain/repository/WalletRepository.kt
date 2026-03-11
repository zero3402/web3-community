package com.web3community.blockchain.domain.repository

import com.web3community.blockchain.domain.document.Chain
import com.web3community.blockchain.domain.document.Wallet
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 지갑 MongoDB 리포지토리
 *
 * ## 개요
 * [Wallet] 도큐먼트에 대한 CRUD 및 비즈니스 쿼리를 제공하는 리액티브 리포지토리.
 * Spring Data MongoDB의 메서드 이름 기반 쿼리 파생(Query Derivation)을 활용한다.
 *
 * ## 주요 쿼리 패턴
 * | 메서드                          | 목적                              |
 * |-------------------------------|----------------------------------|
 * | findByUserIdAndChain           | 특정 체인 지갑 단건 조회              |
 * | findAllByUserId               | 사용자의 모든 지갑 목록 조회           |
 * | existsByAddress               | 주소 중복 검사 (지갑 생성 시)         |
 * | existsByUserIdAndChain        | 지갑 중복 생성 방지 검사              |
 *
 * ## 인덱스 활용
 * - `findByUserIdAndChain`: Wallet.kt의 `idx_wallet_userId_chain` 복합 인덱스 사용
 * - `findAllByUserId`: `userId` 단일 인덱스 사용
 * - `existsByAddress`: `address` unique 인덱스 사용
 *
 * @see Wallet
 * @see Chain
 */
@Repository
interface WalletRepository : ReactiveMongoRepository<Wallet, String> {

    /**
     * 사용자 ID와 체인 타입으로 지갑 단건 조회
     *
     * 특정 체인(BTC 또는 ETH)의 지갑을 조회한다.
     * 사용자당 체인별 지갑은 유일하므로 [Mono]를 반환한다.
     *
     * @param userId 지갑 소유자의 사용자 ID
     * @param chain 블록체인 네트워크 타입 [Chain]
     * @return 조회된 [Wallet] 또는 빈 [Mono]
     */
    fun findByUserIdAndChain(userId: String, chain: Chain): Mono<Wallet>

    /**
     * 사용자 ID로 모든 지갑 목록 조회
     *
     * 사용자가 보유한 BTC, ETH 등 모든 체인의 지갑을 반환한다.
     *
     * @param userId 지갑 소유자의 사용자 ID
     * @return 사용자의 모든 [Wallet] [Flux]
     */
    fun findAllByUserId(userId: String): Flux<Wallet>

    /**
     * 주소 존재 여부 확인
     *
     * 지갑 생성 시 동일한 주소가 이미 존재하는지 검사한다.
     * 블록체인 주소는 전 세계적으로 유일해야 하므로, 주소 충돌을 방지하기 위해 사용한다.
     *
     * @param address 블록체인 공개 주소 (BTC: Bech32, ETH: 0x 형식)
     * @return 주소 존재 여부 [Mono]
     */
    fun existsByAddress(address: String): Mono<Boolean>

    /**
     * 사용자의 특정 체인 지갑 존재 여부 확인
     *
     * 동일 체인의 지갑을 중복 생성하지 않도록 방지하는 검사에 사용된다.
     * 사용자당 체인별 지갑은 하나만 허용된다.
     *
     * @param userId 지갑 소유자의 사용자 ID
     * @param chain 블록체인 네트워크 타입 [Chain]
     * @return 지갑 존재 여부 [Mono]
     */
    fun existsByUserIdAndChain(userId: String, chain: Chain): Mono<Boolean>
}
