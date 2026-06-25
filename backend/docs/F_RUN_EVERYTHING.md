# F. RUN EVERYTHING GUIDE

The complete Windows PowerShell guide. Assumes zero prior setup. Every command is copy-paste ready.

---

## PART 0: Prerequisites checklist

Install these before starting:

1. **Docker Desktop** — https://www.docker.com/products/docker-desktop/
   - Open it, make sure the Docker whale icon is in the tray and status is "Running"

2. **Git** — https://git-scm.com/download/win
   - Open a new PowerShell after installing to reload PATH

3. **Verify installations:**
   ```powershell
   docker --version
   git --version
   ```

> Java and Maven are bundled in the repository. You do NOT need them installed.

---

## PART 1: Clone the repository

```powershell
git clone <repository-url> C:\placement-platform
Set-Location C:\placement-platform\backend
```

> Replace `<repository-url>` with the actual URL.

---

## PART 2: Install prerequisites (JDK + Maven are bundled)

The repo already includes:
- JDK 17 at `..\..\jdk\jdk-17.0.19+10\`
- Maven 3.9.6 at `.m2\apache-maven-3.9.6\`

Set up paths for this session:

```powershell
$env:JAVA_HOME = "C:\placement-platform\jdk\jdk-17.0.19+10"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

Verify:
```powershell
java -version
# Expected: openjdk version "17.x.x"
```

---

## PART 3: Configure environment

```powershell
Set-Location C:\placement-platform\backend

# Copy the example env file
Copy-Item .env.example .env -ErrorAction SilentlyContinue
```

For local development, the defaults work. No changes needed.

---

## PART 4: Start Docker stack

```powershell
Set-Location C:\placement-platform

# Start PostgreSQL + MailHog + ClamAV
docker-compose -f docker-compose.dev.yml up -d

# Wait for PostgreSQL to be ready (~10 seconds)
Start-Sleep -Seconds 10

# Verify PostgreSQL is healthy
docker exec placement-postgres pg_isready -U placement_user
# Expected: localhost:5432 - accepting connections
```

If ClamAV is not needed locally:
```powershell
docker-compose -f docker-compose.dev.minimal.yml up -d
```

---

## PART 5: Run the backend

```powershell
Set-Location C:\placement-platform\backend

.m2\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Wait for:
```
Started Application in X.XXX seconds
```

**Leave this terminal open.** Open a new PowerShell for the next steps.

---

## PART 6: Verify the backend is running

```powershell
# Health check
Invoke-RestMethod http://localhost:8081/actuator/health

# Expected output:
# status : UP
```

```powershell
# Open Swagger UI in browser
Start-Process http://localhost:8081/swagger-ui/index.html
```

---

## PART 7: Run all tests

> Tests use H2 in-memory database. Docker NOT required for tests.

```powershell
Set-Location C:\placement-platform\backend

.m2\apache-maven-3.9.6\bin\mvn.cmd clean test
```

Expected final output:
```
Tests run: 266, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## PART 8: Run security scans

### 8a. SpotBugs (static bug analysis)
```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd spotbugs:check
```
Expected: `BUILD SUCCESS` (zero high-severity findings)

### 8b. Checkstyle (code style)
```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd checkstyle:check
```
Expected: `BUILD SUCCESS`

### 8c. OWASP Dependency Check
```powershell
# This downloads the NVD vulnerability database (~200MB, takes 5-10 minutes first run)
.m2\apache-maven-3.9.6\bin\mvn.cmd dependency-check:check

# View the HTML report
Start-Process "target\dependency-check-report.html"
```

---

## PART 9: Run authorization tests

```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=SecurityIntegrationTest
```
Expected: `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`

```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=AuthorizationOwnershipTest
```
Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

---

## PART 10: Run JWT validation tests

