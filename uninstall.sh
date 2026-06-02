#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# SECUR-EU Platform — Uninstall Script
# ═══════════════════════════════════════════════════════════════
#
# Usage:
#   sudo ./uninstall.sh
#
# By default removes: systemd units, all Docker containers/volumes,
# platform processes, sudoers entry, and the platform directory.
#
# Optional flags:
#   --remove-docker     Also remove Docker Engine and Docker Compose
#   --remove-java       Also remove Temurin JDK
#   --keep-dir          Keep the platform directory (files on disk)
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

INSTALL_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$INSTALL_DIR/backend"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
step()  { echo -e "\n${GREEN}───${NC} $1"; }

REMOVE_DOCKER=0
REMOVE_JAVA=0
KEEP_DIR=0

for arg in "$@"; do
    case "$arg" in
        --remove-docker) REMOVE_DOCKER=1 ;;
        --remove-java)   REMOVE_JAVA=1   ;;
        --keep-dir)      KEEP_DIR=1      ;;
        *) echo "Unknown option: $arg"; exit 1 ;;
    esac
done

if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}[ERROR]${NC} Please run as root: sudo ./uninstall.sh"
    exit 1
fi

REAL_USER="${SUDO_USER:-$USER}"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  SECUR-EU Platform Uninstaller"
echo "═══════════════════════════════════════════════════════════"
echo "  Install dir : $INSTALL_DIR"
echo "  User        : $REAL_USER"
echo "  Remove Docker: $([ $REMOVE_DOCKER -eq 1 ] && echo yes || echo no)"
echo "  Remove Java : $([ $REMOVE_JAVA -eq 1 ] && echo yes || echo no)"
echo "  Keep dir    : $([ $KEEP_DIR -eq 1 ] && echo yes || echo no)"
echo "═══════════════════════════════════════════════════════════"
echo ""
read -r -p "This will permanently destroy all platform data. Continue? [y/N] " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }

# ─────────────────────────────────────────────
# 1. Systemd services and timer
# ─────────────────────────────────────────────
step "Stopping and removing systemd units..."
for unit in secureu-backend secureu-frontend secureu-cleanup.timer secureu-cleanup; do
    systemctl stop "$unit" 2>/dev/null && info "Stopped $unit" || true
    systemctl disable "$unit" 2>/dev/null || true
done
rm -f /etc/systemd/system/secureu-backend.service \
      /etc/systemd/system/secureu-frontend.service \
      /etc/systemd/system/secureu-cleanup.service \
      /etc/systemd/system/secureu-cleanup.timer
systemctl daemon-reload
info "Systemd units removed"

# ─────────────────────────────────────────────
# 2. Running processes
# ─────────────────────────────────────────────
step "Killing platform processes..."
pkill -f "data-traffic-monitoring.*\.jar" 2>/dev/null && info "DTM stopped" || true
pkill -f "anomaly-detection.*\.jar"       2>/dev/null && info "AD stopped"  || true
pkill -f "$BACKEND_DIR/pentest/bin/server" 2>/dev/null && info "Pentest server stopped" || true

# ─────────────────────────────────────────────
# 3. Docker compose stacks (containers + volumes)
# ─────────────────────────────────────────────
step "Tearing down Docker compose stacks..."
for compose_file in \
    "$BACKEND_DIR/pentest/docker_compose.yaml" \
    "$BACKEND_DIR/sslchecker/docker-compose.yml" \
    "$BACKEND_DIR/vsp/docker-compose.yml" \
    "$BACKEND_DIR/darkweb/docker-compose.yml" \
    "$BACKEND_DIR/redflags/docker-compose.yml" \
    "$BACKEND_DIR/seuxdr/docker-compose.yml" \
    "$BACKEND_DIR/sqs/docker-compose.yml" \
    "$BACKEND_DIR/dtmad/monitoring/docker-compose.yml"
do
    if [ -f "$compose_file" ]; then
        docker compose -f "$compose_file" down -v --remove-orphans 2>/dev/null \
            && info "Down: $(basename "$(dirname "$compose_file")")" || true
    fi
done

# ─────────────────────────────────────────────
# 4. Infrastructure containers (started via docker run)
# ─────────────────────────────────────────────
step "Removing infrastructure containers..."
for container in kafka-dtm zookeeper-dtm sphinx-postgres; do
    docker stop "$container" 2>/dev/null || true
    docker rm -v "$container" 2>/dev/null && info "Removed $container" || true
done

# ─────────────────────────────────────────────
# 5. Docker images
# ─────────────────────────────────────────────
step "Pruning Docker images..."
docker image prune -a -f 2>/dev/null && info "Docker images pruned" || true

# ─────────────────────────────────────────────
# 6. Sudoers entry
# ─────────────────────────────────────────────
step "Removing sudoers entry..."
if [ -f "/etc/sudoers.d/$REAL_USER" ]; then
    rm -f "/etc/sudoers.d/$REAL_USER"
    info "Removed /etc/sudoers.d/$REAL_USER"
else
    warn "No sudoers entry found for $REAL_USER — skipping"
fi

# ─────────────────────────────────────────────
# 7. Platform directory
# ─────────────────────────────────────────────
if [ "$KEEP_DIR" -eq 0 ]; then
    step "Removing platform directory..."
    # Run from parent so we're not deleting our cwd
    PARENT="$(dirname "$INSTALL_DIR")"
    BASENAME="$(basename "$INSTALL_DIR")"
    cd "$PARENT"
    rm -rf "$BASENAME"
    info "Removed $INSTALL_DIR"
else
    info "Keeping platform directory (--keep-dir)"
fi

# ─────────────────────────────────────────────
# 8. Optional: remove Docker Engine
# ─────────────────────────────────────────────
if [ "$REMOVE_DOCKER" -eq 1 ]; then
    step "Removing Docker Engine..."
    apt-get remove -y docker-ce docker-ce-cli containerd.io docker-compose-plugin 2>/dev/null || true
    rm -f /etc/apt/sources.list.d/docker.list /etc/apt/keyrings/docker.asc
    apt-get autoremove -y 2>/dev/null || true
    info "Docker removed"
fi

# ─────────────────────────────────────────────
# 9. Optional: remove Temurin JDK
# ─────────────────────────────────────────────
if [ "$REMOVE_JAVA" -eq 1 ]; then
    step "Removing Temurin JDK..."
    apt-get remove -y "temurin-*-jdk" 2>/dev/null || true
    rm -f /etc/apt/sources.list.d/adoptium.list /etc/apt/keyrings/adoptium.gpg
    apt-get autoremove -y 2>/dev/null || true
    info "Java removed"
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo -e "  ${GREEN}SECUR-EU Platform uninstalled successfully${NC}"
echo "═══════════════════════════════════════════════════════════"
echo ""
