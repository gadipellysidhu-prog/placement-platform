# Observability Guide — Placement Intelligence Platform

## Architecture Overview

The platform implements a three-layer observability stack:

| Layer | Implementation |
|---|---|
| Metrics | Micrometer → Prometheus → Grafana |
| Logs | SLF4J / Logback → JSON (prod) → ELK / CloudWatch |
| Tracing | Spring Observation API (OTEL-ready, no exporter yet) |

---

## Actuator Endpoints

| Endpoint | Public | Description |
|---|---|---|
| `/actuator/health` | Yes | Liveness + component health |
| `/actuator/health/liveness` | Yes | Kubernetes liveness probe |
| `/actuator/health/readiness` | Yes | Kubernetes readiness probe |
| `/actuator/info` | Yes | Build info |
| `/actuator/metrics` | Yes | Micrometer metric names |
| `/actuator/prometheus` | Yes | Prometheus scrape target |
| `/actuator/env` | Auth required | Environment properties (values sanitized) |
| `/actuator/configprops` | Auth required | Bound config properties (values sanitized) |

**Health detail visibility:** `show-details: when-authorized` in prod. Unauthenticated callers
receive only `{"status":"UP"}`. Authenticated users see component-level detail.
Dev profile overrides to `show-details: always`.

---

## Custom Health Indicators

| Component | Bean | Healthy When |
|---|---|---|
| `databaseQuery` | `DatabaseQueryHealthIndicator` | `SELECT 1` returns within timeout |
| `outbox` | `OutboxHealthIndicator` | Zero dead-letter events |
| `eventBus` | `EventBusHealthIndicator` | `EventPublisher` bean is available |

Dead-letter events (`status=DEAD`) set the `outbox` indicator to **DOWN**. This surfaces
outbox failures immediately in health dashboards and on-call alerts.

---

## Custom Metrics

### Auth Metrics (`shared.observability.metrics.AuthMetrics`)

| Metric | Type | Description |
|---|---|---|
| `auth.login.success.total` | Counter | Successful logins |
| `auth.login.failure.total` | Counter | Failed logins (wrong password, locked) |
| `auth.register.success.total` | Counter | Successful registrations |
| `auth.login.duration` | Timer | Login end-to-end duration |
| `auth.register.duration` | Timer | Register end-to-end duration |

### File Pipeline Metrics (`shared.observability.metrics.FilePipelineMetrics`)

| Metric | Type | Description |
|---|---|---|
| `file.upload.total` | Counter | Files uploaded |
| `file.scan.clean.total` | Counter | Files scanned clean |
| `file.scan.infected.total` | Counter | Files quarantined |
| `file.scan.failed.total` | Counter | Scan errors |
| `file.scan.duration` | Timer | ClamAV scan duration |

### Outbox Metrics (`shared.outbox.metrics.OutboxMetrics`)

| Metric | Type | Description |
|---|---|---|
| `outbox.pending.count` | Gauge | Current PENDING events |
| `outbox.deadletter.count` | Gauge | Current DEAD events |
| `outbox.processed.count` | Counter | Events marked SENT |
| `outbox.failed.count` | Counter | Events marked FAILED |
| `outbox.dispatch.duration` | Timer | Dispatcher cycle duration |

### Platform Gauges (`shared.observability.metrics.PlatformGauges`)

| Metric | Type | Description |
|---|---|---|
| `platform.users.total` | Gauge | Total registered users |

### Event Bus Metrics (`shared.eventbus.handler.MetricsPlaceholderHandler`)

| Metric | Type | Description |
|---|---|---|
| `domain.events.published` | Counter | All domain events (by type tag) |
| `students.registered` | Counter | StudentCreatedEvent count |
| `job_postings.opened` | Counter | JobPostingOpenedEvent count |
| `applications.submitted` | Counter | ApplicationSubmittedEvent count |
| `offers.accepted` | Counter | OfferAcceptedEvent count |

---

## Request Correlation

`RequestCorrelationFilter` runs at highest precedence on every HTTP request:

1. Reads `X-Request-ID` header (or generates a UUID if absent)
2. Generates a new `traceId` UUID per request
3. Puts both into MDC (`traceId`, `requestId`)
4. Sets `X-Trace-ID` and `X-Request-ID` response headers
5. Logs `REQUEST_RECEIVED` and `REQUEST_COMPLETED` at INFO with method, URI, status, and duration
6. Clears MDC after the response

All log lines emitted within the same request automatically carry `traceId` and `requestId`
because they write to the same thread's MDC.

---

## Structured Logging

### Dev / Test Profile
Console output includes MDC fields:
```
2026-06-22 10:00:00.000 [main] INFO  c.c.p.s.o.b.OutboxEventBridge [abc123] [req-456] - OUTBOX_BRIDGE_RECEIVED ...
```

### Prod Profile
JSON via `logstash-logback-encoder`. MDC fields are auto-included. Static field `service` is set.
```json
{
  "@timestamp": "2026-06-22T10:00:00.000Z",
  "level": "INFO",
  "message": "REQUEST_COMPLETED method=POST uri=/auth/login status=200 durationMs=43",
  "logger": "c.c.p.s.o.w.RequestCorrelationFilter",
  "traceId": "a1b2c3d4-...",
  "requestId": "x9y8z7...",
  "service": "placement-platform"
}
```

---

## Tracing Readiness

`ObservabilityConfig` registers `ObservedAspect` which activates `@Observed` annotation support
on any Spring bean method. The `ObservationRegistry` is auto-configured by Spring Boot.

To add full distributed tracing (OTEL):
1. Add `io.micrometer:micrometer-tracing-bridge-otel`
2. Add `io.opentelemetry:opentelemetry-exporter-otlp`
3. Configure `management.tracing.sampling.probability=1.0`
4. Wire an OTLP exporter URL in `application.yml`

No code changes to `ObservabilityConfig` are required — Spring Boot auto-wires OTEL exporters
when they're on the classpath.

---

## Prometheus Setup

```bash
# Start Prometheus with the provided config
docker run -p 9090:9090 \
  -v $(pwd)/monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

The scrape target is `localhost:8081/actuator/prometheus` with a 10-second interval.

---

## Grafana Dashboard

Import `monitoring/grafana/dashboard.json` into Grafana with a Prometheus data source.

Panels included:
- Application status (UP/DOWN)
- JVM heap usage (%)
- Registered users (gauge)
- Outbox dead-letter events (alert threshold)
- HTTP request rate + P50/P95 latency
- Auth login success/failure rate
- Outbox pending events
- File scan results (clean / infected)

---

## Alert Rules

Import `monitoring/alerts/alert_rules.yml` into Prometheus or Alertmanager.

| Alert | Severity | Condition |
|---|---|---|
| `OutboxDeadLetterEvents` | critical | `outbox_deadletter_count > 0` |
| `OutboxHighFailureRate` | warning | `outbox_failed_count > 20` for 5m |
| `HighAuthLoginFailureRate` | warning | login failures > 10/s for 2m |
| `ApplicationDown` | critical | `up == 0` for 1m |
| `HighJvmHeapUsage` | warning | heap > 85% for 5m |
| `SlowHttpRequests` | warning | P95 > 2s for 5m |
| `FileScanInfectedFilesDetected` | critical | infected files in last 10m |
