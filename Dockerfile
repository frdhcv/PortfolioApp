# --------- Stage 1: Build with Gradle ---------
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Copy only build files first for better Docker caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN gradle build -x test || return 0

# Now copy source files and build the app
COPY . .
RUN gradle bootJar -x test

# --------- Stage 2: Create smaller image ---------
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy jar from builder image
COPY --from=builder /app/build/libs/portfolio-0.0.1-SNAPSHOT.jar app.jar

# Expose port (match your app port)
EXPOSE 8081

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
