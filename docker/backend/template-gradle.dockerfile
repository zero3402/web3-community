# =============================================================================
# 🐳 Backend Dockerfile (Spring Boot + WebFlux + Kotlin - Gradle)
# =============================================================================
# 설명: Spring Boot 마이크로서비스 최적화 Gradle 기반 Docker 이미지
# 목적: Gradle 빌드 시스템을 사용한 최적화된 프로덕션 이미지 생성
# 특징: Spring Boot + WebFlux + Kotlin, Gradle 기반 빌드
# 실무 팁: Gradle Caching, Layer 최적화, JVM 튜닝
# =============================================================================

# =============================================================================
# 🏗️ 빌드 스테이지 (Gradle 기반)
# =============================================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

# 빌드 스테이지 라벨
LABEL stage=builder \
      service=backend \
      technology=spring-boot-webflux-kotlin-gradle

# =============================================================================
# 📦 Gradle 빌드 환경 설정
# =============================================================================
# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼를 위한 사용자 설정
RUN addgroup --system --gid 1000 gradle && \
    adduser --system --uid 1000 --gid gradle gradle

# Gradle 캐시 디렉토리 생성
RUN mkdir -p /home/gradle/.gradle && \
    chown -R gradle:gradle /home/gradle

# ⚡ 빌드 속도 최적화: Gradle 설정 파일 먼저 복사
COPY --chown=gradle:gradle gradle/wrapper/ gradle/wrapper/
COPY --chown=gradle:gradle gradlew build.gradle settings.gradle ./

# Gradle Wrapper 실행 권한 설정
RUN chmod +x gradlew

# =============================================================================
# 📋 의존성 다운로드 (캐싱 최적화)
# =============================================================================
# Gradle 의존성 다운로드 (캐시를 위한 빌드없이 실행)
USER gradle
RUN ./gradlew dependencies --no-daemon --configuration-cache || true

# 🔧 소스 코드 복사
COPY --chown=gradle:gradle src/ src/

# =============================================================================
# 🎨 애플리케이션 빌드
# =============================================================================
# Gradle 옵션 설정
ARG GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2"

# Spring Boot 애플리케이션 빌드
# --no-daemon: Gradle 데몬 사용 안함 (컨테이너 환경에 적합)
# --configuration-cache: 빌드 캐싱
# --build-cache: 증분 빌드 지원
RUN ./gradlew clean build -x test --no-daemon --configuration-cache --build-cache

# =============================================================================
# 🚀 프로덕션 스테이지 (JRE 전용)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production

# 프로덕션 스테이지 라벨
LABEL stage=production \
      service=backend \
      technology=spring-boot-webflux-kotlin-jre

# =============================================================================
# 🔧 JRE 최적화 및 보안 설정
# =============================================================================
# 애플리케이션 사용자 생성 (보안: root 사용자 금지)
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

# 애플리케이션 디렉토리 생성
WORKDIR /app

# =============================================================================
# 📦 애플리케이션 배포
# =============================================================================
# 빌드된 JAR 파일 복사 (Gradle 빌드 결과물)
COPY --from=builder /app/build/libs/*.jar app.jar

# JAR 파일 소유자 변경
RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# =============================================================================
# ⚙️ JVM 튜닝 설정 (Spring Boot + WebFlux 최적화)
# =============================================================================
# JVM 옵션 (메모리, GC, 성능 최적화)
ENV JAVA_OPTS="-server \
              -Xms256m \
              -Xmx512m \
              -XX:+UseG1GC \
              -XX:MaxGCPauseMillis=200 \
              -XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=75.0 \
              -Djava.security.egd=file:/dev/./urandom \
              -Dspring.profiles.active=kubernetes"

# Spring Boot 특화 설정
ENV SPRING_OPTS="--spring.jmx.enabled=false \
                 --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
                 --management.endpoint.health.show-details=always"

# =============================================================================
# 🌐 포트 및 네트워크 설정
# =============================================================================
# Spring Boot 기본 포트 (재정의 가능)
EXPOSE 8080

# Spring Actuator 포트 (모니터링용)
EXPOSE 8081

# =============================================================================
# 🎯 실행 명령 (최적화된 Java 실행)
# =============================================================================
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

# =============================================================================
# 🔍 헬스체크 (쿠버네티스 연동용)
# =============================================================================
# Spring Boot Actuator 헬스 엔드포인트 사용
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# =============================================================================
# 📝 메타데이터 및 정보
# =============================================================================
# 빌드 정보 (빌드 시점 전달)
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# OCI 이미지 스펙 준수 라벨
LABEL org.opencontainers.image.title="Web3 Community Backend Service" \
      org.opencontainers.image.description="Spring Boot WebFlux service with Gradle build system" \
      org.opencontainers.image.url="https://github.com/your-org/web3-community" \
      org.opencontainers.image.source="https://github.com/your-org/web3-community" \
      org.opencontainers.image.version="${VERSION:-latest}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.vendor="Web3 Community Team" \
      org.opencontainers.image.licenses="MIT"

# =============================================================================
# 🛡️ 보안 강화
# =============================================================================
# spring 사용자로 실행
USER spring

# =============================================================================
# 📁 볼륨 설정 (선택사항)
# =============================================================================
# 로그 파일용 볼륨 (필요시)
# VOLUME ["/app/logs"]

# 설정 파일용 볼륨 (외부 설정 마운트용)
# VOLUME ["/app/config"]

# =============================================================================
# 🚀 개발/디버그용 (개발 시 사용)
# =============================================================================
# 개발 환경으로 빌드 시:
# docker build --target builder -t web3-community/backend:dev .
# docker run -p 8080:8080 -p 5005:5005 web3-community/backend:dev ./gradlew bootRun -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# 🎯 기본 JVM 디버그 포트 설정 (개발용)
# EXPOSE 5005

# =============================================================================
# 🌟 Gradle 특화 팁
# =============================================================================
# 
# Gradle Docker 빌드 최적화:
# - Gradle Wrapper 사용으로 버전 일관성 보장
# - Gradle 캐싱으로 빌드 속도 향상
# - Configuration Cache로 재빌드 시간 단축
# - Build Cache로 증분 빌드 지원
#
# Multi-project 구조 예시:
# ├── build.gradle (root)
# ├── settings.gradle
# ├── api-gateway/
# │   └── build.gradle
# ├── user-service/
# │   └── build.gradle
# └── ...
#
# 빌드 예시:
# # 전체 프로젝트 빌드
# ./gradlew build
# 
# # 특정 프로젝트만 빌드
# ./gradlew :api-gateway:build
# 
# # Docker 이미지 빌드
# docker build -t web3-community/api-gateway:gradle -f docker/backend/api-gateway/Dockerfile .
#
# 실무 팁:
# - Gradle Enterprise를 사용하면 빌드 성능 더 향상
# - Remote Cache를 사용하여 CI/CD 속도 개선
# - Custom Tasks로 배포 프로세스 자동화
# - Dependency Management으로 라이브러리 버전 일관성
# =============================================================================