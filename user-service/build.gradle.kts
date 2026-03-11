/**
 * User Service 모듈 빌드 설정
 *
 * 사용자 정보 관리 전담 서비스로 Spring MVC(Tomcat) 기반으로 동작한다.
 * - Spring Data JPA + MySQL: 사용자 데이터 영속성
 * - Spring Kafka Consumer: auth-service의 UserCreatedEvent 소비
 * - Spring Data Redis: 캐시 (선택적 사용)
 * - Spring Boot Actuator: 운영 모니터링 엔드포인트
 *
 * 포트: 8082
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    // Spring 어노테이션 클래스(open 불가 문제)를 자동으로 open 처리
    kotlin("plugin.spring") version "1.9.24"
    // JPA Entity 클래스의 기본 생성자 자동 생성 (Kotlin 클래스는 기본 생성자 없음)
    kotlin("plugin.jpa") version "1.9.24"
}

group = "com.web3community"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // ─── 공통 모듈 ───────────────────────────────────────────────────────────
    // ApiResponse, BusinessException, ErrorCode, UserCreatedEvent, KafkaTopics 등
    implementation(project(":common-module"))

    // ─── Spring Web MVC ───────────────────────────────────────────────────
    // 동기 블로킹 방식의 REST API 서버 (Tomcat 내장)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // ─── Spring Data JPA ──────────────────────────────────────────────────
    // ORM 기반 MySQL 접근, Entity 관리, 트랜잭션 처리
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // ─── MySQL Connector ──────────────────────────────────────────────────
    // MySQL 8.x JDBC 드라이버
    runtimeOnly("com.mysql:mysql-connector-j")

    // ─── Spring Data Redis ────────────────────────────────────────────────
    // 사용자 프로필 캐시 (선택적 사용, 향후 확장용)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // ─── Spring Kafka Consumer ────────────────────────────────────────────
    // auth-service가 발행하는 UserCreatedEvent를 소비하여 사용자 DB에 저장
    implementation("org.springframework.kafka:spring-kafka")

    // ─── Spring Boot Validation ───────────────────────────────────────────
    // @Valid, @NotBlank, @Size 등 Bean Validation 2.0 지원
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ─── Spring Boot Actuator ─────────────────────────────────────────────
    // /actuator/health 등 운영 엔드포인트 제공 (쿠버네티스 헬스체크)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ─── Kotlin ───────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ─── JSON ─────────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // ─── 테스트 ───────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.mockk:mockk:1.13.9")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
