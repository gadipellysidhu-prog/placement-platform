#!/usr/bin/env bash
# backup-postgres.sh — Point-in-time PostgreSQL backup for Placement Platform
#
# Usage:
#   ./scripts/backup-postgres.sh                        # uses defaults from .env.prod
#   ./scripts/backup-postgres.sh --env-file .env.prod   # explicit env file
#   ./scripts/backup-postgres.sh --container placement-prod-postgres
#
# Output:
#   backups/placement_prod_YYYYMMDD_HHMMSS.sql.gz
#
# Schedule (cron — daily at 02:00):
#   0 2 * * * /opt/placement-platform/scripts/backup-postgres.sh >> /var/log/placement/backup.log 2>&1
#
# Retention: keeps last 30 daily backups; deletes older files automatically.

set -euo pipefail

# ── Configuration defaults ────────────────────────────────────────────────────
CONTAINER="${BACKUP_CONTAINER:-placement-prod-postgres}"
BACKUP_DIR="${BACKUP_DIR:-$(dirname "$0")/../backups}"
RETAIN_DAYS="${RETAIN_DAYS:-30}"
ENV_FILE=""

# ── Parse arguments ───────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --container) CONTAINER="$2"; shift 2 ;;
        --env-file)  ENV_FILE="$2";  shift 2 ;;
        --retain)    RETAIN_DAYS="$2"; shift 2 ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

# ── Load env file if specified ────────────────────────────────────────────────
if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
    set -o allexport
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +o allexport
fi

# ── Resolve credentials ───────────────────────────────────────────────────────
DB_NAME="${DB_NAME:-placement_prod}"
DB_USERNAME="${DB_USERNAME:?DB_USERNAME is required (set in env or .env.prod)}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR=$(realpath "$BACKUP_DIR")
BACKUP_FILE="${BACKUP_DIR}/placement_${DB_NAME}_${TIMESTAMP}.sql.gz"

# ── Pre-flight checks ─────────────────────────────────────────────────────────
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo "ERROR: Container '$CONTAINER' is not running." >&2
    exit 1
fi

mkdir -p "$BACKUP_DIR"

# ── Execute backup ────────────────────────────────────────────────────────────
echo "[$(date -Iseconds)] Starting backup: $DB_NAME → $(basename "$BACKUP_FILE")"

docker exec "$CONTAINER" \
    pg_dump -U "$DB_USERNAME" -d "$DB_NAME" \
    --format=plain \
    --no-password \
    --verbose 2>&1 \
    | gzip -9 > "$BACKUP_FILE"

BACKUP_SIZE=$(du -sh "$BACKUP_FILE" | cut -f1)
echo "[$(date -Iseconds)] Backup complete: $BACKUP_FILE ($BACKUP_SIZE)"

# ── Verify backup is non-empty ────────────────────────────────────────────────
if [[ ! -s "$BACKUP_FILE" ]]; then
    echo "ERROR: Backup file is empty — removing." >&2
    rm -f "$BACKUP_FILE"
    exit 1
fi

# ── Rotate old backups ────────────────────────────────────────────────────────
DELETED=$(find "$BACKUP_DIR" -name "placement_${DB_NAME}_*.sql.gz" \
    -mtime "+${RETAIN_DAYS}" -print -delete | wc -l)
echo "[$(date -Iseconds)] Retention: removed $DELETED backup(s) older than ${RETAIN_DAYS} days"

# ── Print restore instructions ────────────────────────────────────────────────
echo ""
echo "To restore this backup:"
echo "  ./scripts/restore-postgres.sh --file $BACKUP_FILE"
echo ""
