# ==============================================================================
# Multi-Stage Dockerfile for Spring Boot Application on Render
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application with Eclipse Temurin JDK 17
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy Maven wrapper and descriptor first to cache dependencies in Docker layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Grant executable permissions to Maven wrapper script
RUN chmod +x mvnw

# Pre-fetch project dependencies in a separate layer to accelerate future builds
RUN ./mvnw dependency:go-offline -B || true

# Copy application source code
COPY src/ src/

# Build the Spring Boot executable fat JAR (app.jar), skipping tests during container build
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Secure Production Runtime with Eclipse Temurin JRE 17
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create unprivileged system group and user for security compliance
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy compiled executable JAR from builder stage
COPY --from=builder /app/target/app.jar app.jar

# Render injects the PORT environment variable dynamically (defaults to 8083 locally)
ENV PORT=8083
EXPOSE 8083

# Run the Spring Boot application; PORT is injected at runtime by Render
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8083} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
