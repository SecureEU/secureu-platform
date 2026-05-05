#!/bin/bash
set -e

# Pentest Go server requires sudo — prompt early so it doesn't interrupt mid-script
sudo -n true 2>/dev/null || { echo "Error: passwordless sudo is required for the pentest backend."; exit 1; }

BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "=== SECUR-EU Backend Startup ==="
echo "Backend dir: $BACKEND_DIR"
echo ""

# ─────────────────────────────────────────────
# 1. Infrastructure (Kafka, Zookeeper, Postgres)
# ─────────────────────────────────────────────
echo "[1/10] Starting infrastructure..."

# Zookeeper (needed by Kafka)
if ! docker ps --format '{{.Names}}' | grep -q '^zookeeper-dtm$'; then
  docker start zookeeper-dtm 2>/dev/null || \
  docker run -d --name zookeeper-dtm \
    --restart unless-stopped \
    --network host \
    -e ZOOKEEPER_CLIENT_PORT=2181 \
    -e ZOOKEEPER_TICK_TIME=2000 \
    confluentinc/cp-zookeeper:7.5.5
  echo "  Zookeeper started"
else
  echo "  Zookeeper already running"
fi

sleep 3

# Kafka (needed by DTM pipeline and SQS logstash)
if ! docker ps --format '{{.Names}}' | grep -q '^kafka-dtm$'; then
  docker start kafka-dtm 2>/dev/null || \
  docker run -d --name kafka-dtm \
    --restart unless-stopped \
    --network host \
    -e KAFKA_BROKER_ID=1 \
    -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    confluentinc/cp-kafka:7.5.5
  echo "  Kafka started"
else
  echo "  Kafka already running"
fi

# Sphinx Postgres (needed by DTM & AD Java apps)
if ! docker ps --format '{{.Names}}' | grep -q '^sphinx-postgres$'; then
  docker start sphinx-postgres 2>/dev/null || \
  docker run -d --name sphinx-postgres \
    --restart unless-stopped \
    -p 8432:5432 \
    -e POSTGRES_USER=sphinx \
    -e POSTGRES_PASSWORD=sphinx \
    -e POSTGRES_DB=sphinx \
    postgres:15-alpine
  echo "  Sphinx Postgres started"
  # Wait for Postgres to accept connections, then create the sphinx schema.
  # On a fresh image pull this can take 5-15s, well past the old fixed sleep.
  for i in $(seq 1 30); do
    if docker exec sphinx-postgres pg_isready -U sphinx -d sphinx >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
  # The DTM/AD Liquibase migrations expect schema "sphinx" to exist. Retry the
  # CREATE SCHEMA a few times in case the user/db are still being initialized
  # right after pg_isready returns ready.
  for i in $(seq 1 10); do
    if docker exec sphinx-postgres psql -U sphinx -d sphinx -c "CREATE SCHEMA IF NOT EXISTS sphinx;" >/dev/null 2>&1; then
      echo "  Sphinx schema initialized"
      break
    fi
    sleep 2
    if [ "$i" = "10" ]; then
      echo "  WARN: failed to create sphinx schema after 10 attempts"
    fi
  done
else
  echo "  Sphinx Postgres already running"
fi

echo ""

# ─────────────────────────────────────────────
# 2. DTM Monitoring Pipeline (Suricata → Logstash → Kafka)
# ─────────────────────────────────────────────
echo "[2/10] Starting DTM monitoring pipeline..."
# Suricata's af-packet does not accept "any" — pick the host's default-route
# interface so it can sniff. Tshark accepts real interface names too, so the
# same env var works for both. Allow override via CAPTURE_INTERFACE.
if [ -z "${CAPTURE_INTERFACE:-}" ]; then
    CAPTURE_INTERFACE=$(ip -4 route show default 2>/dev/null | awk '{print $5; exit}')
    [ -z "$CAPTURE_INTERFACE" ] && CAPTURE_INTERFACE=$(ip -o link show | awk -F': ' '$2!~/lo|docker|br-/ {print $2; exit}')
