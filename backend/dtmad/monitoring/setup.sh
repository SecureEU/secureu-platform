#!/bin/bash
# Setup and start DTM monitoring stack (Suricata + Logstash + Tshark)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== DTM Monitoring Setup ==="

# 1. Download ET Open rules if not present (or older than 24h)
RULES_DIR="suricata/rules"
RULES_MARKER="$RULES_DIR/.downloaded"
if [ ! -f "$RULES_MARKER" ] || [ "$(find "$RULES_MARKER" -mtime +1 2>/dev/null)" ]; then
    echo "[1/4] Downloading Emerging Threats Open rules..."
    curl -L --fail -o /tmp/emerging.rules.tar.gz \
        https://rules.emergingthreats.net/open/suricata/emerging.rules.tar.gz
    # Extract all .rules files into suricata/rules/
    tar xzf /tmp/emerging.rules.tar.gz -C "$RULES_DIR" --strip-components=1 --wildcards 'rules/*.rules'
    rm -f /tmp/emerging.rules.tar.gz
    touch "$RULES_MARKER"
    echo "  Downloaded $(ls "$RULES_DIR"/*.rules | wc -l) rule files"
else
    echo "[1/4] ET Open rules already present (< 24h old), skipping download"
fi

# 2. Build tshark image
echo "[2/4] Building tshark image..."
docker compose build tshark

# 3. Start all services
echo "[3/4] Starting containers..."
docker compose up -d

# 4. Status
echo "[4/4] Container status:"
docker compose ps

echo ""
echo "=== Done ==="
echo "Logs:    docker compose -f $SCRIPT_DIR/docker-compose.yml logs -f"
echo "Stop:    docker compose -f $SCRIPT_DIR/docker-compose.yml down"
echo ""
echo "Verify:"
echo "  docker compose logs suricata | tail -20"
echo "  docker compose logs logstash | tail -20"
echo "  docker compose logs tshark   | tail -20"
