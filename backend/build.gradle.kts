# =============================================================================
// 📋 Gradle 루트 설정 - 멀티 모듈 Spring Boot 프로젝트
// =============================================================================
// 설명: Web3 Community Platform의 루트 Gradle 설정
// 특징: 멀티 모듈, Spring Boot, Kotlin DSL, 의존성 관리
// 목적: 모든 마이크로서비스의 빌드 환경 설정
// =============================================================================

// =============================================================================
// 🔧 플러그인 설정
// =============================================================================
plugins {
    // Java/Spring Boot 플러그인
    id 'java'
    id 'org.springframework.boot' version '3.2.0' apply false
    id 'io.spring.dependency-management' version '1.1.4' apply false
    
    // Kotlin 플러그인
    id 'org.jetbrains.kotlin.jvm' version '1.9.20' apply false
    id 'org.jetbrains.kotlin.plugin.spring' version '1.9.20' apply false
    id 'org.jetbrains.kotlin.plugin.jpa' version '1.9.20' apply false
    
    // Docker 플러그인
    id 'com.palantir.docker' version '0.35.0'
    id 'com.github.ben-manes.versions' version '0.49.0'
}

// =============================================================================
// 📦 그룹 및 버전 설정
// =============================================================================
group = 'com.web3community'
version = '1.0.0'

// =============================================================================
// 📋 전체 프로젝트 설정 (allprojects에 공통적으로 적용)
// =============================================================================
allprojects {
    // Maven 리포지토리 설정
    repositories {
        mavenCentral()
        maven { url 'https://repo.spring.io/milestone' }
        maven { url 'https://repo.spring.io/snapshot' }
        maven { url 'https://repo.spring.io/libs-milestone' }
    }
    
    // 그룹 및 버전 설정
    group = rootProject.group
    version = rootProject.version
    
    // 적용할 플러그인
    apply plugin: 'java'
    apply plugin: 'org.jetbrains.kotlin.jvm'
    apply plugin: 'com.github.ben-manes.versions'
    
    // Java 설정
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    // Kotlin 설정
    kotlin {
        jvmToolchain(17)
        
        // 컴파일러 옵션
        compilerOptions {
            freeCompilerArgs += [
                '-Xjsr305=strict',
                '-Xjvm-default=all',
                '-opt-in=kotlin.RequiresOptIn'
            ]
        }
    }
    
    // 테스트 설정
    test {
        useJUnitPlatform()
        
        // JVM 옵션
        jvmArgs = [
            '--add-opens java.base=java.lang.reflect',
            '--add-opens java.base=java.util',
            '--add-opens java.sql=java.sql'
        ]
    }
    
    // 의존성 관리
    dependencies {
        // 테스트 의존성
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testImplementation 'io.projectreactor:reactor-test'
        testImplementation 'org.jetbrains.kotlin:kotlin-test'
        testImplementation 'org.testcontainers:junit-jupiter'
        testImplementation 'org.testcontainers:kafka'
        testImplementation 'org.testcontainers:mysql'
        testImplementation 'org.testcontainers:mongodb'
    }
    
    // Gradle 설정
    configurations {
        compileOnly {
            extendsFrom annotationProcessor
        }
    }
}

// =============================================================================
// 📦 서브 프로젝트 설정 (subprojects에 개별적으로 적용)
// =============================================================================
subprojects {
    // Spring Boot 플러그인 적용
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
    apply plugin: 'org.jetbrains.kotlin.plugin.spring'
    apply plugin: 'com.palantir.docker'
    
    // Spring Boot 설정
    springBoot {
        buildInfo()
        mainClass.set("${group}.${name}.Web3CommunityApplicationKt")
    }
    
    // 의존성 관리 설정
    dependencyManagement {
        imports {
            mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
            mavenBom "io.projectreactor:reactor-bom:2023.0.0"
            mavenBom "org.testcontainers:testcontainers-bom:1.19.1"
        }
    }
    
    // 공통 의존성
    dependencies {
        // Spring Boot WebFlux
        implementation 'org.springframework.boot:spring-boot-starter-webflux'
        implementation 'org.springframework.boot:spring-boot-starter-actuator'
        
        // Spring Data 관련
        implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
        implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
        implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
        implementation 'org.springframework.boot:spring-boot-starter-security'
        
        // Kotlin
        implementation 'org.jetbrains.kotlin:kotlin-reflect'
        implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'
        implementation 'com.fasterxml.jackson.module:jackson-module-kotlin'
        implementation 'io.projectreactor.kotlin:reactor-kotlin-extensions'
        
        // JWT
        implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
        implementation 'io.jsonwebtoken:jjwt-impl:0.11.5'
        implementation 'io.jsonwebtoken:jjwt-jackson:0.11.5'
        
        // Kafka
        implementation 'org.springframework.kafka:spring-kafka'
        
        // 데이터베이스 드라이버
        implementation 'io.r2dbc:r2dbc-mysql'
        implementation 'org.mongodb:mongodb-driver-reactivestreams'
        implementation 'io.lettuce:lettuce-core'
        
        // 유틸리티 라이브러리
        implementation 'org.apache.commons:commons-lang3'
        implementation 'org.apache.commons:commons-collections4'
        implementation 'org.slf4j:slf4j-api'
        
        // 개발 도구
        developmentOnly 'org.springframework.boot:spring-boot-devtools'
        developmentOnly 'org.springframework.boot:spring-boot-docker-compose'
        
        // 애노테이션 프로세서
        annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    }
    
    // Docker 이미지 빌드 설정
    docker {
        name "${project.name}:${project.version}"
        tag 'latest'
        
        // 빌드 인자
        buildArgs(['PROJECT_VERSION': project.version])
        
        // 파일 복사
        copySpec.from(tasks.bootJar.outputs.files) {
            into 'dependency'
        }
        
        // 환경 변수
        environment = [
            'PROJECT_NAME': project.name,
            'PROJECT_VERSION': project.version,
            'JAVA_OPTS': '-Xms256m -Xmx512m'
        ]
        
        // 포트 설정
        ports = ['8080']
    }
    
    // Jar 설정
    jar {
        enabled = false // BootJar가 사용되므로 일반 Jar 비활성화
    }
    
    // BootJar 설정
    bootJar {
        archiveClassifier.set('')
        enabled = true
        
        // Layered Jar 설정 (Docker 최적화)
        layered {
            enabled = true
            includeLayerTools = true
        }
    }
}

