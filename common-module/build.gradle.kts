/**
 * common-module/build.gradle.kts - 공통 모듈 빌드 파일
 *
 * 이 모듈은 모든 MSA 서비스가 공유하는 공통 컴포넌트를 제공합니다.
 * Spring Boot 실행 가능 JAR이 아닌 라이브러리 JAR로 빌드됩니다.
 *
 * 제공 기능:
 * - 표준 API 응답 포맷 (ApiResponse, PageResponse)
 * - 공통 예외 처리 (ErrorCode, BusinessException)
 * - JWT 토큰 생성/검증 (RS256 알고리즘)
 * - AES-256-GCM 암호화 유틸리티 (블록체인 키 보호)
 * - Kafka 토픽 상수 및 이벤트 DTO
 * - Redis 캐시 키 상수
 */

plugins {
    // Kotlin JVM 플러그인
    kotlin("jvm")
    // Spring 컴포넌트 클래스를 open으로 만들어 AOP 프록시 생성 허용
    kotlin("plugin.spring")
}

// common-module은 실행 가능한 애플리케이션이 아닌 라이브러리이므로
// Spring Boot의 bootJar 태스크를 비활성화하고 일반 jar만 생성
tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

// ============================================================
// JWT 라이브러리 버전
// ============================================================
val jjwtVersion = "0.12.5"  // JJWT: Java JWT 구현체 (최신 0.12.x 권장)

dependencies {

    // ============================================================
    // Spring Boot Starters
    // ============================================================

    // Spring Security: 인증/인가 프레임워크
    // - JWT 필터 체인에서 SecurityContextHolder 사용
    // - PasswordEncoder (BCrypt) 제공
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Spring Web MVC: REST API 개발
    // - @RestControllerAdvice를 통한 전역 예외 처리
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Data Redis: Redis 연동
    // - Refresh Token 저장, 분산락(UTXO/Nonce) 구현
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring Kafka: Kafka 메시지 발행/소비
    // - 서비스 간 비동기 이벤트 처리
    implementation("org.springframework.kafka:spring-kafka")

    // Spring Boot Validation: 입력값 검증 (@Valid, @NotNull 등)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ============================================================
    // JWT (JSON Web Token)
    // ============================================================

    // JJWT API: JWT 생성/파싱 인터페이스 정의
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")

    // JJWT 구현체: JWT 서명, 검증 로직 (런타임 의존성)
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")

    // JJWT Jackson 연동: JWT 페이로드를 Jackson으로 직렬화/역직렬화
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // ============================================================
    // Jackson (JSON 직렬화/역직렬화)
    // ============================================================

    // Jackson Kotlin Module: Kotlin data class를 JSON으로 직렬화
    // - 기본 생성자 없는 데이터 클래스 지원
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Jackson Datatype JSR310: Java 8 날짜/시간 타입 지원
    // - LocalDateTime, ZonedDateTime 직렬화
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // ============================================================
    // 암호화 (AES-256-GCM)
    // ============================================================

    // Bouncy Castle: Java Security Provider 확장
    // - AES-GCM 모드, 고급 암호화 알고리즘 지원
    // - 블록체인 프라이빗 키 암호화에 사용
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")

    // ============================================================
    // 유틸리티
    // ============================================================

    // Kotlin Coroutines: 비동기/논블로킹 프로그래밍 지원
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // SLF4J: 로깅 추상화 레이어 (실제 구현체는 각 서비스에서 선택)
    implementation("org.slf4j:slf4j-api")

    // ============================================================
    // 테스트 의존성
    // ============================================================

    // Spring Boot Test: 통합 테스트 지원
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Spring Security Test: 보안 테스트 (@WithMockUser 등)
    testImplementation("org.springframework.security:spring-security-test")

    // MockK: Kotlin 친화적 모킹 라이브러리
    testImplementation("io.mockk:mockk:1.13.9")

    // Kotest: Kotlin 친화적 테스트 프레임워크
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}
