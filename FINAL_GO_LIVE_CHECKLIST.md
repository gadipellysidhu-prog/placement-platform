# Final Go-Live Checklist
## Placement Intelligence & Skill Verification Platform

**Version:** 1.0.0  
**Target Date:** _______________  
**Release Manager:** _______________

---

## Phase 1 — Infrastructure Pre-Check

### VPS Setup
- [ ] Ubuntu 24.04 LTS server provisioned
- [ ] Docker Engine 24+ installed (`docker --version`)
- [ ] Docker Compose v2+ installed (`docker compose version`)
- [ ] Minimum 2 vCPU, 4 GB RAM, 40 GB SSD confirmed
- [ ] Firewall allows only ports 22 (SSH), 80 (HTTP), 443 (HTTPS)
- [ ] Port 5432 (PostgreSQL) blocked at firewall level
- [ ] Port 8081 (App) blocked at firewall level
- [ ] SSH key authentication enabled; password auth disabled
- [ ] Non-root deploy user created with Docker group membership

### Domain & DNS
- [ ] Production domain configured (e.g., `api.placement.example.com`)
- [ ] DNS A record points to VPS public IP
- [ ] DNS propagation confirmed (`nslookup api.placement.example.com`)

### TLS Certificates
- [ ] `fullchain.pem` placed in `nginx/certs/`
- [ ] `privkey.pem` placed in `nginx/certs/`
- [ ] Certificate covers production domain
- [ ] Certificate expiry is > 30 days away
- [ ] Certificate authority is trusted (Let's Encrypt or commercial CA)

---

## Phase 2 — Configuration

### Environment Variables
- [ ] `.env.prod` created from `.env.example` (`cp .env.example .env.prod`)
- [ ] `DB_NAME` set (e.g., `placement_prod`)
- [ ] `DB_USERNAME` set
- [ ] `DB_PASSWORD` set to strong unique value (min 20 chars, random)
- [ ] `JWT_PRIVATE_KEY_PEM` set (run `.\scripts\generate-prod-keys.ps1`)
- [ ] `JWT_PUBLIC_KEY_PEM` set (same script output)
- [ ] `CORS_ALLOWED_ORIGINS` set to production frontend URL
- [ ] `APP_VERSION` set to release version (e.g., `1.0.0`)
- [ ] `SERVER_PORT` set (default `8081`)
- [ ] `FILE_SCAN_ENABLED` set to `true` if ClamAV is running
- [ ] `.env.prod` permissions set to `600`: `chmod 600 .env.prod`
- [ ] `.env.prod` is NOT committed to git (`git status` shows it as untracked/ignored)

### nginx Configuration
- [ ] `nginx/nginx.conf` contains production domain in `server_name`
- [ ] ACME challenge path (`/.well-known/acme-challenge/`) is accessible

---

## Phase 3 — Build & Tests

### CI Pipeline (must be green)
- [ ] ✅ Stage 1 — Compile: passes
- [ ] ✅ Stage 2 — Tests: all 243 tests pass
- [ ] ✅ Stage 3 — Package: fat JAR built and verified
- [ ] ✅ Stage 4 — Flyway Validate: all migrations pass naming check
- [ ] ✅ Stage 5 — Code Quality: Checkstyle + SpotBugs pass
- [ ] ✅ Stage 6 — Secrets Scan: Gitleaks finds no secrets
- [ ] ✅ Stage 7 — Docker Build: image builds; container starts healthy
- [ ] ✅ Stage 7b — Container CVE Scan: no CRITICAL/HIGH CVEs in image
- [ ] ✅ Stage 8 — Deployment Readiness: all files and structure verified
- [ ] ✅ Stage 9 — Release Readiness: all gates passed

### OWASP Dependency Check
- [ ] Latest run completed (check `dependency-check.yml` workflow)
- [ ] No CVSS ≥ 7 vulnerabilities in production-scope dependencies

---

## Phase 4 — Deployment

### Pre-deployment
- [ ] Repository cloned to VPS deploy directory
- [ ] Latest release tag/branch checked out
- [ ] Docker image built: `docker compose -f docker-compose.prod.yml --env-file .env.prod build`

### Launch
- [ ] `docker compose -f docker-compose.prod.yml --env-file .env.prod up -d`
- [ ] All three containers show `healthy` in `docker compose ps`
  - [ ] `placement-prod-postgres` → healthy
  - [ ] `placement-prod-app` → healthy
  - [ ] `placement-prod-nginx` → healthy

---

## Phase 5 — Health Validation

### Automated check
- [ ] `./deployment/health-check.sh --host api.placement.example.com --port 443 --tls`
- [ ] All checks report `[PASS]`

### Manual spot-checks
- [ ] `curl -sf https://api.placement.example.com/actuator/health/liveness` → `{"status":"UP"}`
- [ ] `curl -sf https://api.placement.example.com/actuator/health/readiness` → `{"status":"UP"}`
- [ ] `curl -si http://api.placement.example.com/` → `301 → https://` redirect
- [ ] `curl -si https://api.placement.example.com/actuator/health` shows `Strict-Transport-Security` header
- [ ] `curl -si https://api.placement.example.com/actuator/health` shows `X-Frame-Options: DENY`
- [ ] `POST /auth/login` with wrong credentials → `401` (not 500)
- [ ] `POST /auth/login` with valid credentials → `200` with `accessToken` + `refreshToken`
- [ ] `GET /students` without token → `401`
- [ ] `GET /students` with ADMIN token → `200`
- [ ] Rate limiter activates: rapid repeated `POST /auth/login` → `429` after burst

---

## Phase 6 — Security Verification

- [ ] `curl -I https://api.placement.example.com/actuator/env` → `401` (not 200)
- [ ] `curl -I https://api.placement.example.com/actuator/beans` → `401` (not 200)
- [ ] Direct access to `:5432` from external: refused
- [ ] Direct access to `:8081` from external: refused (only accessible via nginx internally)
- [ ] HSTS header present: `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- [ ] Server header absent or shows only `nginx` (not nginx version)
- [ ] SecurityChecklist.md reviewed and all items verified

---

## Phase 7 — Observability

- [ ] Prometheus scraping `/actuator/prometheus` successfully
- [ ] Grafana dashboard imported from `monitoring/grafana/dashboard.json`
- [ ] At least one successful login visible in auth metrics
- [ ] Alert rules loaded in AlertManager (`monitoring/alerts/alert_rules.yml`)
- [ ] Log shipping configured (or `docker logs placement-prod-app` accessible)

---

## Phase 8 — Backup

- [ ] First production backup taken: `./scripts/backup-postgres.sh`
- [ ] Backup file visible in `backups/` directory
- [ ] Backup file is non-empty and readable: `gunzip -c backups/*.sql.gz | head -20`
- [ ] Cron job scheduled for daily backups at 02:00:
  ```
  0 2 * * * /opt/placement-platform/scripts/backup-postgres.sh >> /var/log/placement/backup.log 2>&1
  ```
- [ ] `/var/log/placement/` directory exists with appropriate permissions
- [ ] Restore procedure tested on staging (or documented as untested)

---

## Phase 9 — Documentation

- [ ] RUNBOOK.md accessible to all on-call engineers
- [ ] Escalation contacts in RUNBOOK.md filled in
- [ ] Admin credentials documented in password manager (not in files)
- [ ] Deployment guide shared with team
- [ ] MonitoringGuide.md reviewed

---

## Phase 10 — Post-Launch

### Immediate (first 2 hours)
- [ ] Error rate monitoring shows < 1% HTTP 5xx
- [ ] P95 latency < 500ms
- [ ] No OOM events in logs: `docker logs placement-prod-app 2>&1 | grep OutOfMemory`
- [ ] Outbox backlog stays at 0: check `placement_outbox_events_pending` metric

### Day 1
- [ ] Backup completed successfully overnight
- [ ] Log volume is reasonable (no log flooding)
- [ ] Disk usage < 50%

### Week 1
- [ ] OWASP dependency check run (or schedule confirmed)
- [ ] First performance load test run against production: `k6 run load-tests/auth-load-test.js`
- [ ] Certificate auto-renewal configured (if using Let's Encrypt)

---

## Sign-Off

| Role | Name | Date | Signature |
|---|---|---|---|
| Release Manager | | | |
| Security Lead | | | |
| QA Lead | | | |
| Infrastructure Lead | | | |

---

**LAUNCH CLEARED** when all Phase 1–9 items are checked and all sign-offs obtained.
