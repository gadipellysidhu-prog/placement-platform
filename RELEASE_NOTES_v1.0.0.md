# Release Notes — Version 1.0.0

**Placement Intelligence & Skill Verification Platform (backend)**
Release date: 2026-07-03 · Spring Boot 3.5.16 · Java 17 · PostgreSQL 16

---

## Overview

Version 1.0.0 is the first production-ready release of the backend. Every bounded context
is implemented, tested (345 automated tests), and gated by CI (build, tests, Checkstyle,
SpotBugs, secrets scan, CodeQL, OWASP dependency-check, container scan). The system is a
modular monolith with a clean `controller → service → repository → domain` structure,
domain-event decoupling, and a transactional outbox for reliable side effects.

## What's included

- Authentication (JWT RS256, refresh rotation, email verification, brute-force lockout)
- IAM / Admin (user administration, roles, invitations, account lifecycle)
- Students, branches, skills; companies, recruiters, job postings
- Placement (applications, offers, status history, eligibility)
- Certificates (verification workflow)
- Dashboard + Analytics & Reporting
- Notifications with delivery history
- Platform: event bus, transactional outbox, file pipeline (scan + storage), audit log,
  academic years, settings, observability, rate limiting

See [`CHANGELOG.md`](CHANGELOG.md) for the itemised list.

## Known limitations

- **`policy` module** — placeholder (package-info only); deferred until a concrete
  policy/rules requirement is defined. Not wired into any flow.
- **Analytics** — aggregates are computed on demand from operational tables; there is no
  materialized snapshot or caching layer yet. Fine for expected data volumes; revisit if
  reporting queries become hot.
- **Notifications** — email provider is gated by `notification.email.enabled`; ensure a
  real SMTP relay is configured in production.
- **ClamAV** — the file pipeline has no circuit breaker; a scanner outage surfaces as a
  scan-error status rather than degrading gracefully.

## Upgrade guide

This is the initial 1.0 release, so there is no in-place upgrade from a prior tagged
version. For deploying onto an existing database:

1. **Back up the database.**
2. Deploy the 1.0.0 artifact with `SPRING_PROFILES_ACTIVE=prod`. Flyway applies any
   pending migrations up to `V18_0_0` automatically on startup (`ddl-auto: validate`).
3. `V18_0_0__user_account_status.sql` adds `app_users.status` with a default of `ACTIVE`,
   so all existing users remain able to log in; no manual data backfill is required.
4. Confirm `GET /actuator/health` returns `UP` and spot-check `/swagger-ui`.

Rollback: redeploy the previous artifact and restore the database backup. Migrations are
forward-only; do not hand-roll a down-migration against production.

## Deployment checklist

- [ ] Provision PostgreSQL 16 and set `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`.
- [ ] Provide a persistent RS256 key pair via `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM`
      (otherwise an ephemeral key is generated per boot and tokens don't survive restarts).
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Configure SMTP and set `notification.email.enabled=true` if email is required.
- [ ] Configure object storage (`storage.provider`, S3/MinIO credentials) if file uploads
      are used.
- [ ] Set `FRONTEND_BASE_URL` (and, if customised, the verification/reset/invitation paths)
      so email links resolve to the real frontend.
- [ ] Add the `NVD_API_KEY` CI secret for the OWASP workflow.
- [ ] Build and scan the image (`docker build`; the CI Trivy stage runs automatically).
- [ ] Run migrations against a staging copy first; confirm Flyway reaches `V18_0_0`.
- [ ] Verify `/actuator/health` = `UP` and `/actuator/prometheus` scrapes.

## Production readiness

Full detail in the existing operational docs, all current for this release:
[`PRODUCTION_READINESS_REPORT.md`](PRODUCTION_READINESS_REPORT.md),
[`FINAL_GO_LIVE_CHECKLIST.md`](FINAL_GO_LIVE_CHECKLIST.md),
[`RUNBOOK.md`](RUNBOOK.md), and [`SecurityChecklist.md`](SecurityChecklist.md).

## Quality gates (this release)

- `mvn clean verify` — ✅ 345 tests, 0 failures/errors
- Checkstyle — ✅ 0 violations
- SpotBugs — ✅ 0 bugs
- OWASP dependency-check (`failBuildOnCVSS=7`) — ✅ no unresolved HIGH/CRITICAL
