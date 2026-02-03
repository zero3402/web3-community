# =============================================================================
# 🐳 Comment Service Dockerfile (MongoDB 기반)
# =============================================================================
# 설명: 댓글 관리를 위한 Spring Boot 마이크로서비스 최적화 이미지
# 목적: MongoDB와 연동된 댓글 CRUD, 대댓글 기능 제공
# 특징: WebFlux 리액티브, Kotlin, MongoDB, 계층적 댓글
# 실무 팁: 댓글 트리 구조 최적화, 성능 향상, 동시성 처리
# =============================================================================

# =============================================================================
# 🏗️ 빌드 스테이지 (Maven 빌드 환경)
# =============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 빌드 스테이지 라벨
LABEL stage=builder \
      service=comment-service \
      technology=spring-boot-webflux-kotlin-mongodb

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
      service=comment-service \
      technology=spring-boot-webflux-kotlin-mongodb

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
# ⚙️ JVM 튜닝 (MongoDB 연동 최적화)
# =============================================================================
ENV JAVA_OPTS="-server \
              -Xms256m \
              -Xmx512m \
              -XX:+UseG1GC \
              -XX:MaxGCPauseMillis=200 \
              -XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=75.0 \
              -Djava.security.egd=file:/dev/./urandom \
              -Dspring.profiles.active=kubernetes"

# MongoDB 연동 최적화
ENV SPRING_OPTS="--spring.jmx.enabled=false \
                 --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
                 --management.endpoint.health.show-details=always \
                 --spring.data.mongodb.uri=mongodb://mongouser:mongopass@mongodb-service:27017/web3_posts"

# =============================================================================
# 🌐 포트 설정
# =============================================================================
EXPOSE 8083

# =============================================================================
# 🎯 실행 명령
# =============================================================================
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

# =============================================================================
# 🔍 헬스체크
# =============================================================================
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# =============================================================================
# 📝 메타데이터
# =============================================================================
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.opencontainers.image.title="Web3 Community Comment Service" \
      org.opencontainers.image.description="Comment management service with MongoDB database" \
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