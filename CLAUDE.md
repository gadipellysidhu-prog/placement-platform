# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## DO NOT CHANGE

Unless explicitly requested:

- Do not rewrite the existing package architecture.
- Do not replace existing implementations that already satisfy requirements.
- Do not rename database tables.
- Do not rename Flyway migrations.
- Do not modify the authentication flow.
- Do not modify the `AppUser` entity unless absolutely necessary.
- Do not change `SecurityConfig` unless fixing a verified bug.
- Do not remove or weaken integration tests.
- Do not introduce breaking API changes.
- Do not create duplicate entities, repositories, DTOs, services, or configurations.
- Prefer extension over replacement.

---

## Current Project Status

### Completed

- Phase 1: Authentication
- JWT RS256 Authentication
- Refresh Token Rotation
- BCrypt Password Hashing
- Role-Based Access Control
- Security Filter Chain
- Flyway Authentication Migration
- Integration Tests
- Docker Development Environment

### Current Phase

**Phase 2: Core Domain Foundation**

Current implementation target: Student, Company, Recruiter, Branch, Skill

### Future Phases

Core Placement Domain · Business Services · REST APIs · Internal Event Bus · Transactional Outbox · File Pipeline · AI Governance · Observability · Security Hardening · Testing Expansion · CI/CD · Docker Production · Production Deployment

---

## Development Workflow

For every implementation phase:

1. Analyze the existing repository before making changes.
2. Reuse existing classes whenever possible.
3. Modify only required files.
4. Do not rewrite unrelated code.
5. Keep package architecture unchanged.
6. Create Flyway migrations for schema changes.
7. Ensure Hibernate starts successfully.
8. Ensure Flyway migration succeeds.
9. Run `mvn clean test`.
10. Run `mvn spring-boot:run`.
11. Stop after completing the current phase.
12. Wait for explicit approval before continuing.

---

## Project

**Placement Intelligence & Skill Verification Platform** — a Spring Boot 3.3.5 / Java 17 modular monolith.

Working directory for all backend work: `backend/`

---

## Commands

All commands run from `backend/`.

The repo ships a bundled Maven at `.m2/apache-maven-3.9.6/` and a bundled JDK at `../../jdk/jdk-17.0.19+10`.  
Use the wrapper script; do **not** rely on a system `mvn` or `java` being present.

```bash
# Compile
.m2/apache-maven-3.9.6/bin/mvn.cmd compile

# Run all tests (uses H2 in-memory, no Docker required)
.m2/apache-maven-3.9.6/bin/mvn.cmd clean test -Dspring.profiles.active=test

# Run a single test class
.m2/apache-maven-3.9.6/bin/mvn.cmd test -Dspring.profiles.active=test -Dtest=AuthIntegrationTest

# Start the application (activates dev profile, port 8081)
.m2/apache-maven-3.9.6/bin/mvn.cmd spring-boot:run

# Package
.m2/apache-maven-3.9.6/bin/mvn.cmd clean package -DskipTests
```

**Local dev stack** (PostgreSQL 16 + MailHog + ClamAV):
```bash
docker-compose -f docker-compose.dev.yml up -d
# Minimal (PostgreSQL only):
docker-compose -f docker-compose.dev.minimal.yml up -d
```

---

## Profiles

| Profile | DB | Flyway | DDL |
|---|---|---|---|
| *(default)* | PostgreSQL (`localhost:5432/placement_dev`) | enabled | `validate` |
| `dev` | same PostgreSQL | enabled | `validate` |
| `test` | H2 in-memory | **disabled** | `create-drop` |
| `prod` | env vars required | enabled | `validate` |

Test profile disables Flyway; Hibernate generates the H2 schema from entity definitions.  
The default/dev/prod profiles use `ddl-auto: validate`, so every schema change **must** have a Flyway migration.

---

## Architecture

### Modular Monolith

```
com.college.placement
├── Application.java                  ← entry point
├── modules/                          ← business modules (one package per bounded context)
│   ├── auth/                         ← COMPLETE: JWT RS256, RBAC, login/register/refresh/logout
│   │   ├── controller/
│   │   ├── domain/         AppUser, RefreshToken, Role (enum)
│   │   ├── dto/
│   │   ├── repository/
│   │   └── service/        AuthService
│   ├── aigovernance/                 ← placeholder (package-info only)
│   ├── analytics/                    ← placeholder
│   ├── certificate/                  ← placeholder
│   ├── company/                      ← placeholder
│   ├── eligibility/                  ← placeholder
│   ├── notification/                 ← placeholder
│   ├── placement/                    ← placeholder
│   ├── policy/                       ← placeholder
│   └── student/                      ← placeholder
└── shared/                           ← cross-cutting infrastructure
    ├── audit/                        ← placeholder
    ├── eventbus/                     ← placeholder
    ├── exception/      GlobalExceptionHandler, AuthException
    ├── filepipeline/                 ← placeholder
    ├── observability/                ← placeholder
    ├── outbox/                       ← placeholder
    ├── ratelimit/      RateLimitConfig (Bucket4j placeholder)
    └── security/       SecurityConfig, JwtService, JwtAuthenticationFilter,
                        CustomUserDetailsService, SecurityProblemHandler,
                        CorsConfig, JwtProperties
```

