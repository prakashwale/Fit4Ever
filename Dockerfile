# Multi-stage build for optimized production image
FROM maven:3.8.6-openjdk-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17

# Production stage
FROM openjdk:17-jdk-slim

# Install curl for health checks and create non-root user for security
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* && \
    groupadd -r spring && useradd -r -g spring spring

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/fit4ever-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

# Expose port
EXPOSE 8080

# Set user
USER spring

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
