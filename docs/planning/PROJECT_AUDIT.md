# PROJECT_AUDIT.md
## Phase 0 Discovery Audit — Placement Intelligence & Skill Verification Platform

**Audit Date:** 2026-06-26
**Auditor:** Phase 0 Discovery Process
**Status:** Complete

---

## 1. Repository Structure Overview

```
placement-platform/
├── .env.example                         # Environment variable template
├── .env.prod                            # Production env (git-ignored pattern)
├── .github/workflows/
│   ├── ci.yml                           # 9-stage CI pipeline
│   ├── codeql.yml                       # SAST scanning
│   └── dependency-check.yml             # OWASP CVE scan
├── CLAUDE.md                            # Project instructions
├── FINAL_GO_LIVE_CHECKLIST.md           # Launch checklist
├── PRODUCTION_READINESS_REPORT.md       # Assessment report (scored 97/100)
├── README.md                            # Project overview
├── RUNBOOK.md                           # Operations runbook
├── SecurityChecklist.md                 # Security verification list
├── backend/
│   ├── Dockerfile                       # Multi-stage Docker build
│   ├── docker-compose.dev.yml           # Dev stack (PG + MailHog + ClamAV)
│   ├── docker-compose.dev.minimal.yml   # Dev stack (PG only)
│   ├── pom.xml                          # Maven build descriptor
│   ├── docs/                            # Backend operational docs (A–F)
│   ├── scripts/                         # PowerShell operational scripts
│   ├── src/main/java/com/college/placement/
│   │   ├── Application.java
│   │   ├── modules/
│   │   │   ├── auth/                    # COMPLETE: JWT RS256, RBAC, register/login/refresh/logout
│   │   │   ├── certificate/             # COMPLETE: submit/verify/reject certificates
│   │   │   ├── company/                 # COMPLETE: company + job posting management
│   │   │   ├── placement/               # COMPLETE: applications + offers
│   │   │   ├── student/                 # COMPLETE: student profiles, skills, branches
│   │   │   ├── aigovernance/            # Entities + repositories only (no controller/service)
│   │   │   ├── analytics/               # package-info only — placeholder
│   │   │   ├── eligibility/             # package-info only — placeholder
│   │   │   ├── notification/            # Entities + repository + service (no controller)
│   │   │   └── policy/                  # package-info only — placeholder
│   │   └── shared/
│   │       ├── audit/                   # Auditable base, AuditLog entity
│   │       ├── config/                  # OpenApiConfig (Swagger)
│   │       ├── eventbus/                # Spring event bus with domain events
│   │       ├── exception/               # GlobalExceptionHandler, AuthException
│   │       ├── filepipeline/            # COMPLETE: upload/download/delete + ClamAV
│   │       ├── observability/           # Health indicators, metrics, tracing, request correlation
│   │       ├── outbox/                  # Transactional outbox (dispatcher, service, bridge)
│   │       ├── ratelimit/               # Bucket4j rate limiting
│   │       └── security/               # SecurityConfig, JwtService, filters
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       ├── application-test.yml
│       └── db/migration/               # Flyway migrations
├── deployment/                          # deploy.sh, rollback.sh, health-check.sh
├── docker-compose.prod.yml              # Production stack
├── docs/
│   ├── CLAUDE.md                        # Operational handbook (frontend)
│   ├── FRONTEND_CONSTITUTION.md         # Frontend engineering standards
│   └── MASTER_PROMPT.md                 # Frontend development entry point
├── load-tests/                          # k6 load test scripts
├── monitoring/
│   ├── alerts/alert_rules.yml           # Prometheus alert rules
│   ├── grafana/dashboard.json           # Grafana dashboard
│   └── prometheus/prometheus.yml        # Prometheus config
├── nginx/nginx.conf                     # Production nginx (TLS, rate limit, proxy)
└── scripts/                             # Backup, restore, key generation
```

---

## 2. Technology Stack (Verified)

### Backend
| Component | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.5 |
| Build | Maven | 3.9.6 (bundled) |
| Database | PostgreSQL | 16 |
| ORM | Hibernate / Spring Data JPA | Spring Boot managed |
| Migrations | Flyway | Spring Boot managed |
| Auth | JWT RS256 (spring-security-oauth2-resource-server) | — |
| Password | BCrypt (strength 10) | — |
| Rate Limiting | Bucket4j | — |
| File Scan | ClamAV (daemon socket) | — |
| API Docs | SpringDoc / OpenAPI 3 | — |
| Observability | Micrometer + Prometheus | — |
| Event Bus | Spring ApplicationEventPublisher | — |
| Outbox | Custom transactional outbox | — |
| Containerization | Docker (multi-stage, Alpine JRE) | — |
| Reverse Proxy | nginx | — |

