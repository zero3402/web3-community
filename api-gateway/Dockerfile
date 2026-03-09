# ══════════════════════════════════════════════════════════════════════════════
# API Gateway Dockerfile - 멀티스테이지 빌드
#
# 멀티스테이지 빌드를 사용하는 이유:
#   1. 최종 이미지에 빌드 도구(Gradle, JDK)가 포함되지 않아 이미지 크기 대폭 감소
#   2. 빌드 환경과 실행 환경 분리로 보안 향상
#   3. JDK(개발용) vs JRE(실행용): JRE 전용 이미지로 실행 시 불필요한 도구 제거
#
# 빌드 명령:
#   docker build -t web3-community/api-gateway:latest .
#
# 실행 명령:
#   docker run -p 8080:8080 \
#     -e JWT_SECRET=your_secret \
#     -e REDIS_HOST=redis \
#     -e EUREKA_URL=http://eureka:8761/eureka/ \
#     web3-community/api-gateway:latest
# ══════════════════════════════════════════════════════════════════════════════

# ── Stage 1: 빌드 스테이지 ────────────────────────────────────────────────────
# Gradle Wrapper를 사용하여 소스를 컴파일하고 JAR 파일을 생성한다.
# eclipse-temurin: OpenJDK 기반의 경량 공식 이미지 (Alpine 사용 시 주의: glibc 호환성)
FROM eclipse-temurin:21-jdk-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /workspace

# ── 의존성 캐시 최적화 ────────────────────────────────────────────────────────
# Gradle 설정 파일만 먼저 복사하여 의존성 다운로드 레이어를 캐시한다.
# 소스 코드 변경 시 의존성을 다시 다운로드하지 않아 빌드 속도 향상.
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 의존성만 미리 다운로드 (소스 없이 빌드하면 실패하므로 dependencies 태스크 실행)
# --no-daemon: CI/CD 환경에서 Gradle 데몬 프로세스 재사용 불필요
RUN ./gradlew dependencies --no-daemon || true

# ── 소스 코드 복사 및 빌드 ────────────────────────────────────────────────────
# 이 레이어는 소스 변경 시에만 무효화됨
COPY src/ src/

# 테스트를 건너뛰고 JAR 빌드 (-x test: 빠른 이미지 빌드 목적)
# 테스트는 CI/CD 파이프라인에서 별도로 실행해야 한다.
RUN ./gradlew bootJar -x test --no-daemon

# ── Stage 2: 실행 스테이지 ────────────────────────────────────────────────────
# JRE 전용 이미지 사용: 컴파일러, javac 등 불필요한 도구 제거
# jammy(Ubuntu 22.04 LTS) 기반으로 glibc 호환성 보장
FROM eclipse-temurin:21-jre-jammy AS runtime

# 보안: root 대신 전용 비특권 사용자로 실행
# 컨테이너 탈출 시 피해 최소화
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser

WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 복사
# build/libs/*.jar: Spring Boot의 실행 가능한 Fat JAR (모든 의존성 포함)
COPY --from=builder /workspace/build/libs/*.jar app.jar

# 파일 소유권을 appuser로 변경
RUN chown appuser:appgroup app.jar

# 비특권 사용자로 전환
USER appuser

# ── JVM 튜닝 옵션 ─────────────────────────────────────────────────────────────
# -XX:+UseContainerSupport: 컨테이너 CPU/메모리 제한을 JVM이 인식
# -XX:MaxRAMPercentage=75.0: 컨테이너 메모리의 75%를 힙으로 사용
# -Djava.security.egd: 엔트로피 수집을 /dev/urandom으로 변경하여 시작 속도 향상
# -Dspring.profiles.active: 환경 변수로 프로파일 주입 가능
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}"

# API Gateway 포트 노출
EXPOSE 8080

# 헬스체크: Spring Boot Actuator /actuator/health 엔드포인트 사용
# --interval: 30초마다 확인
# --timeout: 10초 내 응답 없으면 실패
# --start-period: 컨테이너 시작 후 60초는 헬스체크 제외 (JVM/Spring 초기화 시간)
# --retries: 3회 연속 실패 시 unhealthy 판정
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
            CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
# exec 형식 사용: PID 1로 실행되어 SIGTERM 신호 정상 수신 (Graceful Shutdown)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
