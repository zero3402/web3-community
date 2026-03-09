FROM gradle:8.2-jdk17 AS build
WORKDIR /app
COPY . .
ARG MODULE
RUN gradle :${MODULE}:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
COPY --from=build /app/${MODULE}/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