### Frontend (Mandated — Not Yet Implemented)
| Component | Technology |
|---|---|
| Framework | React 19 |
| Language | TypeScript (strict mode) |
| Build | Vite |
| Styling | Tailwind CSS |
| Components | shadcn/ui (Radix UI) |
| Server State | TanStack Query |
| Client State | Zustand |
| Routing | React Router |
| Forms | React Hook Form + Zod |
| HTTP | Axios |
| Icons | Lucide Icons |
| Testing | Vitest + React Testing Library |

---

## 3. Module Breakdown

### Business Modules (`modules/`)

| Module | Status | Controllers | Services | Entities | Notes |
|---|---|---|---|---|---|
| auth | Complete | AuthController, UserController | AuthService | AppUser, RefreshToken | JWT RS256, RBAC, brute-force lockout |
| student | Complete | StudentController | StudentService, BranchService, SkillService | Student, Branch, Skill | Full CRUD + skill assignment |
| company | Complete | CompanyController, JobPostingController | CompanyService, JobPostingService, RecruiterService | Company, JobPosting, Recruiter | Full lifecycle |
| placement | Complete | PlacementController (applications), OfferController | JobApplicationService, OfferService | JobApplication, Offer | Full application + offer lifecycle |
| certificate | Complete | CertificateController | CertificateService | Certificate | Submit/verify/reject flow |
| aigovernance | Partial | None | AIGovernanceService (stub) | AIModel, InferenceHistory, PromptRegistry | No REST API |
| analytics | Placeholder | None | None | None | package-info only |
| eligibility | Placeholder | None | None | None | package-info only |
| notification | Partial | None | NotificationService | NotificationHistory | No REST API; used via outbox |
| policy | Placeholder | None | None | None | package-info only |

### Shared Infrastructure (`shared/`)

| Module | Status | Notes |
|---|---|---|
| security | Complete | SecurityConfig, JwtService, JwtAuthenticationFilter, CorsConfig, RateLimitFilter |
| exception | Complete | GlobalExceptionHandler (RFC 7807 ProblemDetail) |
| filepipeline | Complete | FileController, FilePipelineService, ClamAV, FileStorageService |
| eventbus | Complete | Spring-backed domain event bus with handlers |
| outbox | Complete | Transactional outbox with retry, dead-letter |
| observability | Complete | Health indicators, Micrometer metrics, request correlation |
| audit | Complete | Auditable base class, AuditLog entity, domain event audit handler |
| ratelimit | Complete | Bucket4j filter wired to SecurityConfig |

---

## 4. Database Schema (Verified via Flyway Migrations)

### Tables Created

| Table | Migration | Purpose |
|---|---|---|
| app_metadata | V1_0_0 | Schema version tracking |
| app_users | V1_1_0 | Authentication identities |
| refresh_tokens | V1_1_0 | Refresh token store (SHA-256 hashed) |
| branches | V2_0_0 | Academic departments |
| skills | V2_0_0 | Skill catalogue |
| students | V2_0_0 | Student profiles (FK to app_users) |
| student_skills | V2_0_0 | M:M student-skill junction |
| companies | V2_0_0 | Company registry |
| recruiters | V2_0_0 | Recruiter profiles (FK to app_users + companies) |
| job_postings | V2_0_0 | Job postings with lifecycle status |
| job_posting_skills | V2_0_0 | M:M job posting required skills |
| job_posting_branches | V2_0_0 | M:M job posting eligible branches |
| job_applications | V2_0_0 | Student applications (unique per student+posting) |
| offers | V2_0_0 | Placement offers (one-to-one with application) |
| certificates | V2_0_0 | Student certificates for verification |
| notification_history | V2_0_0 | Notification delivery log |
| audit_logs | V2_0_0 | Domain event audit trail |
| outbox_events | V2_0_0 | Transactional outbox queue |
| file_scan_records | V2_0_0 | File upload metadata + AV scan results |
| ai_models | V2_0_0 | AI model registry |
| prompt_registry | V2_0_0 | AI prompt templates |
| inference_history | V2_0_0 | AI inference audit |
| login_failures | V10_0_0 | Brute-force tracking per IP |

