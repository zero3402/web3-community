package com.web3community.blockchain

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Blockchain Service 메인 애플리케이션 클래스
 *
 * ## 역할
 * - Spring Boot 애플리케이션 진입점
 * - BTC/ETH 지갑 생성, 트랜잭션 처리, UTXO 관리, Nonce 관리
 *
 * ## 활성화된 기능
 * - [EnableReactiveMongoAuditing]: MongoDB Document의 createdAt/updatedAt 자동 관리
 * - [EnableScheduling]: UTXO 동기화, Nonce 반환 큐 처리 등 주기적 작업 실행
 *
 * ## 실행 방법
 * ```bash
 * # 환경변수 설정 후 실행
 * ETH_RPC_URL=https://mainnet.infura.io/v3/{key} \
 * BTC_RPC_URL=http://localhost:8332 \
 * ./gradlew :blockchain-service:bootRun
 * ```
 */
@SpringBootApplication
@EnableReactiveMongoAuditing  // @CreatedDate, @LastModifiedDate 자동 처리
@EnableScheduling             // @Scheduled 어노테이션 활성화 (UTXO 동기화 등)
class BlockchainApplication

/**
 * 애플리케이션 시작 함수
 *
 * Spring Boot의 [runApplication] 확장 함수를 사용하여 Kotlin 관용적 방식으로 실행.
 * 내장 Netty 서버로 시작 (포트: application.yml의 server.port 참조)
 */
fun main(args: Array<String>) {
    runApplication<BlockchainApplication>(*args)
}
