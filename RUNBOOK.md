# Placement Platform — Operations Runbook

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Container Management](#container-management)
3. [Database Recovery](#database-recovery)
4. [Server Recovery](#server-recovery)
5. [Deployment Rollback](#deployment-rollback)
6. [JWT Key Rotation](#jwt-key-rotation)
7. [Incident Response](#incident-response)
8. [Monitoring & Alerting](#monitoring--alerting)
9. [Escalation Contacts](#escalation-contacts)

---

## Architecture Overview

```
Internet → Nginx (80/443) → Spring Boot App (:8081) → PostgreSQL 16 (:5432)
```

| Component | Container | Image |
|---|---|---|
| Reverse Proxy | `placement-prod-nginx` | `nginx:1.25-alpine` |
| Application | `placement-prod-app` | `placement-platform:<version>` |
| Database | `placement-prod-postgres` | `postgres:16-alpine` |

**Volumes:**
- `placement_prod_pgdata` — PostgreSQL data (never delete without backup)
- `placement_prod_uploads` — User-uploaded files
- `placement_prod_nginx_logs` — Nginx access/error logs

---

## Container Management

### Check stack health

```bash
docker compose -f docker-compose.prod.yml ps
./deployment/health-check.sh
```

### View logs

```bash
# Application logs (last 100 lines, follow)
docker logs placement-prod-app --tail=100 -f

# Nginx access logs
docker logs placement-prod-nginx --tail=100 -f

# PostgreSQL logs
docker logs placement-prod-postgres --tail=50 -f
```

### Restart a single service

```bash
# Restart app only (preserves postgres data, no downtime for DB)
docker compose -f docker-compose.prod.yml --env-file .env.prod restart app

# Restart nginx (zero downtime — nginx reloads gracefully)
docker exec placement-prod-nginx nginx -s reload
```

### Emergency stop

```bash
docker compose -f docker-compose.prod.yml down
```

---

## Database Recovery

### Scenario 1: Application cannot connect to PostgreSQL

**Symptoms:** `/actuator/health/readiness` shows `DOWN`, logs show `Connection refused` or `FATAL: password authentication failed`.

**Steps:**
1. Check postgres container state:
   ```bash
   docker inspect --format='{{.State.Health.Status}}' placement-prod-postgres
   ```
2. If `unhealthy` or `exited`:
   ```bash
   docker start placement-prod-postgres
   sleep 15
   docker exec placement-prod-postgres pg_isready -U placement_user -d placement_prod
   ```
3. If password mismatch (volume initialized with different credentials):
   ```bash
   # WARNING: This drops the database volume
   docker compose -f docker-compose.prod.yml down --volumes
   # Restore from backup:
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d postgres
   ./scripts/restore-postgres.sh --file backups/<latest>.sql.gz
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
   ```

### Scenario 2: Database corruption

**Steps:**
1. Stop the application immediately:
   ```bash
   docker stop placement-prod-app
   ```
2. Take a backup of the corrupted state (for forensics):
   ```bash
   ./scripts/backup-postgres.sh
   ```
3. Restore from last clean backup:
   ```bash
   ./scripts/restore-postgres.sh --file backups/<last-clean>.sql.gz --yes
   ```
4. Verify integrity:
   ```bash
   docker exec placement-prod-postgres \
     psql -U placement_user -d placement_prod \
     -c "SELECT COUNT(*) FROM app_users;"
   ```
5. Restart application:
   ```bash
   docker start placement-prod-app
   ./deployment/health-check.sh
   ```

### Scenario 3: Out of disk space

**Steps:**
1. Check disk usage:
   ```bash
   df -h
   docker system df
   ```
2. Rotate old backups:
   ```bash
   find backups/ -name "*.sql.gz" -mtime +7 -delete
   ```
3. Clean Docker resources:
   ```bash
   docker system prune --volumes --force
   # WARNING: Only run this if no containers are running
   ```
4. Clean nginx logs:
   ```bash
   docker exec placement-prod-nginx sh -c "truncate -s 0 /var/log/nginx/access.log"
   ```

---

## Server Recovery

### Scenario: VPS reboot / crash

All containers are configured with `restart: unless-stopped`. After VPS boot:

1. Verify Docker started:
   ```bash
   systemctl status docker
   ```
2. Check containers auto-started:
   ```bash
   docker ps
   ```
3. If containers are stopped:
   ```bash
   cd /opt/placement-platform
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
   ```
4. Validate:
   ```bash
   ./deployment/health-check.sh
   ```

### Scenario: High CPU / memory

1. Identify the process:
   ```bash
   docker stats --no-stream
   ```
2. If application is in GC storm:
   ```bash
   # Graceful restart (Flyway will run again — idempotent)
   docker restart placement-prod-app
   ```
3. If PostgreSQL is under load:
   ```bash
   # Check active queries
   docker exec placement-prod-postgres \
     psql -U placement_user -d placement_prod \
     -c "SELECT pid, query, state, now() - query_start AS duration FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC LIMIT 10;"
   ```

---

## Deployment Rollback

### Automatic rollback (triggered by deploy.sh on health check failure)

The `deploy.sh` script automatically calls `rollback.sh` if the health gate fails.

### Manual rollback

```bash
# Roll back to previous image version
./deployment/rollback.sh

# Roll back to a specific version
./deployment/rollback.sh --version 1.1.0

# Roll back app + restore last DB backup (nuclear option)
./deployment/rollback.sh --version 1.1.0 --restore-db
```

### Git rollback

```bash
# View recent commits
git log --oneline -10

# Roll back code to specific commit
git checkout <commit-sha>
./deployment/deploy.sh --skip-backup
```

---

## JWT Key Rotation

JWT keys should be rotated annually or immediately if compromised.

**Impact:** All existing access tokens are invalidated. Users must re-login. Refresh tokens are also invalidated.

**Steps:**
1. Generate new keys:
   ```powershell
   # Windows
   .\scripts\generate-prod-keys.ps1 --EnvFile .env.prod.new
   ```
   ```bash
   # Linux (using OpenSSL)
   openssl genrsa -out /tmp/private.pem 2048
   PRIV=$(openssl pkcs8 -topk8 -nocrypt -in /tmp/private.pem | grep -v -- "-----" | tr -d '\n')
   PUB=$(openssl rsa -in /tmp/private.pem -pubout | grep -v -- "-----" | tr -d '\n')
   sed -i "s|^JWT_PRIVATE_KEY_PEM=.*|JWT_PRIVATE_KEY_PEM=$PRIV|" .env.prod
   sed -i "s|^JWT_PUBLIC_KEY_PEM=.*|JWT_PUBLIC_KEY_PEM=$PUB|" .env.prod
   rm /tmp/private.pem
   ```
2. Rotate refresh tokens in database (forces all users to re-login):
   ```sql
   DELETE FROM refresh_tokens;
   ```
3. Redeploy the application:
   ```bash
   ./deployment/deploy.sh
   ```
4. Verify login works with new keys.

---

## Incident Response

### Severity Levels

| Level | Description | Response Time | Examples |
|---|---|---|---|
| SEV-1 | Total outage | 15 min | All requests failing, data loss |
| SEV-2 | Partial outage | 1 hour | Auth broken, specific module down |
| SEV-3 | Degraded | 4 hours | Slow response, non-critical error |
| SEV-4 | Minor | Next business day | UI glitch, non-blocking warning |

### SEV-1 Response Checklist

1. [ ] Check `docker compose ps` — are containers healthy?
2. [ ] Check `./deployment/health-check.sh` — which component fails?
3. [ ] Check application logs: `docker logs placement-prod-app --tail=200`
4. [ ] Check postgres: `docker exec placement-prod-postgres pg_isready -U placement_user -d placement_prod`
5. [ ] Check disk: `df -h` — is there space?
6. [ ] If unsure: roll back immediately: `./deployment/rollback.sh`
7. [ ] Notify stakeholders via [escalation channel]
8. [ ] Document timeline in incident log

### Security Incident (suspected breach)

1. Immediately rotate JWT keys (see [JWT Key Rotation](#jwt-key-rotation))
2. Rotate DB password in `.env.prod`, restart all containers
3. Review audit logs for suspicious activity:
   ```bash
   docker logs placement-prod-app 2>&1 | grep "SECURITY_EVENT"
   ```
4. Review nginx access logs for unusual traffic:
   ```bash
   docker exec placement-prod-nginx \
     awk '{print $4}' /var/log/nginx/access.log | sort | uniq -c | sort -rn | head -20
   ```

---

## Monitoring & Alerting

### Key health endpoints

| Endpoint | Purpose | Expected |
|---|---|---|
| `GET /actuator/health/liveness` | JVM alive | `{"status":"UP"}` |
| `GET /actuator/health/readiness` | DB connected, ready | `{"status":"UP"}` |
| `GET /actuator/health` | Full health tree | `{"status":"UP"}` |
| `GET /actuator/prometheus` | Metrics scrape | Prometheus text format |

### Prometheus alerts (see `monitoring/alerts/alert_rules.yml`)

- `PlacementPlatformDown` — app not scraped for 1 min (SEV-1)
- `HighErrorRate` — HTTP 5xx > 1% for 5 min (SEV-2)
- `DatabaseConnectionPoolExhausted` — Hikari pool at max for 2 min (SEV-2)
- `HighMemoryUsage` — JVM heap > 80% for 10 min (SEV-3)

### Manual metrics check

```bash
curl -s http://localhost:8081/actuator/prometheus | grep -E "jvm_memory|hikari|http_server"
```

---

## Escalation Contacts

| Role | Contact |
|---|---|
| On-call Engineer | [Add contact] |
| Database Admin | [Add contact] |
| Security Team | [Add contact] |
| Stakeholder | [Add contact] |
