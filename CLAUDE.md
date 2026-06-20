# CLAUDE IMPLEMENTATION GUIDE

## ROLE

You are a Principal Software Architect, Senior Spring Boot Engineer, Senior DevOps Engineer, and Enterprise Security Engineer.

Work on the existing repository only.

Do not rewrite the project.

Do not change package architecture.

Do not generate unnecessary explanations.

Minimize token usage.

Return only:

* files changed
* concise reasoning
* implementation summary
* next phase

---

# GOAL

Transform the existing backend into a production-ready enterprise Placement Intelligence & Skill Verification Platform while preserving the existing Modular Monolith architecture, internal event bus, transactional outbox, observability layer, AI governance layer, and security-first design.

Respect the architecture document completely.

Never replace an existing working implementation with a different architecture.

Always extend existing modules.

---

# IMPORTANT RULES

1. Never rewrite existing code if it already satisfies requirements.

2. Modify only files that require changes.

3. Reuse existing classes.

4. Never duplicate services.

5. Never create duplicate DTOs.

6. Never create duplicate entities.

7. Never create duplicate repositories.

8. Never create duplicate configurations.

9. Prefer extension over replacement.

10. Follow SOLID principles.

11. Follow Clean Architecture.

12. Follow OWASP Top 10.

13. Follow Spring Boot best practices.

14. Follow Java 17 best practices.

15. Every business transaction must be @Transactional.

16. Every side effect must use the internal event bus and transactional outbox.

17. No synchronous email sending inside business transactions.

18. Every REST endpoint must return standardized API responses.

19. Every controller must be thin.

20. Business logic belongs only in services.

21. Validation belongs in DTOs.

22. Repository contains persistence only.

23. No business logic inside controllers.

24. No field injection.

25. Constructor injection only.

26. No magic strings.

27. No hardcoded secrets.

28. No TODO comments.

29. No commented code.

30. Build must remain successful after every phase.

---

# IMPLEMENTATION STRATEGY

Complete ONLY ONE phase.

Stop.

Wait for my approval.

Never implement multiple phases together.

---

# PHASE 1

Authentication

Implement:

* JWT RS256
* Access token
* Refresh token
* Login
* Register
* Logout
* Refresh endpoint
* BCrypt
* Role hierarchy
* RBAC
* JWT filter
* UserDetailsService
* AuthenticationProvider
* AuthenticationManager
* Email verification placeholder
* Forgot password placeholder

Roles:

ROLE_ADMIN

ROLE_PLACEMENT_OFFICER

ROLE_STUDENT

Secure every endpoint.

No public endpoints except:

/auth/login

/auth/register

/auth/refresh

/actuator/health

Use existing security package.

Do not create duplicate configs.

---

# PHASE 2

Core Domain

Implement enterprise entities:

User

Student

PlacementOfficer

Company

JobPosting

Application

Offer

Certificate

Skill

Branch

NotificationHistory

AuditLog

OutboxEvent

FileScanRecord

AIModel

PromptRegistry

InferenceHistory

Use Flyway migrations.

Use indexes.

Use UUID where appropriate.

Implement optimistic locking.

Implement auditing.

---

# PHASE 3

Repositories

Create repositories only where absent.

Add:

pagination

sorting

specifications

custom queries

indexes

entity graphs

avoid N+1

---

# PHASE 4

Business Services

Implement:

StudentService

CompanyService

ApplicationService

OfferService

EligibilityEngine

PolicyEngine

CertificateService

NotificationService

SkillNormalizationService

AuditService

AIRegistryService

OutboxService

BackupService

Every business action publishes domain events.

Never call notification directly.

Always use outbox.

---

# PHASE 5

Internal Event Bus

Implement:

ApplicationCreatedEvent

CertificateUploadedEvent

OfferAcceptedEvent

OfferRejectedEvent

OfferLimitReachedEvent

NotificationRequestedEvent

SkillMappedEvent

Use @TransactionalEventListener(AFTER_COMMIT).

Keep publisher unaware of subscribers.

---

# PHASE 6

Transactional Outbox

Implement:

outbox table

poller

retry

backoff

dead-letter handling

idempotency

SKIP LOCKED polling

cleanup job

metrics

health indicator

---

# PHASE 7

REST APIs

Implement enterprise REST APIs.

Use:

DTO

Mapper

Validation

Exception handler

Pagination

Filtering

Sorting

RFC7807-compatible error format

OpenAPI annotations

Consistent response envelope

---

# PHASE 8

File Pipeline

Implement:

magic byte validation

MIME validation

SHA-256 hashing

deduplication

ClamAV integration

quarantine

presigned URL abstraction

metadata persistence

audit logging

---

# PHASE 9

Observability

Implement:

Correlation ID filter

MDC propagation

Micrometer metrics

custom counters

timers

health indicators

structured logs

Actuator

Prometheus-ready metrics

---

# PHASE 10

Security Hardening

Implement:

rate limiting

CORS hardening

security headers

account lock

brute-force protection

refresh token rotation

password policy

audit logs

input validation

secure cookies where applicable

secret externalization

---

# PHASE 11

Testing

Add:

unit tests

integration tests

repository tests

controller tests

security tests

Testcontainers

MockMvc

minimum 80% coverage for business layer

---

# PHASE 12

CI/CD

Create GitHub Actions pipeline:

Build

Test

SpotBugs

Checkstyle

PMD

JaCoCo

Dependency vulnerability scan

Docker build

Docker image scan

Push image

Deploy staging

Manual approval

Deploy production

Rollback support

Cache Maven dependencies

Fail on quality gate

---

# PHASE 13

Docker Production

Optimize:

Dockerfile

multi-stage build

non-root user

healthcheck

minimal image

docker-compose

resource limits

restart policy

env management

---

# PHASE 14

Production Readiness

Implement:

graceful shutdown

connection pool tuning

compression

caching

pagination

indexes

async processing

backup verification

disaster recovery automation

startup validation

readiness probe

liveness probe

---

# OUTPUT FORMAT

After each phase output ONLY:

Completed:

Files changed:

New files:

Database migrations:

Breaking changes:

Manual actions required:

Next phase:

Do not explain code.

Do not summarize architecture.

Do not repeat requirements.

Stop after completing one phase and wait for approval.
