/**
 * build.gradle.kts - 루트 Gradle 빌드 파일
 *
 * 전체 멀티모듈 프로젝트의 공통 빌드 설정을 정의합니다.
 * 모든 서브모듈에 적용되는 플러그인, 의존성, 컴파일 옵션을 관리합니다.
 *
 * 기술 스택:
 * - Kotlin 1.9.x: 정적 타입 언어, Java 상호운용성, 간결한 문법
 * - Spring Boot 3.2.x: 자동 설정, 내장 서버, 프로덕션 준비 기능
 * - Java 17 (LTS): 최신 LTS 버전, Virtual Threads 지원
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ============================================================
// 플러그인 버전 상수 정의
// ============================================================

// Spring Boot 버전 (3.x는 Spring Framework 6.x, Jakarta EE 9+ 기반)
val springBootVersion = "3.2.3"

// Spring Dependency Management (BOM 기반 의존성 버전 관리)
val springDependencyManagementVersion = "1.1.4"

// Kotlin 버전
val kotlinVersion = "1.9.22"

// ============================================================
// 빌드 스크립트 플러그인 클래스패스 설정
// ============================================================
plugins {
    // Kotlin JVM 플러그인: Kotlin 소스 컴파일 지원
    kotlin("jvm") version "1.9.22" apply false

    // Kotlin Spring 플러그인: Spring 컴포넌트 클래스를 자동으로 open으로 만들어줌
    // (Kotlin 클래스는 기본적으로 final이라 Spring AOP 프록시 생성 시 필요)
    kotlin("plugin.spring") version "1.9.22" apply false

    // Kotlin JPA 플러그인: Entity 클래스의 기본 생성자 자동 생성
    kotlin("plugin.jpa") version "1.9.22" apply false

    // Spring Boot 플러그인: 실행 가능한 JAR 생성, 의존성 버전 관리
    id("org.springframework.boot") version "3.2.3" apply false

    // Spring 의존성 관리: BOM(Bill of Materials) 기반 버전 통일
    id("io.spring.dependency-management") version "1.1.4" apply false
}

// ============================================================
// 모든 서브모듈에 공통 적용되는 설정
// ============================================================
subprojects {

    // 모든 서브모듈에 공통 플러그인 적용
    apply {
        plugin("kotlin")
        plugin("kotlin-spring")
        plugin("io.spring.dependency-management")
    }

    // 프로젝트 그룹 및 버전 설정
    group = "com.web3community"
    version = "0.0.1-SNAPSHOT"

    // ============================================================
    // Java/Kotlin 소스 호환성 설정
    // Java 17 사용: Record, Sealed Classes, Pattern Matching 지원
    // ============================================================
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ============================================================
    // 의존성 저장소 설정
    // ============================================================
    repositories {
        // Maven Central: 대부분의 오픈소스 라이브러리
        mavenCentral()

        // Gradle Plugin Portal: Gradle 플러그인
        gradlePluginPortal()
    }

    // ============================================================
    // Spring BOM(Bill of Materials) 의존성 관리
    // 버전을 명시하지 않아도 Spring Boot가 호환 버전을 자동 선택
    // ============================================================
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    // ============================================================
    // 모든 서브모듈 공통 의존성
    // ============================================================
    dependencies {
        // Kotlin 표준 라이브러리 (JDK 8 확장 포함)
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        // Kotlin Reflection: Spring의 런타임 리플렉션에 필요
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")

        // Jackson Kotlin Module: Kotlin 데이터 클래스 직렬화/역직렬화 지원
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")

        // Spring Boot Test: JUnit5, MockK, AssertJ 포함
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")

        // Kotlin Coroutines Test: 코루틴 테스트 지원
        "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    }

    // ============================================================
    // Kotlin 컴파일러 옵션
    // ============================================================
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            // JVM 17 바이트코드 생성
            jvmTarget = "17"

            // Spring의 AOP 프록시 생성을 위해 필요한 컴파일러 플러그인
            // @Configuration, @Bean 등의 클래스를 open으로 처리
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",          // JSR-305 null-safety 어노테이션 엄격 모드
                "-Xemit-jvm-type-annotations" // JVM 타입 어노테이션 출력
            )
        }
    }

    // ============================================================
    // 테스트 설정
    // ============================================================
    tasks.withType<Test> {
        // JUnit5 플랫폼 사용 (Spring Boot 3.x 기본)
        useJUnitPlatform()

        // 테스트 결과 상세 출력
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
}
