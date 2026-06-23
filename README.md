# Placement Intelligence & Skill Verification Platform

[![CI](https://github.com/YOUR_ORG/placement-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_ORG/placement-platform/actions/workflows/ci.yml)
[![CodeQL](https://github.com/YOUR_ORG/placement-platform/actions/workflows/codeql.yml/badge.svg)](https://github.com/YOUR_ORG/placement-platform/actions/workflows/codeql.yml)
[![Dependency Audit](https://github.com/YOUR_ORG/placement-platform/actions/workflows/dependency-check.yml/badge.svg)](https://github.com/YOUR_ORG/placement-platform/actions/workflows/dependency-check.yml)

A modular-monolith placement platform (Spring Boot 3 · Java 17 · PostgreSQL 16 · React).
This repository is being built **increment by increment** per the Development MOP — each
step compiles, boots, and passes a health check before the next is added.

> **Current state: Increment 1 — Runnable backend foundation.**
> The backend boots, connects to PostgreSQL, runs the Flyway baseline migration, and
> serves a health endpoint. Business modules are scaffolded as empty bounded contexts.

## Repository layout (so far)

```
placement-platform/
├── backend/
│   ├── pom.xml                         # Maven build (Spring Boot 3.3.5, Java 17)
│   └── src/
│       ├── main/
│       │   ├── java/com/college/placement/
│       │   │   ├── Application.java     # Spring Boot entry point
│       │   │   ├── modules/             # Bounded contexts (student, company, ...)
│       │   │   └── shared/              # Cross-cutting (eventbus, outbox, security, ...)
│       │   └── resources/
│       │       ├── application.yml      # Common config
│       │       ├── application-dev.yml  # Dev datasource + actuator details
│       │       └── db/migration/        # Flyway migrations (V1_0_0__baseline.sql)
│       └── test/java/...                # Testcontainers context-load test
├── docker-compose.dev.yml              # Postgres + MailHog + ClamAV
├── .env.example
└── .gitignore
```

## Prerequisites

Java 17, Maven 3.9+, Node 20 (later), Docker Desktop. Validate with `java -version`,
`mvn -version`, `docker --version` (MOP 5.2).

## Run it (Windows / macOS / Linux)

```bash
# 1. Start local infrastructure (Postgres on :5432, MailHog UI on :8025)
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps      # postgres should be (healthy)

# 2. Run the backend with the dev profile
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

On startup you should see Flyway apply `V1_0_0__baseline` and Tomcat start on port 8080.

## Verify

```bash
# Health endpoint (no auth in dev) -> {"status":"UP", ...}
curl http://localhost:8080/actuator/health

# App info
curl http://localhost:8080/actuator/info

# Confirm the migration ran
docker exec -it placement-postgres \
  psql -U placement_user -d placement_dev -c "SELECT * FROM app_metadata;"
```

## Build & test

```bash
cd backend
mvn clean install          # runs the Testcontainers smoke test (needs Docker running)
mvn clean package -DskipTests
java -jar target/placement-1.0.0.jar --spring.profiles.active=dev
```

## CI/CD Pipeline

Three GitHub Actions workflows run automatically:

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | Every push / PR | 9-stage build, test, quality, security, Docker, readiness |
| `codeql.yml` | Push to `main`, PRs, weekly | CodeQL SAST — Java + GitHub Actions |
| `dependency-check.yml` | Push to `main`, weekly | OWASP NVD CVE scan (CVSS ≥ 9 fails build) |

### CI stages

```
1 Compile           → validates sources compile cleanly
2 Test              → mvn clean test (H2 in-memory, no Docker required)
3 Package           → mvn package, verifies Spring Boot fat-JAR structure
4 Flyway Validate   → naming convention + duplicate version detection (parallel)
5 Code Quality      → Checkstyle + SpotBugs (parallel)
6 Secrets Scan      → Gitleaks detects committed credentials (parallel)
7 Docker Build      → multi-stage image + /actuator/health container smoke test
8 Deployment Ready  → prod config, migrations, env-var documentation, JAR integrity
9 Release Readiness → aggregated pass/fail summary in GitHub Actions UI
```

Add an `NVD_API_KEY` repository secret (free at nvd.nist.gov) to avoid rate limiting
in the OWASP workflow. Without it the scan still runs, just slower.

## What's next

Increment 2 — **shared infrastructure**: JWT/RBAC security, the internal event bus,
the transactional outbox (table, service, poller), correlation-ID logging, and the
file-pipeline base.
