#!/bin/bash
# Log retention cleanup: removes old OpenSearch indices and SEUXDR queue files.
# Triggered by secureu-cleanup.timer every 15 minutes; only acts when disk
# usage on / reaches DISK_THRESHOLD percent (default: 80).
#
# Pass --force to skip the threshold check (useful for manual runs).
#
# Retention defaults (override via env):
#   DISK_THRESHOLD        — trigger cleanup above this % disk usage (default: 80)
#   INDEX_RETENTION_DAYS  — delete OpenSearch indices older than N days (default: 30)
#   QUEUE_RETENTION_DAYS  — delete SEUXDR queue log files older than N days (default: 7)

set -euo pipefail

DISK_THRESHOLD="${DISK_THRESHOLD:-80}"
INDEX_RETENTION_DAYS="${INDEX_RETENTION_DAYS:-30}"
QUEUE_RETENTION_DAYS="${QUEUE_RETENTION_DAYS:-7}"
FORCE=0
[ "${1:-}" = "--force" ] && FORCE=1

BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"
SEUXDR_ENV="$BACKEND_DIR/seuxdr/manager/.env"
QUEUE_DIR="/var/lib/docker/volumes/seuxdr_seuxdr-queue/_data"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

# ─────────────────────────────────────────────
# Disk usage gate
# ─────────────────────────────────────────────
DISK_USED=$(df / | awk 'NR==2 {gsub(/%/,"",$5); print $5}')
if [ "$FORCE" -eq 0 ] && [ "${DISK_USED:-0}" -lt "$DISK_THRESHOLD" ]; then
    # Silent exit — nothing to do, avoid noisy journal entries
    exit 0
fi

log "=== SECUR-EU log cleanup triggered (disk ${DISK_USED}% >= threshold ${DISK_THRESHOLD}%, index_retention=${INDEX_RETENTION_DAYS}d, queue_retention=${QUEUE_RETENTION_DAYS}d) ==="

# ─────────────────────────────────────────────
# 1. OpenSearch index cleanup
# ─────────────────────────────────────────────
# Indices are named suricata-YYYY.MM, mirai-*-YYYY.MM, wazuh-alerts-4.x-YYYY.MM.DD
# Credentials live inside the seuxdr-manager container at /seuxdr/manager/.env.

cleanup_opensearch() {
    # Prefer the host-side .env; fall back to reading it from inside the container
    local env_content=""
    if [ -f "$SEUXDR_ENV" ]; then
        env_content=$(cat "$SEUXDR_ENV")
    elif docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^seuxdr-manager$'; then
        env_content=$(docker exec seuxdr-manager cat /seuxdr/manager/.env 2>/dev/null || true)
    fi

    if [ -z "$env_content" ]; then
        log "WARN: Could not read seuxdr .env — skipping OpenSearch cleanup"
        return
    fi

    # Parse credentials (strip surrounding quotes)
    OS_USER=$(echo "$env_content" | grep '^INDEXER_USERNAME' | cut -d= -f2- | tr -d "'" | tr -d '"')
    OS_PASS=$(echo "$env_content" | grep '^INDEXER_PASSWORD' | cut -d= -f2- | tr -d "'" | tr -d '"')

    if [ -z "$OS_USER" ] || [ -z "$OS_PASS" ]; then
        log "WARN: Could not read OpenSearch credentials — skipping index cleanup"
        return
    fi

    CUTOFF=$(date -d "-${INDEX_RETENTION_DAYS} days" '+%Y.%m.%d')
    CUTOFF_MONTH=$(date -d "-${INDEX_RETENTION_DAYS} days" '+%Y.%m')

    log "OpenSearch: removing indices older than ${INDEX_RETENTION_DAYS} days (cutoff ~${CUTOFF_MONTH})"

    # Fetch index list via docker exec (OpenSearch binds to 127.0.0.1 inside container)
    INDICES=$(docker exec seuxdr-manager \
        curl -sk -u "$OS_USER:$OS_PASS" \
        "https://127.0.0.1:9200/_cat/indices?h=index" 2>/dev/null || true)

    if [ -z "$INDICES" ]; then
        log "WARN: Could not reach OpenSearch — skipping index cleanup"
        return
    fi

    deleted=0
    while IFS= read -r idx; do
        idx=$(echo "$idx" | tr -d '[:space:]')
        [ -z "$idx" ] && continue

        # Extract date suffix — supports YYYY.MM.DD and YYYY.MM
        if [[ "$idx" =~ ([0-9]{4}\.[0-9]{2}\.[0-9]{2})$ ]]; then
            [ "${BASH_REMATCH[1]}" \< "$CUTOFF" ] || continue
        elif [[ "$idx" =~ ([0-9]{4}\.[0-9]{2})$ ]]; then
            [ "${BASH_REMATCH[1]}" \< "$CUTOFF_MONTH" ] || continue
        else
            continue  # not a dated index we manage
        fi

        if docker exec seuxdr-manager \
            curl -sk -u "$OS_USER:$OS_PASS" \
            -X DELETE "https://127.0.0.1:9200/$idx" > /dev/null 2>&1; then
            log "  Deleted index: $idx"
            deleted=$((deleted + 1))
        else
            log "  WARN: Failed to delete index: $idx"
        fi
    done <<< "$INDICES"

    log "OpenSearch: deleted $deleted indices"
}

# ─────────────────────────────────────────────
# 2. SEUXDR queue log file cleanup
# ─────────────────────────────────────────────
cleanup_queue() {
    if [ ! -d "$QUEUE_DIR" ]; then
        log "WARN: Queue dir $QUEUE_DIR not found — skipping"
        return
    fi

    log "Queue logs: removing files older than ${QUEUE_RETENTION_DAYS} days from $QUEUE_DIR"
    count=$(find "$QUEUE_DIR" -type f -name "*.log" -mtime "+${QUEUE_RETENTION_DAYS}" | wc -l)
    find "$QUEUE_DIR" -type f -name "*.log" -mtime "+${QUEUE_RETENTION_DAYS}" -delete
    log "Queue logs: deleted $count files"
}

# ─────────────────────────────────────────────
# 3. Docker system prune (dangling images/volumes only — safe)
# ─────────────────────────────────────────────
cleanup_docker() {
    log "Docker: pruning dangling images and build cache..."
    docker image prune -f --filter "dangling=true" > /dev/null 2>&1 || true
    docker builder prune -f --keep-storage 2g > /dev/null 2>&1 || true
    log "Docker: prune complete"
}

cleanup_opensearch
cleanup_queue
cleanup_docker

DISK_AFTER=$(df / | awk 'NR==2 {gsub(/%/,"",$5); print $5}')
log "=== Cleanup complete (disk usage: ${DISK_USED}% → ${DISK_AFTER}%) ==="