```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=JwtValidationTest
```
Expected: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`

This validates:
- Expired tokens rejected
- Forged tokens (wrong RSA key) rejected
- Wrong issuer rejected
- Wrong audience rejected

---

## PART 11: Run file scanning tests

```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=ClamAvVirusScanTest
```
Expected: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

This validates:
- INFECTED file → 422 + quarantined
- Quarantined file download → 403
- Scanner down (FAILED) → upload succeeds with FAILED status
- CLEAN scan → 201 CLEAN

---

## PART 12: Run event bus tests

```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=EventBusIntegrationTest,EventBusEndToEndTest
```
Expected: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`

---

## PART 13: Run outbox pattern tests

```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=OutboxIntegrationTest,OutboxServiceTest
```
Expected: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`

This validates:
- Domain event creates outbox record
- Dispatcher processes pending events
- Failed handler → FAILED status + retry scheduling
- Max retries exceeded → DEAD letter

---

## PART 14: Deploy production stack

### Step 1: Generate JWT keys
```powershell
Set-Location C:\placement-platform\backend
powershell -ExecutionPolicy Bypass -File scripts\generate-jwt-keys.ps1
```

Keys are created at `secrets\jwt-private.pem` and `secrets\jwt-public.pem`.

### Step 2: Configure production environment
```powershell
Copy-Item .env.prod.template .env.prod
notepad .env.prod
```

Fill in at minimum:
- `DB_PASSWORD` — strong random password
- `JWT_PRIVATE_KEY_PEM` — contents of `secrets\jwt-private.pem`
- `JWT_PUBLIC_KEY_PEM` — contents of `secrets\jwt-public.pem`

### Step 3: Build the JAR
```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests
```

### Step 4: Start production Docker stack
```powershell
Set-Location C:\placement-platform
docker-compose -f docker-compose.prod.yml --env-file backend\.env.prod up -d
```

---

## PART 15: Verify production deployment

```powershell
# All containers running?
docker-compose -f docker-compose.prod.yml ps

# Health check
Invoke-RestMethod https://localhost/actuator/health

# App logs
docker-compose -f docker-compose.prod.yml logs -f app
```

---

## PART 16: Backup database

```powershell
Set-Location C:\placement-platform\backend
powershell -ExecutionPolicy Bypass -File scripts\backup.ps1
```

Backup files are saved to `backups\placement_backup_YYYYMMDD_HHMMSS.sql`.

---

## PART 17: Restore database

```powershell
# List available backups
Get-ChildItem backups\*.sql

# Restore a specific backup
powershell -ExecutionPolicy Bypass -File scripts\restore.ps1 -BackupFile "backups\placement_backup_20250601_120000.sql"
```

---

## PART 18: Monitor the application

```powershell
# Run the monitoring dashboard (requires ADMIN JWT)
powershell -ExecutionPolicy Bypass -File scripts\monitor.ps1
```

This shows:
- JVM heap usage
- Active HTTP requests
- Database connection pool
- Outbox queue depth
- File scan success/failure rates

---

## PART 19: Full health verification

```powershell
powershell -ExecutionPolicy Bypass -File scripts\health-check.ps1
```

Checks and reports:
- HTTP health endpoint
- Liveness probe
- Readiness probe
- Database connectivity
- ClamAV connectivity
- Outbox queue state

---

## Quick reference: Most-used commands

```powershell
# Run all tests
.m2\apache-maven-3.9.6\bin\mvn.cmd clean test

# Start local dev server
.m2\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run

# Start Docker dev stack
docker-compose -f docker-compose.dev.yml up -d

# Stop Docker dev stack
docker-compose -f docker-compose.dev.yml down

# Health check
Invoke-RestMethod http://localhost:8081/actuator/health

# View logs (dev)
docker-compose -f docker-compose.dev.yml logs -f

# Run specific test
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=<TestClassName>

# Package JAR
.m2\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests

# Spotbugs
.m2\apache-maven-3.9.6\bin\mvn.cmd spotbugs:check

# OWASP scan
.m2\apache-maven-3.9.6\bin\mvn.cmd dependency-check:check
```