// =============================================================================
// 📦 프로젝트 모듈 정의
// =============================================================================
project(':api-gateway') {
    description = 'API Gateway for routing and authentication'
    
    dependencies {
        implementation project(':common')
        implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
        implementation 'org.springframework.cloud:spring-cloud-starter-loadbalancer'
    }
}

project(':user-service') {
    description = 'User management service'
    
    dependencies {
        implementation project(':common')
        implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
        implementation 'mysql:mysql-connector-java'
        implementation 'org.flywaydb:flyway-core'
        implementation 'org.flywaydb:flyway-mysql'
    }
}

project(':post-service') {
    description = 'Post management service'
    
    dependencies {
        implementation project(':common')
    }
}

project(':comment-service') {
    description = 'Comment management service'
    
    dependencies {
        implementation project(':common')
    }
}

project(':auth-service') {
    description = 'Authentication and authorization service'
    
    dependencies {
        implementation project(':common')
        implementation 'org.springframework.security:spring-security-oauth2-authorization-server'
    }
}

project(':notification-service') {
    description = 'Notification service'
    
    dependencies {
        implementation project(':common')
        implementation 'org.springframework.kafka:spring-kafka-stream'
    }
}

project(':common') {
    description = 'Common utilities and shared code'
    
    dependencies {
        // 공통 라이브러리
        implementation 'org.springframework.boot:spring-boot-starter-validation'
        implementation 'jakarta.validation:jakarta.validation-api'
    }
    
    // 공통 프로젝트는 BootJar 생성하지 않음
    bootJar {
        enabled = false
    }
}

// =============================================================================
// 📋 버전 관리 및 CI/CD 설정
// =============================================================================
// 의존성 버전 업데이트 확인
dependencyUpdates {
    resolutionStrategy {
        componentSelection { rules ->
            rules.all {
                // 안정 버전만 허용
                rejectVersionIf {
                    quality.isAlpha() || quality.isBeta() || quality.isMilestone()
                }
            }
        }
    }
}

// 릴리즈 태스크
task release {
    doLast {
        println "Releasing version ${version}"
        // Git 태그 생성 및 푸시 (필요시)
        exec {
            commandLine "git tag v${version}"
            workingDir = rootProject.projectDir
        }
        exec {
            commandLine "git push origin v${version}"
            workingDir = rootProject.projectDir
        }
    }
}

// =============================================================================
// 🔧 개발 환경 설정
// =============================================================================
// 개발 환경에서만 사용하는 태스크
if (project.hasProperty('dev')) {
    // 개발 환경 설정
    allprojects {
        bootJar {
            archiveClassifier.set('dev')
        }
    }
    
    // Docker 개발 이미지
    task dockerDev {
        doLast {
            println "Building development Docker images..."
            exec {
                commandLine "./gradlew dockerBuild -Pdev"
                workingDir = rootProject.projectDir
            }
        }
    }
}

// =============================================================================
// 📊 코드 품질 및 분석 설정
// =============================================================================
// JaCoCo 코드 커버리지 (선택사항)
apply plugin: 'jacoco'

jacoco {
    toolVersion = "0.8.8"
}

subprojects {
    apply plugin: 'jacoco'
    
    jacoco {
        toolVersion = "0.8.8"
        reportsDirectory = layout.buildDirectory.dir('reports/jacoco')
    }
    
    // 테스트 커버리지 리포트
    test {
        finalizedBy jacocoTestReport
    }
    
    jacocoTestReport {
        dependsOn test
        reports {
            xml.required = true
            html.required = true
            html.outputLocation = layout.buildDirectory.dir('reports/jacoco/html')
        }
    }
}

// =============================================================================
// 🚀 빌드 최적화 설정
// =============================================================================
// 병렬 빌드 설정
tasks.withType(JavaCompile).configure {
    options.encoding = 'UTF-8'
    options.compilerArgs += [
        '-parameters',
        '-Xlint:unchecked'
    ]
}

// Gradle 데몬 설정
org.gradle.jvmargs = [
    '-Xmx2g',
    '-XX:+UseG1GC',
    '-XX:+UseStringDeduplication'
]

// Gradle 캐시 설정
buildCache {
    local {
        enabled = true
    }
}

// =============================================================================
// 📝 사용자 정의 태스크
// =============================================================================
// 전체 프로젝트 빌드
task buildAll {
    description = 'Build all subprojects'
    dependsOn subprojects.collect { "${it.path}:build" }
}

// 전체 프로젝트 테스트
task testAll {
    description = 'Run all tests'
    dependsOn subprojects.collect { "${it.path}:test" }
}

// Docker 이미지 전체 빌드
task dockerBuildAll {
    description = 'Build Docker images for all services'
    dependsOn subprojects.collect { 
        project -> 
            if (project.name != 'common') {
                "${project.path}:dockerBuild"
            }
        }
        .findAll { it != null }
}

// =============================================================================
// 📚 헬프 툴 함수
// =============================================================================
def getVersion() {
    return version
}

def getProjectName() {
    return project.name
}

def isService(project) {
    return !project.name.equals('common')
}