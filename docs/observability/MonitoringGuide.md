# Monitoring Guide — Placement Platform

## Overview

The platform exposes metrics, health probes, and structured logs for full observability.

| Layer | Technology | Endpoint |
|---|---|---|
| Metrics | Micrometer + Prometheus | `GET /actuator/prometheus` |
| Health probes | Spring Boot Actuator | `GET /actuator/health/**` |
| Structured logs | Logback + Logstash JSON | Docker log driver / file |
| Alerting | Prometheus AlertManager | `monitoring/alerts/alert_rules.yml` |
| Dashboards | Grafana | `monitoring/grafana/dashboard.json` |

---

## Health Endpoints

### Liveness Probe
```
GET /actuator/health/liveness
```
Returns `{"status":"UP"}` when the JVM is alive. Kubernetes/Docker restarts the container if this fails.

### Readiness Probe
```
GET /actuator/health/readiness
```
Returns `{"status":"UP"}` when the database connection is established and the app is ready to serve traffic. Traffic is withheld until this passes.

### Full Health Tree (admin only)
```
GET /actuator/health
Authorization: Bearer <ADMIN_TOKEN>
```
Returns the complete health tree including database, disk space, event bus, and outbox.

### Docker health check

Both `Dockerfile` and `docker-compose.prod.yml` configure health checks:
- Dockerfile: uses liveness probe
- Compose: uses readiness probe (confirms DB connected)

---

## Key Metrics

### Application Health

| Metric | Description |
|---|---|
| `http_server_requests_seconds{uri,method,status}` | HTTP request latency histogram |
| `jvm_memory_used_bytes` | JVM heap and non-heap memory |
| `jvm_gc_pause_seconds` | GC pause duration |
| `process_cpu_usage` | JVM CPU usage |

### HikariCP Connection Pool

| Metric | Description |
|---|---|
| `hikaricp_connections_active` | Active DB connections |
| `hikaricp_connections_idle` | Idle connections |
| `hikaricp_connections_pending` | Waiting for connection |
| `hikaricp_connection_acquire_seconds` | Time to acquire a connection |

**Alert threshold:** `hikaricp_connections_active / 10 > 0.9` for 2+ minutes → pool exhaustion risk.

### Authentication

| Metric | Description |
|---|---|
| `placement_auth_login_success_total` | Successful logins |
| `placement_auth_login_failure_total` | Failed login attempts |
| `placement_auth_token_issued_total` | Access tokens issued |
| `placement_auth_refresh_total` | Token refresh operations |

### Outbox

| Metric | Description |
|---|---|
| `placement_outbox_events_pending` | Events waiting to be dispatched |
| `placement_outbox_dispatch_success_total` | Events dispatched successfully |
| `placement_outbox_dispatch_failure_total` | Failed dispatch attempts |
| `placement_outbox_dead_total` | Events moved to DEAD status |

**Alert threshold:** `placement_outbox_events_pending > 100` for 5+ minutes → dispatcher blocked.

### Event Bus

| Metric | Description |
|---|---|
| `placement_events_published_total{eventType}` | Events published per type |
| `placement_events_handled_total{handlerName}` | Events handled per handler |

### File Pipeline

| Metric | Description |
|---|---|
| `placement_files_uploaded_total` | Total files uploaded |
| `placement_files_scanned_total{result}` | Files scanned (CLEAN/INFECTED) |
| `placement_files_rejected_total{reason}` | Files rejected |

---

## Prometheus Configuration

See `monitoring/prometheus/prometheus.yml`:

```yaml
scrape_configs:
  - job_name: placement-platform
    scrape_interval: 15s
    static_configs:
      - targets: ['app:8081']
    metrics_path: /actuator/prometheus
```

**Authentication:** The `/actuator/prometheus` endpoint requires `ROLE_ADMIN`. In production, configure Prometheus with HTTP basic auth or use a network-level IP allowlist (nginx `allow` directive for the Prometheus server IP).

---

## Alert Rules

See `monitoring/alerts/alert_rules.yml` for full definitions. Key alerts:

| Alert | Condition | Severity |
|---|---|---|
| `PlacementPlatformDown` | Target not scraped for 1m | critical |
| `HighErrorRate` | HTTP 5xx > 1% for 5m | warning |
| `HighLatencyP95` | P95 > 1s for 5m | warning |
| `DatabaseConnectionPoolExhausted` | Active connections > 90% for 2m | critical |
| `HighMemoryUsage` | Heap > 80% for 10m | warning |
| `OutboxBacklogHigh` | Pending events > 100 for 5m | warning |
| `OutboxDeadEvents` | Dead events > 0 | warning |

---

## Structured Logs

In production, all logs are emitted as JSON via Logstash encoder:

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.college.placement.shared.security.JwtAuthenticationFilter",
  "message": "SECURITY_EVENT event=AUTHENTICATION_SUCCESS subject=user@example.com",
  "traceId": "abc123",
  "spanId": "def456",
  "requestId": "req-uuid"
}
```

### Key log patterns

```bash
# Authentication events
docker logs placement-prod-app 2>&1 | grep "SECURITY_EVENT"

# Failed logins
docker logs placement-prod-app 2>&1 | grep "event=AUTHENTICATION_FAILURE"

# Rate limit violations
docker logs placement-prod-app 2>&1 | grep "event=RATE_LIMIT_EXCEEDED"

# Startup validation
docker logs placement-prod-app 2>&1 | grep "STARTUP_VALIDATION"

# Outbox dispatch errors
docker logs placement-prod-app 2>&1 | grep "OutboxDispatcher" | grep "ERROR"
```

---

## Grafana Dashboard

Import `monitoring/grafana/dashboard.json` into Grafana:

1. Open Grafana → Dashboards → Import
2. Upload `monitoring/grafana/dashboard.json`
3. Select your Prometheus data source
4. Click Import

Dashboard panels:
- HTTP request rate and error rate
- P50/P95/P99 latency
- JVM memory usage (heap/non-heap)
- GC pause time
- HikariCP connection pool utilization
- Authentication success/failure rate
- Outbox backlog gauge

---

## Quick Reference

```bash
# Live metrics (raw Prometheus format)
curl -s http://localhost:8081/actuator/prometheus | grep "http_server_requests"

# Health check (without auth — liveness only)
curl -s http://localhost:8081/actuator/health/liveness | python3 -m json.tool

# Check active DB connections
curl -s http://localhost:8081/actuator/prometheus | grep hikaricp_connections_active

# Monitor logs in real time
docker logs placement-prod-app -f 2>&1 | jq -r '"\(.timestamp) [\(.level)] \(.message)"'
```
