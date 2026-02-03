# =============================================================================
# 🐳 Backend Dockerfile Template (Generic Microservice)
# =============================================================================
# 이 템플릿을 사용하여 다른 마이크로서비스들의 Dockerfile을 생성합니다.
# 각 서비스별로 특화된 설정으로 수정하여 사용합니다.
# =============================================================================

# =============================================================================
# 🐳 Post Service Dockerfile (MongoDB 기반)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production-post

LABEL stage=production \
      service=post-service \
      technology=spring-boot-webflux-kotlin-mongodb

RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# MongoDB 연동 최적화
ENV JAVA_OPTS="-server -Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_OPTS="--spring.data.mongodb.uri=mongodb://mongouser:mongopass@mongodb-service:27017/web3_posts"

EXPOSE 8082

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# =============================================================================
# 🐳 Comment Service Dockerfile (MongoDB 기반)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production-comment

LABEL stage=production \
      service=comment-service \
      technology=spring-boot-webflux-kotlin-mongodb

RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# MongoDB 연동
ENV JAVA_OPTS="-server -Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_OPTS="--spring.data.mongodb.uri=mongodb://mongouser:mongopass@mongodb-service:27017/web3_posts"

EXPOSE 8083

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# =============================================================================
# 🐳 Auth Service Dockerfile (Redis 기반)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production-auth

LABEL stage=production \
      service=auth-service \
      technology=spring-boot-webflux-kotlin-redis

RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# Redis 연동 최적화
ENV JAVA_OPTS="-server -Xms128m -Xmx256m -XX:+UseG1GC"
ENV SPRING_OPTS="--spring.data.redis.host=redis-service --spring.data.redis.port=6379"

EXPOSE 8084

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# =============================================================================
# 🐳 Notification Service Dockerfile (Kafka 기반)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS production-notification

LABEL stage=production \
      service=notification-service \
      technology=spring-boot-webflux-kotlin-kafka

RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# Kafka 연동 최적화
ENV JAVA_OPTS="-server -Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_OPTS="--spring.kafka.bootstrap-servers=kafka-service:9092"

EXPOSE 8085

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1