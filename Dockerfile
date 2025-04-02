# ---- Stage 1: Build using Gradle ----
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy build files and download dependencies first (for cache efficiency)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN gradle build -x test || return 0

# Copy rest of the source and build
COPY . .
RUN gradle bootJar -x test

# ---- Stage 2: Run the app with lightweight Java image ----
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy built jar file from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose your app's port (8081)
EXPOSE 8081

# Start the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
