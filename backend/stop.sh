#!/bin/bash

BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "=== SECUR-EU Backend Shutdown ==="

# Stop Java apps
echo "Stopping DTM & AD Java apps..."
pkill -f "data-traffic-monitoring-0.0.1-SNAPSHOT.jar" 2>/dev/null && echo "  DTM stopped" || echo "  DTM not running"
pkill -f "anomaly-detection-0.0.1-SNAPSHOT.jar" 2>/dev/null && echo "  AD stopped" || echo "  AD not running"

# Stop Pentest Go server
echo "Stopping Pentest server..."
sudo pkill -f "bin/server" 2>/dev/null && echo "  Pentest server stopped" || echo "  Pentest server not running"

# Stop Docker compose services.
# NOTE: `compose stop` instead of `compose down` so containers (and any state
# that lives inside the container's filesystem, like Wazuh in seuxdr-manager)
# survive a `systemctl restart secureu-backend`. With `down`, the container
# is removed, /var/ossec is wiped, and the next start triggers a ~10-minute
# Wazuh re-install (which is also flaky). To fully tear down the platform,
# use `docker compose down` directly or the documented uninstall path.
echo "Stopping Docker services..."
docker compose -f "$BACKEND_DIR/pentest/docker_compose.yaml" stop 2>/dev/null
docker compose -f "$BACKEND_DIR/sslchecker/docker-compose.yml" stop 2>/dev/null
docker compose -f "$BACKEND_DIR/vsp/docker-compose.yml" stop 2>/dev/null
docker compose -f "$BACKEND_DIR/darkweb/docker-compose.yml" stop 2>/dev/null
docker compose -f "$BACKEND_DIR/redflags/docker-compose.yml" stop 2>/dev/null
docker compose -f "$BACKEND_DIR/seuxdr/docker-compose.yml" stop 2>/dev/null
docker compose -f "$BACKEND_DIR/sqs/docker-compose.yml" stop 2>/dev/null
docker compose -f "$BACKEND_DIR/dtmad/monitoring/docker-compose.yml" stop 2>/dev/null

# Infrastructure containers started via `docker run` (not compose). Use
# `docker stop` only; their named volumes persist their data across restart.
# start.sh recreates the containers from the existing volumes on next boot.
echo "Stopping infrastructure..."
docker stop kafka-dtm zookeeper-dtm sphinx-postgres 2>/dev/null
docker rm kafka-dtm zookeeper-dtm sphinx-postgres 2>/dev/null

echo ""
echo "=== All backends stopped ==="