---

## 5. Documentation Inventory

| Document | Location | Status | Purpose |
|---|---|---|---|
| README.md | root | Current | Repository overview |
| CLAUDE.md | root | Current (backend focus) | Backend development instructions |
| PRODUCTION_READINESS_REPORT.md | root | Current | 97/100 production readiness |
| FINAL_GO_LIVE_CHECKLIST.md | root | Current | Pre-launch checklist |
| RUNBOOK.md | root | Exists | Operations runbook |
| SecurityChecklist.md | root | Exists | Security verification |
| FRONTEND_CONSTITUTION.md | docs/ | Current | Frontend engineering standards |
| MASTER_PROMPT.md | docs/ | Current | Frontend development entry point |
| CLAUDE.md | docs/ | Current | Frontend operational handbook |
| A_LOCAL_DEVELOPMENT_SETUP.md | backend/docs/ | Current | Local dev guide |
| B_DOCKER_DEPLOYMENT.md | backend/docs/ | Current | Docker deployment |
| C_PRODUCTION_DEPLOYMENT.md | backend/docs/ | Current | Production deployment |
| D_POSTMAN_TESTING.md | backend/docs/ | Current | API testing with Postman |
| E_SECURITY_VALIDATION.md | backend/docs/ | Current | Security validation |
| F_RUN_EVERYTHING.md | backend/docs/ | Current | Complete run guide |

---

## 6. Key Findings

### Verified Facts

1. **Backend is substantially complete.** All 6 core domain modules (auth, student, company, placement, certificate, filepipeline) have working REST controllers, services, repositories, and entities.

2. **No frontend exists.** Zero frontend files are present in the repository. The `frontend/` directory does not exist. Implementation has not begun.

3. **Role hierarchy:** `ROLE_ADMIN > ROLE_PLACEMENT_OFFICER > ROLE_STUDENT`. This means ADMIN implicitly satisfies all PLACEMENT_OFFICER checks, and PLACEMENT_OFFICER implicitly satisfies all STUDENT checks.

4. **Authentication:** JWT RS256, access tokens (15 min), refresh tokens (7 days). All authenticated routes require `Authorization: Bearer <token>`. The `/auth/*` endpoints are public.

5. **Error format:** All errors return RFC 7807 `ProblemDetail` JSON (`application/problem+json`) with fields: `type`, `title`, `status`, `detail`, and optionally `errors` (validation list).

6. **Pagination:** Spring Data `Pageable` convention: query params `page` (0-indexed), `size`, `sort`.

7. **File upload:** Max 10MB, allowed MIME types: `application/pdf`, `image/png`, `image/jpeg`. Returns `fileKey` (UUID) used in certificate submissions.

8. **Missing REST controllers:** Branches and Skills have full service implementations but NO REST controllers. They cannot be managed via API currently.

9. **No analytics or reporting endpoints** exist. The analytics module is a placeholder.

10. **No notification REST endpoints** exist. Notifications are internal (via outbox).

11. **CORS allowed origins:** Defaults to `http://localhost:3000,http://localhost:5173` for dev. Production requires `CORS_ALLOWED_ORIGINS` env var.

12. **Base API path:** Most authenticated endpoints are under `/api/`. Auth endpoints are under `/auth/` (no `/api/` prefix).

### Discrepancies Between Documentation and Implementation

1. **CLAUDE.md (root) lists Phase 3 (Repositories) as "Current"** but the repository clearly has complete controllers, services, and integration tests through Phase 14 items. The CLAUDE.md appears outdated relative to actual implementation.

2. **README.md says "Increment 1 — Runnable backend foundation"** but the codebase is far beyond that — full domain implementation exists.

3. **Production Readiness Report claims 97/100** but the frontend is completely absent — the report assesses backend only.

4. **No Branch or Skill REST endpoints** despite BranchService and SkillService having full CRUD implementations.

5. **Recruiter entity and RecruiterService exist** but there is no RecruiterController.

### Inferences

1. The backend was built in rapid successive phases and documentation was not updated after initial setup.
2. The project is backend-complete but frontend-zero.
3. The frontend roadmap is well-defined in FRONTEND_CONSTITUTION.md and docs/CLAUDE.md.
