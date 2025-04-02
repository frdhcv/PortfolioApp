# ---- Stage 1: Build using Gradle and Java 21 ----
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy Gradle build files first for caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (this helps use Docker layer caching)
RUN gradle build -x test || return 0

# Copy full project source and re-run build
COPY . .
RUN gradle bootJar -x test

# ---- Stage 2: Run with lightweight JDK image ----
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose Spring Boot port (change if different)
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
