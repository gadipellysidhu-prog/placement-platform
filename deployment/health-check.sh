#!/usr/bin/env bash
# health-check.sh — Full stack health validation for Placement Platform
#
# Usage:
#   ./deployment/health-check.sh
#   ./deployment/health-check.sh --host api.example.com --port 443 --tls
#   ./deployment/health-check.sh --env-file .env.prod

set -euo pipefail

HOST="${HEALTH_HOST:-localhost}"
PORT="${HEALTH_PORT:-8081}"
USE_TLS=false
ENV_FILE=""
COMPOSE_FILE="$(cd "$(dirname "$0")/.." && pwd)/docker-compose.prod.yml"
DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --host)     HOST="$2";    shift 2 ;;
        --port)     PORT="$2";    shift 2 ;;
        --tls)      USE_TLS=true; shift ;;
        --env-file) ENV_FILE="$2"; shift 2 ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
    set -o allexport; source "$ENV_FILE"; set +o allexport
fi

SCHEME="http"
if [[ "$USE_TLS" == "true" ]]; then SCHEME="https"; fi
BASE_URL="${SCHEME}://${HOST}:${PORT}"

PASS=0
FAIL=0

check() {
    local name="$1"
    local result="$2"
    local expected="$3"
    if echo "$result" | grep -q "$expected"; then
        echo "  [PASS] $name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $name (got: ${result:0:80})"
        FAIL=$((FAIL + 1))
    fi
}

echo ""
echo "============================================================"
echo "  Placement Platform — Health Check"
echo "  Target: $BASE_URL"
echo "  $(date -Iseconds)"
echo "============================================================"
echo ""

# ── Docker container states ───────────────────────────────────────────────────
echo "-- Container Status --"
for svc in placement-prod-postgres placement-prod-app placement-prod-nginx; do
    STATE=$(docker inspect --format '{{.State.Health.Status}}' "$svc" 2>/dev/null || echo "not-running")
    if [[ "$STATE" == "healthy" ]]; then
        echo "  [PASS] $svc → $STATE"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $svc → $STATE"
        FAIL=$((FAIL + 1))
    fi
done

echo ""
echo "-- Spring Boot Actuator --"

# Liveness probe
LIVE=$(curl -sf --max-time 5 "http://localhost:8081/actuator/health/liveness" 2>/dev/null || echo "")
check "liveness probe" "$LIVE" '"status":"UP"'

# Readiness probe
READY=$(curl -sf --max-time 5 "http://localhost:8081/actuator/health/readiness" 2>/dev/null || echo "")
check "readiness probe" "$READY" '"status":"UP"'

# Overall health
HEALTH=$(curl -sf --max-time 5 "http://localhost:8081/actuator/health" 2>/dev/null || echo "")
check "overall health" "$HEALTH" '"status":"UP"'

echo ""
echo "-- API Endpoints --"

# Auth endpoint reachable (401 is expected without a token)
AUTH_STATUS=$(curl -so /dev/null -w "%{http_code}" --max-time 5 \
    -X POST "http://localhost:8081/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"health@check","password":"check"}' 2>/dev/null || echo "000")
if [[ "$AUTH_STATUS" == "401" || "$AUTH_STATUS" == "400" ]]; then
    echo "  [PASS] POST /auth/login → HTTP $AUTH_STATUS (auth endpoint reachable)"
    PASS=$((PASS + 1))
else
    echo "  [FAIL] POST /auth/login → HTTP $AUTH_STATUS (expected 400 or 401)"
    FAIL=$((FAIL + 1))
fi

# Protected endpoint returns 401 without token
PROT_STATUS=$(curl -so /dev/null -w "%{http_code}" --max-time 5 \
    "http://localhost:8081/students" 2>/dev/null || echo "000")
if [[ "$PROT_STATUS" == "401" || "$PROT_STATUS" == "403" ]]; then
    echo "  [PASS] GET /students → HTTP $PROT_STATUS (auth enforcement working)"
    PASS=$((PASS + 1))
else
    echo "  [FAIL] GET /students → HTTP $PROT_STATUS (expected 401 or 403)"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "-- Security Headers (via nginx) --"
if [[ "$USE_TLS" == "true" ]]; then
    HEADERS=$(curl -sI --max-time 5 "${BASE_URL}/actuator/health" 2>/dev/null || echo "")
    check "HSTS header" "$HEADERS" "Strict-Transport-Security"
    check "X-Frame-Options" "$HEADERS" "X-Frame-Options"
    check "X-Content-Type-Options" "$HEADERS" "nosniff"
else
    echo "  [SKIP] Security headers (run with --tls to verify via nginx)"
fi

echo ""
echo "-- Disk Space --"
DISK_PCT=$(df -h / | awk 'NR==2{print $5}' | tr -d '%')
if [[ "$DISK_PCT" -lt 85 ]]; then
    echo "  [PASS] Disk usage: ${DISK_PCT}% (< 85%)"
    PASS=$((PASS + 1))
else
    echo "  [WARN] Disk usage: ${DISK_PCT}% (>= 85% — consider cleanup)"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "============================================================"
echo "  Results: ${PASS} passed, ${FAIL} failed"
echo "============================================================"
echo ""

[[ "$FAIL" -eq 0 ]] && exit 0 || exit 1