Each `modules/<name>/` sub-package follows a strict layered structure:
`controller → service → repository → domain`

### Security (Phase 1 — complete)

- **JWT RS256**: `JwtService` signs/verifies. Ephemeral key pair on startup unless `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM` env vars are set.
- **Filter**: `JwtAuthenticationFilter` (OncePerRequestFilter). Never propagates exceptions; clears context on failure.
- **Role hierarchy**: `ROLE_ADMIN > ROLE_PLACEMENT_OFFICER > ROLE_STUDENT`. Wired into `MethodSecurityExpressionHandler` for `@PreAuthorize`.
- **Error format**: all security exceptions → RFC 7807 `ProblemDetail` JSON (`application/problem+json`) via `SecurityProblemHandler` (filter-level) and `GlobalExceptionHandler` (MVC-level).
- **Public paths**: `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/verify-email`, `/auth/forgot-password`, `/actuator/health`.

### Persistence

- **ORM**: Hibernate / Spring Data JPA, `open-in-view: false`.
- **Migrations**: Flyway, `classpath:db/migration`, naming `V{major}_{minor}_{patch}__{description}.sql`.
  - `V1_0_0__baseline.sql` — extensions + `app_metadata`
  - `V1_1_0__auth_tables.sql` — `app_users`, `refresh_tokens`
- `AppUser` is the auth identity entity (`modules/auth/domain`). Domain-level user profiles (Student, PlacementOfficer) link to it via FK; do **not** duplicate it.
- Refresh tokens store a SHA-256 hex hash of the raw token (never the raw token).

### Exception handling

- `GlobalExceptionHandler` (`@RestControllerAdvice`) handles: `MethodArgumentNotValidException` → 400, `ResponseStatusException` → preserves status, `AccessDeniedException` → 403, `AuthenticationException` → 401, `Exception` → 500.
- `AuthException extends ResponseStatusException` — factory methods: `invalidCredentials()`, `invalidRefreshToken()`, `emailAlreadyRegistered()` (409), `accountLocked()`.

---

## Entity Design Rules

- UUID primary keys with `@GeneratedValue(strategy = GenerationType.UUID)`.
- `@Version` for optimistic locking on every entity.
- `createdAt` and `updatedAt` set via `@PrePersist` / `@PreUpdate`.
- `LAZY` relationships by default; use `EAGER` only when the association is unconditionally needed in every call path.
- Bidirectional relationships only when navigation is required from both sides.
- Avoid circular dependencies between module packages.
- Validation annotations on entity fields.
- `equals()` and `hashCode()` based only on the identifier field; do **not** use `@Data` on JPA entities.
- Proper indexes and unique constraints declared on `@Table`.
- Repository layer contains persistence logic only — no business logic.

---

## Performance Rules

- Avoid N+1 queries; use fetch joins or entity graphs when loading associations.
- Use pagination for all collection endpoints.
- Index searchable and filterable columns.
- Keep entities lightweight — do not eagerly load large collections.

---

## Security Rules

- Never hardcode secrets; use environment variables.
- Preserve the JWT RS256 authentication architecture.
- Preserve the RBAC role hierarchy.
- Preserve RFC 7807 `ProblemDetail` error responses.
- Never bypass authentication or authorization.

---

## Coding Conventions

- **Constructor injection only** — no `@Autowired` field injection.
- **No business logic in controllers** — thin controllers delegate to services.
- **No magic strings** — use named constants.
- **Lombok**: `@Getter`, `@Setter`, `@NoArgsConstructor` on entities; `@Builder` on value objects / DTOs where appropriate.
- **`@Modifying` queries** must declare `clearAutomatically = true, flushAutomatically = true`.
- Every `@Transactional` write method belongs on the service, not the controller.
- Side effects (email, notifications) go through the transactional outbox — never called synchronously inside a business transaction.

---

## Phase Status

| Phase | Status |
|---|---|
| 1 — Authentication | ✅ Complete |
| 2 — Core Domain entities | ✅ Complete |
| 3 — Repositories | 🔲 **Current** |
| 4 — Business Services | 🔲 |
| 5 — Internal Event Bus | 🔲 |
| 6 — Transactional Outbox | 🔲 |
| 7 — REST APIs | 🔲 |
| 8 — File Pipeline | 🔲 |
| 9 — Observability | 🔲 |
| 10 — Security Hardening | 🔲 |
| 11 — Testing | 🔲 |
| 12 — CI/CD | 🔲 |
| 13 — Docker Production | 🔲 |
| 14 — Production Readiness | 🔲 |

**Implement ONE phase at a time. Stop and wait for explicit approval before proceeding.**
