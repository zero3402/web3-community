# =============================================================================
# 🐳 Auth Service Dockerfile (Redis 기반)
# =============================================================================
# 설명: 인증/인가를 위한 Spring Boot 마이크로서비스 최적화 이미지
# 목적: Redis 기반의 JWT 토큰 관리, 세션 관리 기능 제공
# 특징: WebFlux 리액티브, Kotlin, Redis, 보안 강화
# 실무 팁: JWT 최적화, 세션 관리, 보안 설정, OAuth2 연동
# =============================================================================

# =============================================================================
# 🏗️ 빌드 스테이지 (Maven 빌드 환경)
# =============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 빌드 스테이지 라벨
LABEL stage=builder \
      service=auth-service \
      technology=spring-boot-webflux-kotlin-redis

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

ARG MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
RUN mvn clean package -B -q -DskipTests

# =============================================================================
# 🚀 프로덕션 스테이지 (실행 환경)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production

# 프로덕션 스테이지 라벨
LABEL stage=production \
      service=auth-service \
      technology=spring-boot-webflux-kotlin-redis

# =============================================================================
# 🔧 JRE 최적화 및 보안 설정
# =============================================================================
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

WORKDIR /app

# =============================================================================
# 📦 애플리케이션 배포
# =============================================================================
COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# =============================================================================
# ⚙️ JVM 튜닝 (Redis 연동 최적화)
# =============================================================================
ENV JAVA_OPTS="-server \
              -Xms128m \
              -Xmx256m \
              -XX:+UseG1GC \
              -XX:MaxGCPauseMillis=200 \
              -XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=75.0 \
              -Djava.security.egd=file:/dev/./urandom \
              -Dspring.profiles.active=kubernetes"

# Redis 및 JWT 최적화
ENV SPRING_OPTS="--spring.jmx.enabled=false \
                 --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
                 --management.endpoint.health.show-details=always \
                 --spring.data.redis.host=redis-service \
                 --spring.data.redis.port=6379 \
                 --spring.data.redis.password=redispass \
                 --spring.data.redis.database=0 \
                 --spring.session.store-type=redis"

# JWT 설정 환경 변수
ENV JWT_SECRET="mysecretjwdkey123"
ENV JWT_EXPIRATION="86400"

# =============================================================================
# 🌐 포트 설정
# =============================================================================
EXPOSE 8084

# =============================================================================
# 🎯 실행 명령
# =============================================================================
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

# =============================================================================
# 🔍 헬스체크
# =============================================================================
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# =============================================================================
# 📝 메타데이터
# =============================================================================
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.opencontainers.image.title="Web3 Community Auth Service" \
      org.opencontainers.image.description="Authentication service with Redis session management" \
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