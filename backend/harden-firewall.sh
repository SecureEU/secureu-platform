#!/bin/bash
# Host firewall hardening — DNV finding VULN-18127-04-001 (exposed backend services).
#
# Why this exists: several components run with `network_mode: host`
# (sqs-logstash, dtm-logstash, dtm-suricata, dtm-tshark) or `--network host`
# (kafka-dtm, zookeeper-dtm). Host networking BYPASSES docker port publishing,
# so their listeners (Logstash API 9600/9601, Kafka 9092, Zookeeper 2181) are
# reachable on every interface and cannot be limited with a "127.0.0.1:" bind.
# A default-deny host firewall is the only way to close them without
# re-architecting those stacks onto bridge networks.
#
# Default-deny inbound, with an explicit allowlist of the ports end users and
# SEUXDR agents genuinely need.
#
# Usage:
#   sudo ./harden-firewall.sh              # show what would change (dry run)
#   sudo ./harden-firewall.sh --apply      # apply the rules
#   sudo SSH_PORT=2222 ./harden-firewall.sh --apply
#
# Override the allowlist if your deployment differs:
#   sudo ALLOW_TCP="22 3000 8443 8081" ./harden-firewall.sh --apply

set -euo pipefail

SSH_PORT="${SSH_PORT:-22}"

# Ports that must stay reachable:
#   $SSH_PORT administration
#   3000      dashboard (Next.js) - the only user-facing service by design
#   8443      SEUXDR manager API  - agents + dashboard
#   8081      SEUXDR registration - agents
#   3001 8000 8001 8002 5000 5002 - module APIs called DIRECTLY by the browser
#             via NEXT_PUBLIC_*_API_URL. Drop these from the list once the
#             dashboard is migrated to the server-side /proxy/* rewrites that
#             already exist in frontend/next.config.mjs, which is the proper
#             long-term fix for this finding.
ALLOW_TCP="${ALLOW_TCP:-$SSH_PORT 3000 8443 8081 3001 8000 8001 8002 5000 5002}"

# Explicitly closed (documented for the report): 9200 OpenSearch, 27017 MongoDB,
# 8083 mongo-express, 8432 Postgres, 9092 Kafka, 2181 Zookeeper,
# 9600/9601 Logstash APIs, 4040 ngrok inspector, 55000 Wazuh API.

APPLY=0
[ "${1:-}" = "--apply" ] && APPLY=1

[ "$(id -u)" -eq 0 ] || { echo "ERROR: run as root: sudo $0 [--apply]"; exit 1; }
command -v ufw >/dev/null || { echo "ERROR: ufw not installed. Install with: apt-get install -y ufw"; exit 1; }

echo "=== SECUR-EU firewall hardening ==="
echo "Policy : deny inbound, allow outbound"
echo "Allow  : ${ALLOW_TCP// /, } (tcp)"
echo "Closed : everything else, including 9200 27017 8083 8432 9092 2181 9600 9601"
echo

if [ "$APPLY" -eq 0 ]; then
    echo "Dry run — no changes made. Re-run with --apply to enforce."
    echo "Current status:"; ufw status verbose 2>/dev/null | sed 's/^/  /' || true
    exit 0
fi

# Order matters: allow SSH BEFORE enabling, or an active session can be cut off.
ufw --force reset >/dev/null
ufw default deny incoming >/dev/null
ufw default allow outgoing >/dev/null
for p in $ALLOW_TCP; do
    ufw allow "${p}/tcp" >/dev/null
    echo "  allowed ${p}/tcp"
done
ufw --force enable >/dev/null

echo
echo "=== Active rules ==="
ufw status verbose | sed 's/^/  /'
echo
echo "Verify from another host that 9200/27017/9600 are refused, e.g.:"
echo "  nc -z -w3 <this-host> 9200 && echo STILL OPEN || echo closed"
