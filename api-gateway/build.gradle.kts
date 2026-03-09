/**
 * API Gateway 모듈 빌드 설정
 *
 * Spring Cloud Gateway는 Webflux(Netty) 기반으로 동작하므로
 * spring-boot-starter-web(Tomcat) 과 동시에 사용하면 충돌이 발생한다.
 * 반드시 Webflux 의존성만 사용해야 한다.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    // Kotlin Spring 플러그인: @Configuration 등 Spring 어노테이션 클래스를 자동으로 open 처리
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
    // JWT 유틸, 공통 예외, 응답 모델 등 공유 코드
    implementation(project(":common-module"))

    // ─── Spring Cloud Gateway ─────────────────────────────────────────────
    // Webflux 기반 API Gateway. Tomcat 대신 Netty 서버를 사용한다.
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // Eureka 클라이언트: lb:// 스킴으로 서비스 디스커버리 기반 로드밸런싱 활성화
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // ─── Spring Boot Actuator ─────────────────────────────────────────────
    // /actuator/health, /actuator/metrics 등 운영 엔드포인트 제공
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ─── Redis (Reactive) ─────────────────────────────────────────────────
    // Rate Limiting에 사용할 Redis 연동. Webflux 환경이므로 Reactive 드라이버 사용.
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    // Lettuce 커넥션 풀 설정을 위한 Commons Pool2
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // ─── Kotlin ───────────────────────────────────────────────────────────
    // Kotlin 리플렉션: Spring이 Kotlin 클래스를 처리하는 데 필요
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Coroutines Core: 비동기 처리
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    // Coroutines Reactor 브릿지: suspend fun ↔ Mono/Flux 변환
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // ─── JSON ─────────────────────────────────────────────────────────────
    // JWT 파싱 시 Jackson을 사용하므로 Kotlin 모듈 추가
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // ─── JWT ──────────────────────────────────────────────────────────────
    // JWT 검증 라이브러리 (Auth 서버와 동일 버전 유지 필요)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // ─── 테스트 ───────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Webflux 테스트 지원 (WebTestClient)
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

// Spring Cloud BOM 임포트: 버전 충돌 없이 Spring Cloud 의존성 관리
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // Spring의 @Configuration 프록시 생성을 위해 -Xjsr305=strict 권장
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
