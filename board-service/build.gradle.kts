/**
 * board-service 모듈 빌드 설정
 *
 * 기술 스택:
 * - Spring Boot WebFlux: Netty 기반 Non-blocking 리액티브 웹 프레임워크
 * - Spring Data MongoDB Reactive: 비동기 MongoDB 드라이버 (ReactiveMongoRepository)
 * - Spring Data Redis Reactive: 비동기 Redis 클라이언트 (Lettuce)
 * - Apache Kafka: 이벤트 스트리밍 플랫폼 (게시글 작성/삭제 이벤트 발행)
 * - Kotlin Coroutines: Reactor Mono/Flux를 suspend 함수로 래핑하여 가독성 향상
 * - common-module: 공통 응답 모델, 예외 처리, JWT 유틸리티 공유
 *
 * 왜 WebFlux + MongoDB Reactive?
 * - 게시판은 읽기 요청이 압도적으로 많아 Non-blocking I/O가 효과적
 * - MongoDB는 Document 모델로 게시글(태그, 반응 등 중첩 구조) 저장에 유리
 * - Reactive Streams 기반으로 Backpressure 처리 가능
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Spring Boot 플러그인: 실행 가능한 FAT JAR 생성, 내장 서버 포함
    id("org.springframework.boot") version "3.3.0"
    // Spring 의존성 관리: BOM 기반으로 Spring 라이브러리 버전 일관성 유지
    id("io.spring.dependency-management") version "1.1.5"
    // Kotlin JVM 플러그인: Kotlin 소스를 JVM 바이트코드로 컴파일
    kotlin("jvm") version "1.9.24"
    // Kotlin Spring 플러그인: @Configuration, @Component 등 Spring 클래스를 자동으로 open 처리
    // (Kotlin 클래스는 기본적으로 final이므로 Spring AOP 프록시 생성을 위해 필요)
    kotlin("plugin.spring") version "1.9.24"
}

group = "com.web3community"
version = "0.0.1-SNAPSHOT"

java {
    // Java 21 LTS: Virtual Threads, Record Patterns, Sequenced Collections 지원
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // ─── Common Module ──────────────────────────────────────────────────────────
    // 공통 응답 모델(ApiResponse), 예외 처리, JWT 유틸리티 등 공유 컴포넌트
    implementation(project(":common-module"))

    // ─── Spring WebFlux ─────────────────────────────────────────────────────────
    // Netty 기반 Non-blocking 리액티브 웹 프레임워크
    // 함수형 라우터(RouterFunction)와 핸들러(HandlerFunction) 패턴 지원
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // ─── Spring Data MongoDB Reactive ───────────────────────────────────────────
    // MongoDB 공식 Reactive Streams 드라이버 기반
    // ReactiveMongoRepository: Flux<T>, Mono<T> 반환 타입으로 비동기 CRUD 지원
    // ReactiveMongoTemplate: 복잡한 쿼리, 집계(Aggregation) 파이프라인 지원
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // ─── Spring Data Redis Reactive ─────────────────────────────────────────────
    // Lettuce 기반 비동기 Redis 클라이언트
    // ReactiveRedisTemplate: 게시글 상세 캐싱(5분), 좋아요/싫어요 카운트 캐싱에 사용
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Lettuce 커넥션 풀을 위한 Apache Commons Pool2
    // 다수의 동시 Redis 요청을 효율적으로 처리하기 위해 커넥션 풀링 활성화
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // ─── Apache Kafka ───────────────────────────────────────────────────────────
    // 게시글 작성/수정/삭제 이벤트를 Kafka 토픽으로 발행
    // 다른 서비스(알림, 검색 인덱싱 등)가 이벤트를 구독하여 처리
    implementation("org.springframework.kafka:spring-kafka")

    // ─── Kotlin Coroutines ──────────────────────────────────────────────────────
    // 코루틴 코어: suspend 함수, CoroutineScope, async/await 지원
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    // Reactor 브릿지: Mono/Flux ↔ suspend 함수 상호 변환 (awaitSingle, asFlow 등)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    // Kotlin Flow: Flux 대신 Flow를 사용하여 스트림 처리 가독성 향상
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")

    // ─── Kotlin ─────────────────────────────────────────────────────────────────
    // Kotlin 리플렉션: Spring이 Kotlin 클래스의 생성자/프로퍼티를 분석하는 데 필요
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Jackson Kotlin Module: data class 직렬화/역직렬화, null-safety 지원
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Jackson Datatype JSR310: LocalDateTime, Instant 등 Java Time API 직렬화 지원
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // ─── Spring Boot Actuator ───────────────────────────────────────────────────
    // /actuator/health: 서비스 헬스체크 (Docker, Kubernetes Liveness/Readiness Probe)
    // /actuator/metrics: Micrometer 기반 메트릭 수집 (Prometheus 연동 가능)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ─── Validation ─────────────────────────────────────────────────────────────
    // @Valid, @NotBlank, @Size 등 Bean Validation 어노테이션 지원
    // WebFlux에서는 ServerRequest.bodyToMono<T>() 후 수동으로 validate() 호출 필요
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ─── Test ───────────────────────────────────────────────────────────────────
    // Spring Boot Test: JUnit5, Mockito, AssertJ 통합 테스트 지원
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Reactor Test: StepVerifier로 Mono/Flux 스트림 단위 테스트
    testImplementation("io.projectreactor:reactor-test")
    // Embedded MongoDB: 테스트 시 실제 MongoDB 없이 인메모리 실행
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x:4.13.1")
    // Kafka Test: EmbeddedKafkaBroker로 통합 테스트
    testImplementation("org.springframework.kafka:spring-kafka-test")
    // Coroutines Test: runTest, TestCoroutineDispatcher 지원
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // -Xjsr305=strict: @Nullable, @NotNull 등 JSR-305 어노테이션을 Kotlin null-safety로 엄격 처리
        freeCompilerArgs += "-Xjsr305=strict"
        // JVM 21 바이트코드 생성 (Virtual Threads 등 최신 기능 활용)
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    // JUnit5 Platform 사용 (Spring Boot 3.x 기본 테스트 프레임워크)
    useJUnitPlatform()

    // 테스트 결과 로깅 설정
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}
