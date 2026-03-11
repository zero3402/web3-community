/**
 * board-service 모듈 빌드 설정
 *
 * 기술 스택:
 * - Spring Boot Web MVC: Tomcat 기반 동기 블로킹 REST API 서버
 * - Spring Data JPA: MySQL 연동 (Hibernate ORM)
 * - Spring Data Redis: 게시글 상세 캐싱 (StringRedisTemplate)
 * - Apache Kafka: 게시글 작성/수정/삭제 이벤트 발행
 * - common-module: 공통 응답 모델, 예외 처리 공유
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    // Spring 어노테이션 클래스를 자동으로 open 처리 (AOP 프록시 생성)
    kotlin("plugin.spring") version "1.9.24"
    // JPA 엔티티 클래스의 no-arg 생성자 자동 생성
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
    // ─── Common Module ──────────────────────────────────────────────────────────
    implementation(project(":common-module"))

    // ─── Spring Web MVC ─────────────────────────────────────────────────────────
    // Tomcat 기반 동기 방식 REST API 서버
    implementation("org.springframework.boot:spring-boot-starter-web")

    // ─── Spring Data JPA ────────────────────────────────────────────────────────
    // Hibernate ORM 기반 MySQL 연동, @Entity, JpaRepository 지원
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // ─── MySQL Connector ────────────────────────────────────────────────────────
    runtimeOnly("com.mysql:mysql-connector-j")

    // ─── Spring Data Redis ──────────────────────────────────────────────────────
    // 게시글 상세 캐싱 (5분 TTL), Lettuce 클라이언트 기본 내장
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Lettuce 커넥션 풀 설정
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // ─── Apache Kafka ───────────────────────────────────────────────────────────
    // 게시글 CRUD 이벤트를 Kafka 토픽으로 발행
    implementation("org.springframework.kafka:spring-kafka")

    // ─── Spring Boot Actuator ───────────────────────────────────────────────────
    // /actuator/health: Docker/Kubernetes Liveness/Readiness Probe
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ─── Validation ─────────────────────────────────────────────────────────────
    // @Valid, @NotBlank, @Size 등 Bean Validation 지원
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ─── Kotlin ─────────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // ─── Test ───────────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
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
