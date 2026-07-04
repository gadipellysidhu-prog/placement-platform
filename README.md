# Placement Intelligence & Skill Verification Platform

[![CI](https://github.com/gadipellysidhu-prog/placement-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/gadipellysidhu-prog/placement-platform/actions/workflows/ci.yml)
[![CodeQL](https://github.com/gadipellysidhu-prog/placement-platform/actions/workflows/codeql.yml/badge.svg)](https://github.com/gadipellysidhu-prog/placement-platform/actions/workflows/codeql.yml)
[![Dependency Audit](https://github.com/gadipellysidhu-prog/placement-platform/actions/workflows/dependency-check.yml/badge.svg)](https://github.com/gadipellysidhu-prog/placement-platform/actions/workflows/dependency-check.yml)

A modular-monolith backend for managing campus placements end-to-end: students and
skills, companies and job postings, applications and offers, certificate verification,
notifications, analytics, and administrative identity management.

**Stack:** Spring Boot 3.5.16 · Java 17 · PostgreSQL 16 · Flyway · Hibernate/JPA ·
Spring Security (JWT RS256) · Micrometer/Prometheus · Docker.

> **Status: backend feature-complete (Version 1.0).** All bounded contexts are
> implemented, tested (345 tests), and covered by a 9-stage CI pipeline plus CodeQL
> and OWASP dependency scanning. The only deferred module is `policy` (an intentional
> placeholder). See [`CHANGELOG.md`](CHANGELOG.md) and
> [`RELEASE_NOTES_v1.0.0.md`](RELEASE_NOTES_v1.0.0.md).

---

## Features

- **Authentication & sessions** — JWT RS256, refresh-token rotation (SHA-256 hashed at
  rest), BCrypt passwords, email verification, brute-force lockout.
- **IAM / Admin** — user listing/search, enable/disable/lock/unlock, role assignment,
  tokenised invitations, last-active-admin protection.
- **Core domain** — students, branches, skills; companies, recruiters, job postings.
- **Placement** — job applications, an application-status funnel, offers, and status
  history; eligibility rules.
- **Certificates** — certificate records with a verification workflow.
- **Dashboard & Analytics** — aggregate summary plus a full read-model reporting API
  (placement rate, application funnel, per-branch outcomes, top recruiters, CTC
  distribution, monthly trends).
- **Notifications** — multi-channel notifications with delivery history (email provider).
- **Platform** — in-process domain event bus, transactional outbox, file upload/scan
  (ClamAV)/storage pipeline, audit logging, academic years, application settings,
  observability (health, metrics, tracing), rate limiting.

---

## Architecture

Modular monolith. Each bounded context lives under `modules/`; cross-cutting
infrastructure lives under `shared/`. Every module follows a strict layered structure:
`controller → service → repository → domain`.

```
com.college.placement
├── modules/
│   ├── auth/         Authentication + IAM/admin (users, roles, invitations, account state)
│   ├── student/      Students, branches, skills
│   ├── company/      Companies, recruiters, job postings
│   ├── placement/    Applications, offers, status history
│   ├── eligibility/  Application eligibility rules (domain service)
│   ├── certificate/  Certificate records + verification
│   ├── notification/ Channels + delivery history
│   ├── dashboard/    Aggregate summary endpoint
│   ├── analytics/    Read-model reporting API
│   ├── aigovernance/ AI model / prompt registry / inference history
│   └── policy/       Placeholder (deferred)
└── shared/
    ├── security/     JWT RS256, filter chain, role hierarchy, RFC 7807 errors
    ├── eventbus/     In-process domain event bus
    ├── outbox/       Transactional outbox + dispatcher
    ├── filepipeline/ Upload → virus scan → storage
    ├── storage/      Storage abstraction (S3/MinIO/local)
    ├── observability/Health, metrics, tracing, web instrumentation
    ├── audit/        Audit log + service
    ├── academic/     Academic years
    ├── settings/     Application settings
    ├── notification/ Email provider
    ├── ratelimit/    Bucket4j rate limiting
    └── exception/    Global exception handling
```

See [`CLAUDE.md`](CLAUDE.md) for the full architecture, conventions, and phase status.

---

## API summary

All endpoints are JSON; errors are RFC 7807 `application/problem+json`. Interactive docs
are served at `/swagger-ui/**` (OpenAPI). Authorization is JWT bearer + role-based.

| Area | Base path | Access |
|---|---|---|
| Auth (login, register, refresh, verify-email, forgot/reset password, accept-invitation) | `/auth/**` | Public |
| IAM / user administration | `/api/admin/users/**` | `ROLE_ADMIN` |
| Students | `/api/students/**` | Officer / owner |
| Branches, skills | `/api/branches/**`, `/api/skills/**` | Officer |
| Companies, job postings | `/api/companies/**` | Officer |
| Applications, offers | `/api/placement/**` | Officer / student |
| Certificates | `/api/certificates/**` | Officer / owner |
| Dashboard summary | `/api/dashboard/summary` | `ROLE_PLACEMENT_OFFICER` |
| Analytics & reporting | `/api/analytics/**` | `ROLE_PLACEMENT_OFFICER` |
| Files | `/api/files/**` | Authenticated |
| Health/metrics | `/actuator/health`, `/actuator/prometheus` | Health public; rest secured |

Role hierarchy: `ROLE_ADMIN > ROLE_PLACEMENT_OFFICER > ROLE_STUDENT`.

---

## Prerequisites

- **Java 17**, **Maven 3.9+** (a Maven wrapper is bundled under `backend/.m2/`), **Docker
  Desktop** (for the local Postgres/MailHog/ClamAV stack).
- Verify: `java -version`, `docker --version`.

---

## Installation & local run

```bash
# 1. Start local infrastructure (PostgreSQL :5432, MailHog UI :8025, ClamAV)
docker compose -f docker-compose.dev.yml up -d
#    Minimal (PostgreSQL only):
docker compose -f docker-compose.dev.minimal.yml up -d

# 2. Run the backend (dev profile, port 8081) — from backend/
cd backend
.m2/apache-maven-3.9.6/bin/mvn.cmd spring-boot:run    # Windows
./.m2/apache-maven-3.9.6/bin/mvn  spring-boot:run     # macOS/Linux
```

On startup Flyway applies migrations `V1_0_0 … V18_0_0` and Tomcat starts on **:8081**.

```bash
# Health (public)
curl http://localhost:8081/actuator/health      # {"status":"UP", ...}
# Interactive API docs
open http://localhost:8081/swagger-ui/index.html
```

---

## Build & test

```bash
cd backend
# Full test suite (H2 in-memory, no Docker required) — 345 tests
.m2/apache-maven-3.9.6/bin/mvn.cmd clean test -Dspring.profiles.active=test
# Code quality gates
.m2/apache-maven-3.9.6/bin/mvn.cmd checkstyle:check spotbugs:check
# Package the Spring Boot fat JAR
.m2/apache-maven-3.9.6/bin/mvn.cmd clean package -DskipTests
```

### Profiles

| Profile | Database | Flyway | `ddl-auto` |
|---|---|---|---|
| *(default)* / `dev` | PostgreSQL (`localhost:5432/placement_dev`) | enabled | `validate` |
| `test` | H2 in-memory | disabled | `create-drop` |
| `prod` | env-driven (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) | enabled | `validate` |

Because non-test profiles use `ddl-auto: validate`, **every schema change requires a
Flyway migration** (`V{major}_{minor}_{patch}__{description}.sql`).

---

## Deployment

The backend ships a multi-stage `backend/Dockerfile` producing a Spring Boot fat-JAR
image. Production configuration is driven entirely by environment variables (see
`application-prod.yml`); no secrets are baked into the image.

```bash
cd backend
docker build -t placement-platform:1.0.0 .
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://<host>:5432/placement \
  -e DB_USERNAME=... -e DB_PASSWORD=... \
  -e JWT_PRIVATE_KEY_PEM=... -e JWT_PUBLIC_KEY_PEM=... \
  placement-platform:1.0.0
```

Operational references:
- [`RELEASE_NOTES_v1.0.0.md`](RELEASE_NOTES_v1.0.0.md) — release notes, known limitations, upgrade & deployment checklist
- [`FINAL_GO_LIVE_CHECKLIST.md`](FINAL_GO_LIVE_CHECKLIST.md) — go-live checklist
- [`PRODUCTION_READINESS_REPORT.md`](PRODUCTION_READINESS_REPORT.md) — readiness assessment
- [`RUNBOOK.md`](RUNBOOK.md) — operations runbook
- [`SecurityChecklist.md`](SecurityChecklist.md) — security checklist

---

## CI/CD

| Workflow | Trigger | Purpose |
|---|---|---|
| `ci.yml` | Every push / PR | 9-stage: compile → test → package → Flyway validate → code quality → secrets scan → Docker build+health → container scan (Trivy) → release readiness |
| `codeql.yml` | Push to `main`, PRs, weekly | CodeQL SAST (Java + Actions) |
| `dependency-check.yml` | Push to `main`, weekly | OWASP NVD CVE scan (fails on CVSS ≥ 7) |

Add an `NVD_API_KEY` repository secret (free at nvd.nist.gov) to speed up the OWASP scan.

---

## Development workflow

1. Branch from `main`; keep changes scoped to one concern per PR.
2. Reuse existing abstractions; follow the layered module structure and constructor injection.
3. Add a Flyway migration for any schema change.
4. Run `mvn clean test`, `checkstyle:check`, and `spotbugs:check` locally — all must be green.
5. Open a PR; CI must pass before merge.

See [`CLAUDE.md`](CLAUDE.md) for coding conventions, entity design rules, and security rules.
