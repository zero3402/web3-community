# =============================================================================
# 🐳 Backend Dockerfile (User Service - Spring Boot + WebFlux + Kotlin)
# =============================================================================
# 설명: 사용자 관리를 위한 Spring Boot 마이크로서비스 최적화 이미지
# 목적: MySQL 데이터베이스와 연동된 사용자 CRUD 기능 제공
# 특징: WebFlux 리액티브, Kotlin, MySQL 연결, JPA/ reactive 리포지토리
# 실무 팁: 데이터베이스 커넥션 풀, 트랜잭션 관리, 보안 강화
# =============================================================================

# =============================================================================
# 🏗️ 빌드 스테이지 (Maven 빌드 환경)
# =============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 빌드 스테이지 라벨
LABEL stage=builder \
      service=user-service \
      technology=spring-boot-webflux-kotlin-mysql

# 작업 디렉토리 설정
WORKDIR /app

# ⚡ 빌드 속도 최적화: 의존성 먼저 복사하여 캐시 활용
COPY pom.xml .

# Maven 의존성 다운로드 (오프라인 빌드 준비)
RUN mvn dependency:go-offline -B

# 🔧 소스 코드 복사
COPY src ./src

# =============================================================================
# 🎨 애플리케이션 빌드
# =============================================================================
# Maven 빌드 실행 (테스트는 나중에 통합 테스트에서)
ARG MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
RUN mvn clean package -B -q -DskipTests

# =============================================================================
# 🚀 프로덕션 스테이지 (실행 환경)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production

# 프로덕션 스테이지 라벨
LABEL stage=production \
      service=user-service \
      technology=spring-boot-webflux-kotlin-mysql

# =============================================================================
# 🔧 JRE 최적화 및 보안 설정
# =============================================================================
# 애플리케이션 사용자 생성
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

# 애플리케이션 디렉토리 생성
WORKDIR /app

# =============================================================================
# 📦 애플리케이션 배포
# =============================================================================
# 빌드된 JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

# 소유권 및 권한 설정
RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# =============================================================================
# ⚙️ JVM 튜닝 (데이터베이스 연동 최적화)
# =============================================================================
ENV JAVA_OPTS="-server \
              -Xms256m \
              -Xmx768m \
              -XX:+UseG1GC \
              -XX:MaxGCPauseMillis=200 \
              -XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=75.0 \
              -Djava.security.egd=file:/dev/./urandom \
              -Dspring.profiles.active=kubernetes"

# Spring Boot 데이터베이스 최적화 설정
ENV SPRING_OPTS="--spring.jmx.enabled=false \
                 --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
                 --management.endpoint.health.show-details=always \
                 --spring.datasource.hikari.maximum-pool-size=20 \
                 --spring.datasource.hikari.minimum-idle=5 \
                 --spring.datasource.hikari.idle-timeout=300000 \
                 --spring.datasource.hikari.pool-name=UserServiceHikariCP"

# =============================================================================
# 🌐 포트 설정
# =============================================================================
# 메인 애플리케이션 포트
EXPOSE 8081

# 액추에이터 포트 (분리 운영)
EXPOSE 8082

# =============================================================================
# 🎯 실행 명령
# =============================================================================
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

# =============================================================================
# 🔍 헬스체크 (데이터베이스 연결 확인)
# =============================================================================
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# =============================================================================
# 📝 메타데이터
# =============================================================================
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.opencontainers.image.title="Web3 Community User Service" \
      org.opencontainers.image.description="User management service with MySQL database" \
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
USER spring

# =============================================================================
# 📁 볼륨 설정
# =============================================================================
# 로그 볼륨
# VOLUME ["/app/logs"]

# 설정 파일 볼륨
# VOLUME ["/app/config"]

# =============================================================================
# 🚀 개발 환경 설정
# =============================================================================
# 개발 빌드: docker build --target builder -t web3-community/user-service:dev .
# 개발 실행: docker run -p 8081:8081 -p 5006:5006 web3-community/user-service:dev mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006"

# 데이터베이스 테스트용 포트
# EXPOSE 5006