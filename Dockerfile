FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    apk del maven

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Environment variables
ENV SERVER_PORT=8080
ENV REDIS_HOST=localhost
ENV REDIS_PORT=6379
ENV REDIS_DATABASE=0
ENV EUREKA_SERVER=http://localhost:8761/eureka

# Copy jar from builder stage
COPY --from=builder /build/target/url-shortener-shred-1.0-SNAPSHOT.jar application.jar

# Expose port
EXPOSE ${SERVER_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Run with proper security flags
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UnlockExperimentalVMOptions", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "application.jar"]