# ---- Stage 1: Build with Gradle ----
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle build -x test || return 0

COPY . .
RUN gradle bootJar -x test

# ---- Stage 2: Run minimal image ----
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app


# This line avoids hardcoding the JAR name
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
