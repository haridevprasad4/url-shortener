# ══════════════════════════════════════════════════════════════
#  STAGE 1 — BUILDER
# ══════════════════════════════════════════════════════════════

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install Maven — not included in eclipse-temurin by default
RUN apk add --no-cache maven

# Copy pom.xml first for layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the JAR
RUN mvn package -DskipTests -B

# ══════════════════════════════════════════════════════════════
#  STAGE 2 — RUNNER
# ══════════════════════════════════════════════════════════════

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Give ownership to non-root user
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# JVM container optimisations
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -Djava.security.egd=file:/dev/./urandom"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]