fi
echo "  Capture interface: ${CAPTURE_INTERFACE:-<auto>}"
export CAPTURE_INTERFACE
docker compose -f "$BACKEND_DIR/dtmad/monitoring/docker-compose.yml" up -d
echo ""

# ─────────────────────────────────────────────
# 3. SQS Backend (OpenSearch + Logstash bridge + FastAPI)
# ─────────────────────────────────────────────
echo "[3/10] Starting SQS (Botnet Detection) backend..."
docker compose -f "$BACKEND_DIR/sqs/docker-compose.yml" up -d
echo ""

# ─────────────────────────────────────────────
# 4. Dark Web Backend
# ─────────────────────────────────────────────
echo "[4/10] Starting Dark Web backend..."
docker compose -f "$BACKEND_DIR/darkweb/docker-compose.yml" up -d
echo ""

# ─────────────────────────────────────────────
# 5. Pentest Backend (MongoDB + Postgres + Go server)
# ─────────────────────────────────────────────
echo "[5/10] Starting Pentest databases..."
docker compose -f "$BACKEND_DIR/pentest/docker_compose.yaml" up -d

echo "[5/10] Building/pulling pentest scan images..."
# Build nmap-bash image if not present
if ! docker image inspect nmap-bash:latest > /dev/null 2>&1; then
  echo "  Building nmap-bash image..."
  docker build -t nmap-bash "$BACKEND_DIR/pentest/nmap-docker"
else
  echo "  nmap-bash image already exists"
fi
# Pull ZAP and Metasploit images if not present
for img in zaproxy/zap-stable:latest metasploitframework/metasploit-framework:latest; do
  if ! docker image inspect "$img" > /dev/null 2>&1; then
    echo "  Pulling $img..."
    docker pull "$img"
  else
    echo "  $img already exists"
  fi
done

echo "[5/10] Starting Pentest Go server (requires sudo)..."
cd "$BACKEND_DIR/pentest"
sudo -E nohup ./bin/server > server_run.log 2>&1 &
PENTEST_PID=$!
cd "$BACKEND_DIR"
echo "  Pentest server PID: $PENTEST_PID"
echo ""

# ─────────────────────────────────────────────
# 6. SSL Checker Backend
# ─────────────────────────────────────────────
echo "[6/10] Starting SSL Checker backend..."
docker compose -f "$BACKEND_DIR/sslchecker/docker-compose.yml" up -d
echo ""

# ─────────────────────────────────────────────
# 7. VSP Backend (Vulnerability Score Prediction)
# ─────────────────────────────────────────────
echo "[7/10] Starting VSP backend..."
docker compose -f "$BACKEND_DIR/vsp/docker-compose.yml" up -d
echo ""

# ─────────────────────────────────────────────
# 8. Red Flags Backend (Log Anomaly Detection)
# ─────────────────────────────────────────────
echo "[8/10] Starting Red Flags backend..."
docker compose -f "$BACKEND_DIR/redflags/docker-compose.yml" up -d
echo ""

# ─────────────────────────────────────────────
# 9. SEUXDR (Host-Based Intrusion Detection)
# ─────────────────────────────────────────────
echo "[9/10] Starting SEUXDR backend..."
SEUXDR_DIR="$BACKEND_DIR/seuxdr"

# Auto-detect local IP
# Honor SERVER_IP override (set by install.sh for multi-NIC hosts where
# `hostname -I` would pick the NAT/internal address instead of the host-only
# adapter operators actually expose).
LOCAL_IP="${SERVER_IP:-$(hostname -I | awk '{print $1}')}"
echo "  Detected local IP: $LOCAL_IP"

