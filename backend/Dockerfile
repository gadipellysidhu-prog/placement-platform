# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build
# Uses the full Maven + JDK image to compile and package the fat JAR.
# Dependencies are resolved before copying source so the layer is cached
# on subsequent builds unless pom.xml changes.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -q
COPY src/ src/
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# Minimal Alpine JRE image — no compiler, no Maven, no dev tools.
# Non-root execution, read-only filesystem with explicit /tmp tmpfs.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# OCI standard image labels for registry discoverability and audit
LABEL org.opencontainers.image.title="Placement Intelligence & Skill Verification Platform"
LABEL org.opencontainers.image.description="Modular monolith: Spring Boot 3.3.5 / Java 17 / PostgreSQL 16"
LABEL org.opencontainers.image.vendor="College Placement Team"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.base.name="eclipse-temurin:17-jre-alpine"

WORKDIR /app

# Upgrade all Alpine packages to their latest patched versions.
# This fixes OS-level CVEs that Trivy flags as "fixable" (i.e. a newer package
# version exists in the Alpine repos). Must run before dropping to non-root.
RUN apk upgrade --no-cache

# Create non-root system user/group and grant /app ownership
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown appuser:appgroup /app

# Copy fat JAR from build stage (chowned immediately — no layer with root-owned file)
COPY --chown=appuser:appgroup --from=build /app/target/*.jar app.jar

# Drop to non-root before any process runs
USER appuser

# Application port (matches SERVER_PORT default and application.yml)
EXPOSE 8081

# Container health check — verifies the liveness probe via busybox wget.
# start-period gives Flyway + Hibernate time to complete on cold start.
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -q --tries=1 --spider http://localhost:8081/actuator/health/liveness || exit 1

# JVM flags:
#   UseContainerSupport     — respect cgroup memory/CPU limits
#   InitialRAMPercentage    — floor: claim 50% at startup to prevent GC pressure spikes
#   MaxRAMPercentage        — ceiling: leave 25% headroom for OS + off-heap
#   ExitOnOutOfMemoryError  — crash immediately so the orchestrator can restart cleanly
#   Duser.timezone          — consistent timestamps in logs
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Duser.timezone=Asia/Kolkata", \
    "-jar", "app.jar"]
