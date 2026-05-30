# Multi-stage build for Spring Boot Tour Service
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /build/target/tour-service-*.jar app.jar

# Expose port
EXPOSE 8083
EXPOSE 9093

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]