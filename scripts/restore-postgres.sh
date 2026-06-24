#!/usr/bin/env bash
# restore-postgres.sh — Restore PostgreSQL backup for Placement Platform
#
# Usage:
#   ./scripts/restore-postgres.sh --file backups/placement_prod_20240101_020000.sql.gz
#   ./scripts/restore-postgres.sh --file backups/placement_prod_20240101_020000.sql.gz \
#                                  --container placement-prod-postgres \
#                                  --env-file .env.prod
#
# WARNING: This is a DESTRUCTIVE operation. It drops and recreates the target
#          database. Always verify the target before running.

set -euo pipefail

CONTAINER="${RESTORE_CONTAINER:-placement-prod-postgres}"
BACKUP_FILE=""
ENV_FILE=""
SKIP_CONFIRM="${SKIP_CONFIRM:-false}"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --file)      BACKUP_FILE="$2";  shift 2 ;;
        --container) CONTAINER="$2";    shift 2 ;;
        --env-file)  ENV_FILE="$2";     shift 2 ;;
        --yes)       SKIP_CONFIRM=true; shift ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

# ── Load env file ─────────────────────────────────────────────────────────────
if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
    set -o allexport
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +o allexport
fi

DB_NAME="${DB_NAME:-placement_prod}"
DB_USERNAME="${DB_USERNAME:?DB_USERNAME is required}"

# ── Validate inputs ───────────────────────────────────────────────────────────
if [[ -z "$BACKUP_FILE" ]]; then
    echo "ERROR: --file <backup.sql.gz> is required" >&2
    exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "ERROR: Backup file not found: $BACKUP_FILE" >&2
    exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo "ERROR: Container '$CONTAINER' is not running." >&2
    exit 1
fi

# ── Safety confirmation ───────────────────────────────────────────────────────
echo ""
echo "=================================================="
echo "  RESTORE OPERATION — DESTRUCTIVE"
echo "=================================================="
echo "  File:      $(basename "$BACKUP_FILE")"
echo "  Database:  $DB_NAME"
echo "  Container: $CONTAINER"
echo "=================================================="
echo ""

if [[ "$SKIP_CONFIRM" != "true" ]]; then
    read -rp "This will DROP and recreate '$DB_NAME'. Type 'yes' to continue: " CONFIRM
    if [[ "$CONFIRM" != "yes" ]]; then
        echo "Restore cancelled."
        exit 0
    fi
fi

# ── Take pre-restore backup ───────────────────────────────────────────────────
PRE_RESTORE_BACKUP="backups/placement_${DB_NAME}_PRE_RESTORE_$(date +%Y%m%d_%H%M%S).sql.gz"
mkdir -p backups
echo "[$(date -Iseconds)] Taking pre-restore snapshot → $PRE_RESTORE_BACKUP"
docker exec "$CONTAINER" \
    pg_dump -U "$DB_USERNAME" -d "$DB_NAME" --no-password 2>/dev/null \
    | gzip -9 > "$PRE_RESTORE_BACKUP" \
    || echo "WARNING: Pre-restore backup failed (database may not exist yet)."

# ── Stop app container to avoid writes during restore ────────────────────────
if docker ps --format '{{.Names}}' | grep -q "placement-prod-app"; then
    echo "[$(date -Iseconds)] Stopping application container..."
    docker stop placement-prod-app
fi

# ── Drop and recreate database ────────────────────────────────────────────────
echo "[$(date -Iseconds)] Dropping database '$DB_NAME'..."
docker exec "$CONTAINER" psql -U "$DB_USERNAME" -d postgres \
    -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();" \
    > /dev/null

docker exec "$CONTAINER" psql -U "$DB_USERNAME" -d postgres \
    -c "DROP DATABASE IF EXISTS ${DB_NAME};" > /dev/null

docker exec "$CONTAINER" psql -U "$DB_USERNAME" -d postgres \
    -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USERNAME};" > /dev/null

echo "[$(date -Iseconds)] Restoring from $(basename "$BACKUP_FILE")..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER" \
    psql -U "$DB_USERNAME" -d "$DB_NAME" -q

echo "[$(date -Iseconds)] Restore complete."

# ── Restart application ───────────────────────────────────────────────────────
echo "[$(date -Iseconds)] Restarting application container..."
docker start placement-prod-app 2>/dev/null || true

echo ""
echo "=================================================="
echo "  RESTORE SUCCEEDED"
echo "  Pre-restore backup: $PRE_RESTORE_BACKUP"
echo "=================================================="
echo ""
