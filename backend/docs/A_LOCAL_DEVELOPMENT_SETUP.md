# A. Local Development Setup Guide

Everything a new developer needs to clone and run the project from scratch.

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java 17 | JDK 17 | Bundled at `../../jdk/jdk-17.0.19+10` — **no system install needed** |
| Maven 3.9.6 | 3.9.6 | Bundled at `.m2/apache-maven-3.9.6/` — **no system install needed** |
| Docker Desktop | 4.x+ | https://www.docker.com/products/docker-desktop/ |
| Git | Any | https://git-scm.com/ |

> The repo ships both JDK and Maven. You do **not** need them globally installed.

---

## 1. Clone the repository

```powershell
git clone <repository-url>
cd placement-platform
```

---

## 2. Set up environment variables (local dev)

Create a `.env` file in `backend/` (copy from template):

```powershell
Copy-Item backend\.env.example backend\.env
```

For local development, the defaults work out of the box. No changes needed.

---

## 3. Start the local Docker stack

This starts PostgreSQL 16, MailHog (email), and ClamAV:

```powershell
docker-compose -f docker-compose.dev.yml up -d
```

Or PostgreSQL only (minimal):

```powershell
docker-compose -f docker-compose.dev.minimal.yml up -d
```

Verify PostgreSQL is up:

```powershell
docker ps --filter "name=postgres"
```

---

## 4. Run the backend

```powershell
cd backend
.m2\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Expected output:
```
Started Application in X.XXX seconds (process running for X.XXX)
```

The backend starts on **port 8081** (dev profile).

Verify:
```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Expected: `{"status":"UP",...}`

---

## 5. Run all tests (no Docker needed)

Tests run against H2 in-memory — no external services required:

```powershell
cd backend
.m2\apache-maven-3.9.6\bin\mvn.cmd clean test
```

Expected: `Tests run: 266, Failures: 0, Errors: 0, Skipped: 0`

---

## 6. Run a single test class

```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=AuthIntegrationTest
```

---

## 7. Application profiles

| Profile | Database | Flyway | DDL | Port |
|---------|----------|--------|-----|------|
| `test` | H2 in-memory | Disabled | `create-drop` | 8080 |
| `dev` (default) | PostgreSQL localhost:5432 | Enabled | `validate` | 8081 |
| `prod` | PostgreSQL via env vars | Enabled | `validate` | 8080 |

---

## 8. Key URLs (local dev)

| Service | URL |
|---------|-----|
| API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui/index.html |
| Health | http://localhost:8081/actuator/health |
| Metrics | http://localhost:8081/actuator/metrics (ADMIN only) |
| MailHog UI | http://localhost:8025 |

---

## 9. Common dev commands

```powershell
# Compile only
.m2\apache-maven-3.9.6\bin\mvn.cmd compile

# Package JAR (skip tests)
.m2\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests

# Static analysis
.m2\apache-maven-3.9.6\bin\mvn.cmd spotbugs:check
.m2\apache-maven-3.9.6\bin\mvn.cmd checkstyle:check

# Stop Docker stack
docker-compose -f docker-compose.dev.yml down
```

---

## 10. Flyway migrations

All schema changes must have a Flyway migration:

```
backend/src/main/resources/db/migration/
  V1_0_0__baseline.sql
  V1_1_0__auth_tables.sql
  V2_0_0__core_domain.sql
```

Naming: `V{major}_{minor}_{patch}__{description}.sql`

The `test` profile uses Hibernate `create-drop` — no Flyway in tests.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 8081 already in use | `netstat -ano \| findstr 8081` then kill the process |
| PostgreSQL connection refused | Check Docker: `docker ps`, `docker logs postgres` |
| Test failures | Run `mvn clean test` — never run tests from IDE without profile |
| Flyway baseline error | Run `docker-compose down -v` to wipe volumes, then re-up |