# Replace IP in config files
sed -i "s|^domain:.*|domain: \"$LOCAL_IP\"|" "$SEUXDR_DIR/manager/manager.yaml"
# Replace non-0.0.0.0 IPs in IP_ADDRESSES arrays
sed -i "/IP_ADDRESSES/,/expiration_date/{s|\"[1-9][0-9]*\.[0-9]*\.[0-9]*\.[0-9]*\"|\"$LOCAL_IP\"|g}" "$SEUXDR_DIR/manager/manager.yaml"
sed -i "s|IP\.1 = .*|IP.1 = $LOCAL_IP|" "$SEUXDR_DIR/localhost.ext"
sed -i "s|VITE_ROOT_URI=.*|VITE_ROOT_URI=https://$LOCAL_IP:8443|" "$SEUXDR_DIR/manager_front/.env"

# First-run: generate certs if they don't exist
if [ ! -f "$SEUXDR_DIR/manager/certs/server.crt" ]; then
  echo "  First run — generating certificates..."
  cd "$SEUXDR_DIR"
  bash gen-certs.sh
  cd "$BACKEND_DIR"
  echo "  Certificates generated"
fi

docker compose -f "$SEUXDR_DIR/docker-compose.yml" up -d --build

# First-run: initialize Wazuh + Go server inside the container
if ! docker exec seuxdr-manager systemctl is-enabled seuxdr.service > /dev/null 2>&1; then
  echo "  First run — initializing SEUXDR (this takes several minutes)..."
  docker exec seuxdr-manager /usr/local/bin/startup.sh TEST
  echo "  SEUXDR initialization complete"
fi

# Verify SEUXDR manager is ready
echo "  Waiting for SEUXDR manager to start..."
for i in $(seq 1 60); do
  if curl -sk "https://$LOCAL_IP:8443/api/status" 2>/dev/null | grep -q '"status"'; then
    echo "  SEUXDR manager is ready"
    break
  fi
  sleep 3
done
echo ""

# ─────────────────────────────────────────────
# 10. DTM & AD Java Apps
# ─────────────────────────────────────────────
echo "[10/10] Starting DTM & AD Java applications..."

# Wait for Kafka and Postgres to be ready
sleep 5

# Data Traffic Monitoring (port 8087) — must start first (owns Liquibase migrations)
nohup "$BACKEND_DIR/dtmad/start-dtm.sh" > "$BACKEND_DIR/dtmad/dtm.log" 2>&1 &
echo "  DTM started (port 8087), PID: $!"

# Wait for DTM to finish Liquibase migrations before starting AD (they share the same DB schema)
echo "  Waiting for DTM to be ready..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:8087/sphinx/dtm/actuator/health > /dev/null 2>&1; then
    echo "  DTM is ready"
    break
  fi
  sleep 2
done

# Default 'local' DTM instance is now seeded by DefaultInstanceInitializer
# (CommandLineRunner) inside the DTM Spring Boot app on startup. No HTTP curl
# needed — the JSON POST path returns 415 Unsupported Media Type for reasons we
# haven't fully root-caused, so we sidestep it with a JPA insert at boot.

# Anomaly Detection (port 5001)
nohup "$BACKEND_DIR/dtmad/start-ad.sh" > "$BACKEND_DIR/dtmad/ad.log" 2>&1 &
echo "  AD started (port 5001), PID: $!"

echo ""
echo "=== All backends started ==="
echo ""
echo "Services:"
echo "  SSL Checker Backend             : http://localhost:5000"
echo "  VSP Backend (Score Prediction) : http://localhost:5002"
echo "  SQS Backend (Botnet Detection) : http://localhost:8000"
echo "  Dark Web Backend               : http://localhost:8001"
echo "  Red Flags Backend              : http://localhost:8002"
echo "  Pentest Backend                 : http://localhost:3001"
echo "  DTM Backend                     : http://localhost:8087/sphinx/dtm"
echo "  AD Backend                      : http://localhost:5001/sphinx/ad"
echo "  OpenSearch                      : http://localhost:9200"
echo "  Kafka                           : localhost:9092"
echo "  MongoDB (Pentest)               : localhost:27017"
echo "  Mongo Express                   : http://localhost:8081"
echo "  SEUXDR Manager API              : https://localhost:8443"
echo "  (SEUXDR Frontend is served by the Next.js dashboard)"
echo "  Sphinx Postgres                 : localhost:8432"
