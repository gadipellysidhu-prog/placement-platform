#!/usr/bin/env bash
# deploy.sh — Zero-downtime production deployment for Placement Platform
#
# Target: Ubuntu 24.04 VPS with Docker + Docker Compose installed
#
# Usage:
#   ./deployment/deploy.sh                              # uses APP_VERSION from .env.prod
#   ./deployment/deploy.sh --version 1.2.0
#   ./deployment/deploy.sh --env-file /etc/placement/.env.prod
#   ./deployment/deploy.sh --skip-backup               # skip pre-deploy DB backup
#
# Prerequisites on VPS:
#   - Docker Engine 24+ and Docker Compose v2+
#   - Git repository cloned to DEPLOY_DIR
#   - .env.prod populated (run scripts/generate-prod-keys.ps1 once)
#   - nginx/certs/ contains fullchain.pem + privkey.pem
#
# Zero-downtime strategy:
#   1. Pull new image (build locally or pull from registry)
#   2. Pre-deploy backup
#   3. Recreate app + nginx containers (postgres stays running)
#   4. Health check gate (60-second window)
#   5. Rollback automatically if health check fails

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
DEPLOY_DIR="${DEPLOY_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env.prod}"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.prod.yml"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-120}"
SKIP_BACKUP="${SKIP_BACKUP:-false}"
APP_VERSION_OVERRIDE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)     APP_VERSION_OVERRIDE="$2"; shift 2 ;;
        --env-file)    ENV_FILE="$2";              shift 2 ;;
        --skip-backup) SKIP_BACKUP=true;           shift ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

# ── Validate environment ──────────────────────────────────────────────────────
if [[ ! -f "$ENV_FILE" ]]; then
    echo "ERROR: .env.prod not found at $ENV_FILE" >&2
    echo "       Copy .env.example to .env.prod and fill in all required values." >&2
    exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
    echo "ERROR: docker-compose.prod.yml not found at $COMPOSE_FILE" >&2
    exit 1
fi

# Load env
set -o allexport
# shellcheck disable=SC1090
source "$ENV_FILE"
set +o allexport

APP_VERSION="${APP_VERSION_OVERRIDE:-${APP_VERSION:-1.0.0}}"

echo ""
echo "============================================================"
echo "  Placement Platform — Deploy v${APP_VERSION}"
echo "  $(date -Iseconds)"
echo "============================================================"
echo "  Deploy dir: $DEPLOY_DIR"
echo "  Env file:   $ENV_FILE"
echo ""

# ── Pull latest code ──────────────────────────────────────────────────────────
echo "[1/6] Pulling latest code from git..."
cd "$DEPLOY_DIR"
git fetch origin
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
git pull origin "$CURRENT_BRANCH"
echo "      Branch: $CURRENT_BRANCH @ $(git rev-parse --short HEAD)"

# ── Pre-deploy database backup ────────────────────────────────────────────────
if [[ "$SKIP_BACKUP" == "false" ]]; then
    echo "[2/6] Taking pre-deploy database backup..."
    BACKUP_CONTAINER="${BACKUP_CONTAINER:-placement-prod-postgres}"
    if docker ps --format '{{.Names}}' | grep -q "^${BACKUP_CONTAINER}$"; then
        "$DEPLOY_DIR/scripts/backup-postgres.sh" \
            --container "$BACKUP_CONTAINER" \
            --env-file "$ENV_FILE" \
            --retain 30
    else
        echo "      WARNING: postgres container not running — skipping backup."
    fi
else
    echo "[2/6] Skipping backup (--skip-backup specified)."
fi

# ── Build new image ───────────────────────────────────────────────────────────
echo "[3/6] Building Docker image placement-platform:${APP_VERSION}..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build app
echo "      Build complete."

# ── Record previous container IDs for rollback ───────────────────────────────
PREV_APP_ID=$(docker ps -q --filter "name=placement-prod-app" || true)

# ── Rolling restart: app + nginx (postgres untouched) ────────────────────────
echo "[4/6] Recreating app and nginx containers..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" \
    up -d --no-deps --force-recreate app nginx

echo "      Containers recreated. Waiting for health checks..."

# ── Health check gate ─────────────────────────────────────────────────────────
echo "[5/6] Health gate (${HEALTH_TIMEOUT}s timeout)..."
APP_PORT="${SERVER_PORT:-8081}"
ELAPSED=0
HEALTHY=false
while [[ $ELAPSED -lt $HEALTH_TIMEOUT ]]; do
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    STATUS=$(docker exec placement-prod-app \
        wget -q --tries=1 -O- "http://localhost:${APP_PORT}/actuator/health/readiness" \
        2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" \
        2>/dev/null || true)
    echo "      [${ELAPSED}s] status=${STATUS:-unknown}"
    if [[ "$STATUS" == "UP" ]]; then
        HEALTHY=true
        break
    fi
done

if [[ "$HEALTHY" != "true" ]]; then
    echo ""
    echo "ERROR: Health check failed after ${HEALTH_TIMEOUT}s — initiating rollback."
    echo ""
    exec "$DEPLOY_DIR/deployment/rollback.sh" --env-file "$ENV_FILE"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo "[6/6] Deployment successful."
echo ""
echo "============================================================"
echo "  DEPLOYED: placement-platform v${APP_VERSION}"
echo "  $(date -Iseconds)"
echo "============================================================"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
echo ""
