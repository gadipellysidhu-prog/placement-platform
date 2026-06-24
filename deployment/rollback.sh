#!/usr/bin/env bash
# rollback.sh — Automated rollback for Placement Platform
#
# Usage:
#   ./deployment/rollback.sh                            # roll back to previous image tag
#   ./deployment/rollback.sh --version 1.1.0            # roll back to specific version
#   ./deployment/rollback.sh --restore-db               # also restore last backup
#   ./deployment/rollback.sh --env-file /etc/.env.prod

set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env.prod}"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.prod.yml"
ROLLBACK_VERSION=""
RESTORE_DB=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)    ROLLBACK_VERSION="$2"; shift 2 ;;
        --env-file)   ENV_FILE="$2";         shift 2 ;;
        --restore-db) RESTORE_DB=true;       shift ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

set -o allexport
# shellcheck disable=SC1090
source "$ENV_FILE"
set +o allexport

echo ""
echo "============================================================"
echo "  Placement Platform — ROLLBACK"
echo "  $(date -Iseconds)"
echo "============================================================"

# ── Determine rollback version ────────────────────────────────────────────────
if [[ -n "$ROLLBACK_VERSION" ]]; then
    TARGET_VERSION="$ROLLBACK_VERSION"
else
    # Find the last available image (second-most-recent tag)
    TARGET_VERSION=$(docker images placement-platform \
        --format '{{.Tag}}' \
        | grep -v 'ci\|latest' \
        | sort -Vr \
        | sed -n '2p')
    if [[ -z "$TARGET_VERSION" ]]; then
        echo "ERROR: No previous image found for rollback." >&2
        echo "       Use --version <tag> to specify explicitly." >&2
        exit 1
    fi
fi

echo "  Rolling back to: placement-platform:${TARGET_VERSION}"
echo ""

# ── Optional DB restore ───────────────────────────────────────────────────────
if [[ "$RESTORE_DB" == "true" ]]; then
    LATEST_BACKUP=$(find "$DEPLOY_DIR/backups" -name "placement_*.sql.gz" \
        ! -name "*PRE_RESTORE*" | sort -r | head -1)
    if [[ -n "$LATEST_BACKUP" ]]; then
        echo "Restoring database from: $(basename "$LATEST_BACKUP")"
        "$DEPLOY_DIR/scripts/restore-postgres.sh" \
            --file "$LATEST_BACKUP" \
            --env-file "$ENV_FILE" \
            --yes
    else
        echo "WARNING: No backup found for DB restore — skipping."
    fi
fi

# ── Set target version and restart ───────────────────────────────────────────
export APP_VERSION="$TARGET_VERSION"

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" \
    up -d --no-deps --force-recreate app nginx

# ── Health check ──────────────────────────────────────────────────────────────
echo "Waiting for health check..."
APP_PORT="${SERVER_PORT:-8081}"
for i in $(seq 1 24); do
    sleep 5
    STATUS=$(docker exec placement-prod-app \
        wget -q --tries=1 -O- "http://localhost:${APP_PORT}/actuator/health/readiness" \
        2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" \
        2>/dev/null || true)
    echo "  [${i} × 5s] status=${STATUS:-unknown}"
    if [[ "$STATUS" == "UP" ]]; then
        echo ""
        echo "============================================================"
        echo "  ROLLBACK SUCCEEDED: placement-platform v${TARGET_VERSION}"
        echo "  $(date -Iseconds)"
        echo "============================================================"
        exit 0
    fi
done

echo "ERROR: Rollback health check failed. Manual intervention required." >&2
echo "       Logs: docker logs placement-prod-app --tail 100" >&2
exit 1
