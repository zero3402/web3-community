/**
 * Blockchain Service 모듈 빌드 설정
 *
 * 블록체인 지갑 생성, 트랜잭션 전송, UTXO 관리 등 블록체인 관련 기능을 담당하는 서비스.
 *
 * 기술 스택 선택 이유:
 * - Spring Webflux: 블록체인 노드 RPC 호출은 I/O 집약적이므로 논블로킹 처리로 처리량 극대화
 * - MongoDB Reactive: 트랜잭션 히스토리, UTXO Set의 유연한 스키마 및 비동기 처리
 * - Redis Reactive: Nonce 원자적 관리, UTXO 분산 락, 잔액 캐시
 * - Kafka: 트랜잭션 이벤트 비동기 발행 (다른 서비스와 느슨한 결합)
 * - Web3j: ETH/ERC20 RPC 통신, 트랜잭션 서명
 * - BitcoinJ: BTC HD 지갑 생성, UTXO 처리, 트랜잭션 서명
 * - Kotlin Coroutines: Webflux와 함께 사용 시 비동기 코드를 동기 스타일로 작성 가능
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Spring Boot 플러그인: 실행 가능한 JAR 패키징, 의존성 버전 관리
    id("org.springframework.boot") version "3.3.0"

    // Spring 의존성 관리 플러그인: BOM 기반 버전 통일
    id("io.spring.dependency-management") version "1.1.5"

    // Kotlin JVM 플러그인
    kotlin("jvm") version "1.9.24"

    // Kotlin Spring 플러그인: Spring 컴포넌트 클래스를 자동으로 open 처리
    // (Kotlin 클래스는 기본 final이므로 AOP 프록시 생성에 필요)
    kotlin("plugin.spring") version "1.9.24"
}

group = "com.web3community"
version = "0.0.1-SNAPSHOT"

java {
    // Java 21 LTS: Virtual Threads, Record Patterns 지원
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()

    // BitcoinJ는 Maven Central에 있으나 일부 종속성이 JCenter에 있을 수 있어 추가
    maven { url = uri("https://jitpack.io") }
}

// Spring Cloud 버전 (BOM 기반 관리)
extra["springCloudVersion"] = "2023.0.2"

// Kotlin Coroutines 버전 (Webflux 연동 시 필요)
val coroutinesVersion = "1.8.1"

// Web3j 버전 (Ethereum RPC 클라이언트)
val web3jVersion = "4.10.3"

// BitcoinJ 버전 (Bitcoin 지갑 및 트랜잭션 처리)
val bitcoinjVersion = "0.16.2"

dependencies {

    // ─── 공통 모듈 ───────────────────────────────────────────────────────────
    // CryptoUtils (AES-256-GCM 암호화), 공통 예외, ApiResponse, Kafka 이벤트 DTO 등
    implementation(project(":common-module"))

    // ─── Spring Webflux ───────────────────────────────────────────────────
    // 논블로킹 I/O 기반 리액티브 웹 프레임워크 (Netty 내장)
    // 블록체인 노드 RPC 호출 시 스레드 블로킹 없이 높은 동시성 처리
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // ─── Spring Data MongoDB Reactive ─────────────────────────────────────
    // 비동기 MongoDB 드라이버 (MongoDB Reactive Streams Driver 기반)
    // 트랜잭션 히스토리, UTXO Set, 지갑 정보를 MongoDB에 저장
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // ─── Spring Data Redis Reactive ───────────────────────────────────────
    // Lettuce 기반 비동기 Redis 클라이언트
    // - Nonce 원자적 관리 (ETH)
    // - UTXO 분산 락 (BTC)
    // - 잔액 캐시 (TTL 기반)
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Lettuce 커넥션 풀 (Commons Pool2 필요)
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // ─── Kafka ────────────────────────────────────────────────────────────
    // 트랜잭션 이벤트 발행 (TransactionConfirmed, WithdrawalCompleted 등)
    // 배치 출금 요청 소비 (withdrawal.batch 토픽)
    implementation("org.springframework.kafka:spring-kafka")

    // ─── Web3j (Ethereum) ─────────────────────────────────────────────────
    // ETH/ERC20 지갑 생성, 트랜잭션 서명, RPC 노드 통신
    // - Wallet 생성: secp256k1 키쌍, Keccak256 주소 파생
    // - 트랜잭션: EIP-1559 타입 2 트랜잭션, ERC20 ABI 인코딩
    // - RPC: eth_getBalance, eth_sendRawTransaction, eth_getTransactionCount
    implementation("org.web3j:core:$web3jVersion")

    // ─── BitcoinJ (Bitcoin) ───────────────────────────────────────────────
    // BTC HD 지갑 생성, UTXO 처리, 트랜잭션 서명, 네트워크 통신
    // - BIP44 HD Wallet: m/44'/0'/0'/0/index
    // - 주소 타입: P2PKH, P2SH-P2WPKH, Bech32(P2WPKH)
    // - 트랜잭션 서명: SIGHASH_ALL
    implementation("org.bitcoinj:bitcoinj-core:$bitcoinjVersion") {
        // Netty와 버전 충돌 방지를 위해 제외
        exclude(group = "com.google.guava", module = "guava")
    }

    // Guava 명시적 버전 지정 (BitcoinJ와 Spring 간 충돌 해결)
    implementation("com.google.guava:guava:32.1.3-jre")

    // ─── Kotlin Coroutines ────────────────────────────────────────────────
    // Webflux(Reactor)와 Coroutines 연동: suspend 함수로 리액티브 코드 작성
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")  // Mono/Flux ↔ suspend 변환
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion") // Reactive Streams 연동

    // ─── Spring Boot Actuator ─────────────────────────────────────────────
    // /actuator/health, /actuator/metrics 등 운영 엔드포인트
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ─── Kotlin ───────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Jackson Datatype JSR310: LocalDateTime 등 Java 8 날짜 타입 직렬화
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // ─── OkHttp (Web3j RPC 클라이언트) ────────────────────────────────────
    // Web3j가 내부적으로 사용하는 HTTP 클라이언트
    // 타임아웃, 재시도, 커넥션 풀 설정 커스터마이징에 필요
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ─── 테스트 ───────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")                          // StepVerifier
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo:4.12.0")   // 임베디드 MongoDB
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",           // Spring @NonNull 등 JSR-305 어노테이션 엄격 모드
            "-Xopt-in=kotlin.RequiresOptIn" // OptIn 어노테이션 사용 허용 (Coroutines 실험 API)
        )
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
