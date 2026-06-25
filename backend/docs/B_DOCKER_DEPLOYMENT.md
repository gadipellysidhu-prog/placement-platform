# B. Docker Deployment Guide

Complete guide to deploying the application from a fresh machine using Docker Compose.

---

## Prerequisites

- Docker Desktop 4.x+ installed and running
- Git installed

---

## 1. Clone and navigate

```powershell
git clone <repository-url>
cd placement-platform
```

---

## 2. Generate production JWT keys

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File scripts\generate-jwt-keys.ps1
```

This creates `secrets/jwt-private.pem` and `secrets/jwt-public.pem`.

---

## 3. Configure environment

Copy the production environment template:

```powershell
Copy-Item .env.prod.template .env.prod
```

Edit `.env.prod` and fill in all required values:

```powershell
notepad .env.prod
```

Minimum required values:
```
DB_PASSWORD=<strong-password>
JWT_PRIVATE_KEY_PEM=<contents of secrets/jwt-private.pem>
JWT_PUBLIC_KEY_PEM=<contents of secrets/jwt-public.pem>
```

---

## 4. Build the Docker image

```powershell
cd backend
.m2\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests
docker build -t placement-platform:latest .
```

Or use the production compose (which builds automatically):

```powershell
docker-compose -f docker-compose.prod.yml build
```

---

## 5. Start the production stack

```powershell
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

This starts:
- **PostgreSQL 16** — database
- **Spring Boot app** — backend API
- **Nginx** — reverse proxy with HTTPS
- **ClamAV** — antivirus scanning

---

## 6. Verify deployment

```powershell
# All containers running?
docker-compose -f docker-compose.prod.yml ps

# Health check
Invoke-RestMethod https://localhost/actuator/health

# App logs
docker-compose -f docker-compose.prod.yml logs -f app

# PostgreSQL health
docker-compose -f docker-compose.prod.yml exec postgres pg_isready -U placement_user
```

---

## 7. Health endpoints

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `GET /actuator/health` | None | Liveness + readiness |
| `GET /actuator/health/liveness` | None | Liveness probe |
| `GET /actuator/health/readiness` | None | Readiness probe |
| `GET /actuator/metrics` | ADMIN JWT | Application metrics |
| `GET /actuator/prometheus` | ADMIN JWT | Prometheus scrape |

---

## 8. Nginx HTTPS

The production Nginx is configured for HTTPS on port 443. For local Docker testing with self-signed certs, add `-k` flag to curl:

```powershell
curl -k https://localhost/actuator/health
```

For production: replace `nginx/certs/` with real SSL certificates from Let's Encrypt or your CA.

---

## 9. Stop and cleanup

```powershell
# Stop without removing volumes
docker-compose -f docker-compose.prod.yml down

# Stop and remove volumes (WARNING: deletes database)
docker-compose -f docker-compose.prod.yml down -v
```

---

## 10. View and follow logs

```powershell
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Just the app
docker-compose -f docker-compose.prod.yml logs -f app

# Just Nginx
docker-compose -f docker-compose.prod.yml logs -f nginx

# Last 100 lines
docker-compose -f docker-compose.prod.yml logs --tail=100 app
```

---

## Container resource limits (docker-compose.prod.yml)

| Service | CPU | Memory |
|---------|-----|--------|
| app | 1.0 | 512MB |
| postgres | 0.5 | 256MB |
| nginx | 0.25 | 64MB |
| clamav | 0.5 | 512MB |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| App fails to start | `docker logs placement-app` — look for `BeanCreationException` |
| DB connection refused | Check `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in `.env.prod` |
| Flyway migration fails | Check migration files, validate SQL against PostgreSQL |
| ClamAV not starting | Check `docker logs clamav` — first start downloads virus DB (~200MB) |
| HTTPS 502 Bad Gateway | App not started yet — wait 30s and retry |
| JWT error on login | Verify `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM` are set |
