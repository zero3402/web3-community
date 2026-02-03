# =============================================================================
# 🐳 Post Service Dockerfile (MongoDB 기반)
# =============================================================================
# 설명: 게시글 관리를 위한 Spring Boot 마이크로서비스 최적화 이미지
# 목적: MongoDB와 연동된 게시글 CRUD, 검색 기능 제공
# 특징: WebFlux 리액티브, Kotlin, MongoDB, 전문 검색
# 실무 팁: MongoDB 인덱스 최적화, 페이징 처리, 검색 성능
# =============================================================================

# =============================================================================
# 🏗️ 빌드 스테이지 (Maven 빌드 환경)
# =============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 빌드 스테이지 라벨
LABEL stage=builder \
      service=post-service \
      technology=spring-boot-webflux-kotlin-mongodb

# 작업 디렉토리 설정
WORKDIR /app

# 의존성 최적화: pom.xml 먼저 복사
COPY pom.xml .

# Maven 의존성 다운로드
RUN mvn dependency:go-offline -B

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
ARG MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
RUN mvn clean package -B -q -DskipTests

# =============================================================================
# 🚀 프로덕션 스테이지 (실행 환경)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production

# 프로덕션 스테이지 라벨
LABEL stage=production \
      service=post-service \
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
                 --spring.data.mongodb.uri=mongodb://mongouser:mongopass@mongodb-service:27017/web3_posts \
                 --spring.data.mongodb.auto-index-creation=true"

# =============================================================================
# 🌐 포트 설정
# =============================================================================
EXPOSE 8082

# =============================================================================
# 🎯 실행 명령
# =============================================================================
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

# =============================================================================
# 🔍 헬스체크
# =============================================================================
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# =============================================================================
# 📝 메타데이터
# =============================================================================
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.opencontainers.image.title="Web3 Community Post Service" \
      org.opencontainers.image.description="Post management service with MongoDB database" \
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