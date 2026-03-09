/**
 * Auth Service 모듈 빌드 설정
 *
 * 인증/인가 전담 서비스로 Spring MVC(Tomcat) 기반으로 동작한다.
 * - Spring Security + JWT: 토큰 발급 및 검증
 * - Spring OAuth2 Client: Google/Naver/Kakao 소셜 로그인
 * - Spring Data Redis: Refresh Token 저장 (TTL 7일)
 * - OpenFeign: User Service HTTP 호출
 * - Kafka Producer: 회원가입 이벤트 발행
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    // Spring 어노테이션 클래스(open 불가 문제)를 자동으로 open 처리
    kotlin("plugin.spring") version "1.9.24"
}

group = "com.web3community"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

// Spring Cloud 버전 관리 (BOM)
extra["springCloudVersion"] = "2023.0.2"

dependencies {
    // ─── 공통 모듈 ───────────────────────────────────────────────────────────
    // JWT 유틸, 공통 예외, ApiResponse 등 공유 코드
    implementation(project(":common-module"))

    // ─── Spring Web MVC ───────────────────────────────────────────────────
    // 동기 블로킹 방식의 REST API 서버 (Tomcat 내장)
    // Auth 서비스는 I/O 집약적이지 않고 보안 처리가 주 목적이므로 MVC 선택
    implementation("org.springframework.boot:spring-boot-starter-web")

    // ─── Spring Security ──────────────────────────────────────────────────
    // JWT 기반 Stateless 인증/인가 처리
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ─── Spring Data Redis ────────────────────────────────────────────────
    // Refresh Token 저장소 (TTL 기반 자동 만료)
    // Lettuce 클라이언트 사용 (기본 내장)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Lettuce 커넥션 풀 설정을 위한 Commons Pool2
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // ─── Spring OAuth2 Client ─────────────────────────────────────────────
    // Google / Naver / Kakao 소셜 로그인 OAuth2 Authorization Code Flow 처리
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // ─── Spring Cloud OpenFeign ───────────────────────────────────────────
    // User Service HTTP 클라이언트 (선언적 REST 클라이언트)
    // 이메일로 사용자 조회, 신규 사용자 생성 요청
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // ─── Spring Boot Actuator ─────────────────────────────────────────────
    // /actuator/health 등 운영 엔드포인트 제공
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ─── Kafka Producer ───────────────────────────────────────────────────
    // UserCreatedEvent 발행 (회원가입 완료 시 User Service에 비동기 알림)
    implementation("org.springframework.kafka:spring-kafka")

    // ─── JWT ──────────────────────────────────────────────────────────────
    // Access Token / Refresh Token 생성 및 검증
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // ─── Kotlin ───────────────────────────────────────────────────────────
    // Kotlin 리플렉션: Spring이 Kotlin 클래스를 처리하는 데 필요
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ─── JSON ─────────────────────────────────────────────────────────────
    // Jackson Kotlin 모듈: data class 직렬화/역직렬화 지원
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // ─── 테스트 ───────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

// Spring Cloud BOM: Spring Cloud 라이브러리 버전 일괄 관리
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // JSR-305 null-safety 어노테이션 엄격 모드: Spring의 @NonNull 등을 Kotlin 타입 시스템에 반영
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
