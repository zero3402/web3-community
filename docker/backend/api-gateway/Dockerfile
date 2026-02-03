# =============================================================================
# 🐳 Backend Dockerfile (API Gateway - Spring Boot + WebFlux + Kotlin)
# =============================================================================
# 설명: Spring Cloud Gateway를 위한 최적화된 Docker 이미지
# 목적: 마이크로서비스 아키텍처의 API 게이트웨이 역할
# 특징: WebFlux 리액티브 프로그래밍, Kotlin 언어, 경량화 이미지
# 실무 팁: JAR 최적화, JVM 튜닝, 다단계 빌드
# =============================================================================

# =============================================================================
# 🏗️ 빌드 스테이지 (Maven 빌드 환경)
# =============================================================================
# Maven JDK 17 (LTS) - Alpine 기반으로 크기 최소화
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 빌드 스테이지 라벨
LABEL stage=builder \
      service=api-gateway \
      technology=spring-boot-webflux-kotlin

# =============================================================================
# 📦 Maven 빌드 환경 설정
# =============================================================================
# 작업 디렉토리 설정
WORKDIR /app

# ⚡ 빌드 속도 최적화: 의존성 설정 파일 먼저 복사
# 이렇게 하면 소스 코드 변경 시 의존성 다운로드 방지
COPY pom.xml .

# Maven 의존성 다운로드 (오프라인 빌드용)
RUN mvn dependency:go-offline -B

# 🔧 소스 코드 복사
COPY src ./src

# =============================================================================
# 🎨 애플리케이션 빌드
# =============================================================================
# Maven 빌드 옵션 설정
ARG MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository

# Spring Boot 애플리케이션 빌드
# -B: 비대화형 모드, -q: quiet 모드, -DskipTests: 테스트 건너뛰기 (빠른 빌드)
RUN mvn clean package -B -q -DskipTests

# =============================================================================
# 🚀 프로덕션 스테이지 (실행 환경)
# =============================================================================
# 최소한의 JRE 이미지 (Alpine Linux)
FROM eclipse-temurin:17-jre-alpine AS production

# 프로덕션 스테이지 라벨
LABEL stage=production \
      service=api-gateway \
      technology=spring-boot-webflux-kotlin

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
# 빌드된 JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

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
# 최적화된 java 명령으로 Spring Boot 애플리케이션 실행
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
LABEL org.opencontainers.image.title="Web3 Community API Gateway" \
      org.opencontainers.image.description="Spring Cloud Gateway with WebFlux and Kotlin" \
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
# docker build --target builder -t web3-community/api-gateway:dev .
# docker run -p 8080:8080 -p 5005:5005 web3-community/api-gateway:dev mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# 🎯 기본 JVM 디버그 포트 설정 (개발용)
# EXPOSE 5005