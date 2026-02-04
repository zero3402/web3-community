FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build.gradle.kts ./
COPY src ./src/

RUN ./gradlew build -x test

EXPOSE 8083

CMD ["java", "-jar", "build/libs/*.jar"]