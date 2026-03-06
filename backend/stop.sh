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

# Stop Docker compose services
echo "Stopping Docker services..."
docker compose -f "$BACKEND_DIR/pentest/docker_compose.yaml" down 2>/dev/null
docker compose -f "$BACKEND_DIR/sslchecker/docker-compose.yml" down 2>/dev/null
docker compose -f "$BACKEND_DIR/vsp/docker-compose.yml" down 2>/dev/null
docker compose -f "$BACKEND_DIR/darkweb/docker-compose.yml" down 2>/dev/null
docker compose -f "$BACKEND_DIR/redflags/docker-compose.yml" down 2>/dev/null
docker compose -f "$BACKEND_DIR/seuxdr/docker-compose.yml" down 2>/dev/null
docker compose -f "$BACKEND_DIR/sqs/docker-compose.yml" down 2>/dev/null
docker compose -f "$BACKEND_DIR/dtmad/monitoring/docker-compose.yml" down 2>/dev/null

# Stop infrastructure
echo "Stopping infrastructure..."
docker stop kafka-dtm zookeeper-dtm sphinx-postgres 2>/dev/null
docker rm kafka-dtm zookeeper-dtm sphinx-postgres 2>/dev/null

echo ""
echo "=== All backends stopped ==="
