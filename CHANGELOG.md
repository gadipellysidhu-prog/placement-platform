# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — 2026-07-03

First production-ready release of the backend. All bounded contexts are implemented and
tested; the `policy` module is intentionally deferred.

### Added

- **Authentication** — JWT RS256 with refresh-token rotation (SHA-256 hashed at rest),
  BCrypt password hashing, email verification, and brute-force account lockout.
- **IAM / Admin** (`/api/admin/users`) — list/search/filter users, enable/disable/lock/
  unlock accounts, assign roles, and invite privileged users via tokenised activation
  links. Administrative `AccountStatus` (`ACTIVE`/`DISABLED`/`LOCKED`/`INVITED`) gates
  login; the last active administrator is protected from disable/lock/demote.
- **Core domain** — students, branches, skills; companies, recruiters, job postings.
- **Placement** — job applications, application-status funnel, offers, status history,
  and eligibility rules.
- **Certificates** — certificate records with a verification workflow.
- **Dashboard** — aggregate placement summary endpoint.
- **Analytics & Reporting** (`/api/analytics`) — read-model reporting: overview KPIs,
  application funnel, per-branch placement outcomes, top recruiters, CTC distribution,
  and monthly application/offer trends.
- **Notifications** — multi-channel notifications with delivery history.
- **Platform infrastructure** — in-process domain event bus, transactional outbox,
  file upload/scan (ClamAV)/storage pipeline, audit logging, academic years,
  application settings, observability (health/metrics/tracing), and rate limiting.
- **Delivery** — 9-stage CI pipeline, CodeQL SAST, OWASP dependency scanning,
  multi-stage Docker image, and a production profile driven by environment variables.

### Security

- Upgraded managed Log4j2 to 2.26.1 and removed the unused Netty stack from the AWS S3
  client, clearing all HIGH/CRITICAL findings from the OWASP dependency-check gate
  (`failBuildOnCVSS=7`). Remaining suppressions are verified CPE-mismatch false
  positives only.

### Database

- Flyway migrations `V1_0_0` … `V18_0_0`, including `V18_0_0__user_account_status.sql`
  (adds `app_users.status` with `ACTIVE` default and status/role indexes).

### Known limitations

- The `policy` module is a placeholder (package-info only) — intentionally deferred
  until a concrete rules requirement is defined.
- Analytics is computed on demand (no materialized/cached snapshots).

[1.0.0]: https://github.com/gadipellysidhu-prog/placement-platform/releases/tag/v1.0.0
