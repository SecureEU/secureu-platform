#!/bin/bash
set -e

# ═══════════════════════════════════════════════════════════════
# SECUR-EU Platform — Local Installation Script
# ═══════════════════════════════════════════════════════════════
#
# Usage:
#   git clone https://github.com/SecureEU/secureu-platform.git /opt/secur-eu
#   cd /opt/secur-eu
#   sudo ./install.sh
#
# Target: Ubuntu 22.04 / 24.04 LTS or Debian 12+
# ═══════════════════════════════════════════════════════════════

INSTALL_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$INSTALL_DIR/frontend"
BACKEND_DIR="$INSTALL_DIR/backend"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ──────────────────────────────────────────────
# Pre-checks
# ──────────────────────────────────────────────
if [ "$EUID" -ne 0 ]; then
    error "Please run as root: sudo ./install.sh"
fi

if [ ! -f "$FRONTEND_DIR/package.json" ]; then
    error "Frontend not found at $FRONTEND_DIR. Make sure you cloned the full repo."
fi

if [ ! -f "$BACKEND_DIR/start.sh" ]; then
    error "Backend not found at $BACKEND_DIR. Make sure you cloned the full repo."
fi

# Detect distribution (Ubuntu or Debian)
if [ ! -f /etc/os-release ]; then
    error "/etc/os-release not found — cannot determine distribution."
fi
DISTRO=$(. /etc/os-release && echo "$ID")
case "$DISTRO" in
    ubuntu|debian)
        info "Detected distribution: $DISTRO"
        ;;
    *)
        error "Unsupported distribution: $DISTRO. Supported: ubuntu, debian."
        ;;
esac

# Detect the user who invoked sudo
REAL_USER="${SUDO_USER:-$USER}"
if [ "$REAL_USER" = "root" ]; then
    warn "No SUDO_USER detected. Files will be owned by root."
    warn "Consider running with: sudo ./install.sh"
fi

# Auto-chown when the install dir isn't owned by REAL_USER. The README's
# "git clone /opt/secureu-platform" pattern requires writable /opt, so most
# operators do `sudo git clone`, which leaves the tree root-owned. Without
# this fix, the later `sudo -u $REAL_USER mvn package` and `sudo -u $REAL_USER
# npm ci` calls fail with "could not create parent directories".
if [ "$REAL_USER" != "root" ]; then
    CURRENT_OWNER=$(stat -c '%U' "$INSTALL_DIR")
    if [ "$CURRENT_OWNER" != "$REAL_USER" ]; then
        warn "Install dir owned by $CURRENT_OWNER, changing to $REAL_USER to allow non-root build steps"
        chown -R "$REAL_USER:$REAL_USER" "$INSTALL_DIR"
    fi
fi

if [ -n "$SERVER_IP" ]; then
    info "Using SERVER_IP override: $SERVER_IP"
else
    SERVER_IP=$(hostname -I | awk '{print $1}')
    info "Detected server IP: $SERVER_IP"
    info "If end users will reach this host on a different IP, re-run with: sudo SERVER_IP=<ip> ./install.sh"
    info "On VirtualBox NAT setups, also set CAPTURE_INTERFACE=<host-only-iface> in the secureu-backend systemd unit so Suricata can sniff (NAT slirp doesn't expose raw packets)."
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  SECUR-EU Platform Installer"
echo "═══════════════════════════════════════════════════════════"
echo "  Install dir:  $INSTALL_DIR"
echo "  Frontend:     $FRONTEND_DIR"
echo "  Backend:      $BACKEND_DIR"
echo "  Server IP:    $SERVER_IP"
echo "  Run as user:  $REAL_USER"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Disable PackageKit if it is running — it holds the dpkg frontend lock and
# blocks our apt-get install commands on fresh Debian images.
if systemctl is-active --quiet packagekit 2>/dev/null; then
    info "Stopping packagekit to release the dpkg lock..."
    systemctl stop packagekit 2>/dev/null || true
    systemctl mask packagekit 2>/dev/null || true
fi

# On fresh cloud images, unattended-upgrades / apt-daily run at boot and hold
# the dpkg frontend lock. apt-get does not retry, so our first install step
# would fail. Stop and disable that machinery, then wait for the lock to clear.
info "Disabling apt auto-update services to free the dpkg lock..."
systemctl stop unattended-upgrades apt-daily.timer apt-daily-upgrade.timer \
    apt-daily.service apt-daily-upgrade.service 2>/dev/null || true
systemctl disable unattended-upgrades apt-daily.timer apt-daily-upgrade.timer 2>/dev/null || true
for _ in $(seq 1 60); do
    fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 || break
    sleep 2
done

# ──────────────────────────────────────────────
# 1. System packages
# ──────────────────────────────────────────────
info "[1/7] Installing system packages..."
apt-get update -qq
apt-get install -y -qq \
    apt-transport-https ca-certificates curl gnupg lsb-release \
    git openssl unzip wget net-tools acl \
    python3 python3-xmltodict
info "System packages installed"

# ──────────────────────────────────────────────
# 2. Docker
# ──────────────────────────────────────────────
info "[2/7] Installing Docker..."
if command -v docker &> /dev/null; then
    info "Docker already installed: $(docker --version)"
else
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL "https://download.docker.com/linux/$DISTRO/gpg" -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/$DISTRO $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
    systemctl enable docker
    systemctl start docker
    info "Docker installed"
fi

# Add user to docker group
if [ "$REAL_USER" != "root" ]; then
    usermod -aG docker "$REAL_USER"
    info "Added $REAL_USER to docker group"
fi

# ──────────────────────────────────────────────
# 3. Node.js 20
# ──────────────────────────────────────────────
info "[3/7] Installing Node.js 20..."
if command -v node &> /dev/null && node -v | grep -q "v2[0-9]"; then
    info "Node.js already installed: $(node -v)"
else
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt-get install -y -qq nodejs
    info "Node.js installed: $(node -v)"
fi

# Resolve the working npm binary. Hosts that have BOTH the apt 'npm' package
# (broken on Node 22 — "Cannot find module 'semver'") and a NodeSource-installed
# /usr/local/bin/npm will fail at frontend startup if we hardcode /usr/bin/npm
# in the systemd unit. Pick the one that actually runs.
NPM_BIN=""
for candidate in /usr/local/bin/npm /usr/bin/npm $(command -v npm 2>/dev/null); do
    if [ -x "$candidate" ] && "$candidate" --version &>/dev/null; then
        NPM_BIN="$candidate"
        break
    fi
done
if [ -z "$NPM_BIN" ]; then
    error "No working npm binary found. Install nodejs from NodeSource and retry."
fi
info "Using npm at: $NPM_BIN ($("$NPM_BIN" --version))"

# ──────────────────────────────────────────────
# 4. Java 17
# ──────────────────────────────────────────────
info "[4/7] Installing Java 17..."
# Detect a *working* JDK 17: javac must actually run and report version 17.
# Do NOT grep `java -version | grep 17` — when Java is absent, Ubuntu's
# command-not-found handler prints suggestion text mentioning
# "openjdk-17-jre-headless", which matches "17" and falsely reports Java as
# installed, so the real install gets skipped and the build later fails with
# "Cannot run program javac".
if command -v javac >/dev/null 2>&1 && javac -version 2>&1 | grep -q "javac 17"; then
    info "Java 17 already installed: $(javac -version 2>&1)"
else
    # Prefer the distro's openjdk-17-jdk — always in the Ubuntu/Debian repos,
    # no third-party key/repo to fetch, and sufficient to build the apps.
    if apt-get install -y -qq openjdk-17-jdk; then
        info "Installed openjdk-17-jdk"
    else
        # Fall back to Temurin if the distro package is unavailable.
        warn "openjdk-17-jdk unavailable; falling back to Temurin"
        if [ ! -f /etc/apt/keyrings/adoptium.gpg ]; then
            curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
            echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" > /etc/apt/sources.list.d/adoptium.list
            apt-get update -qq
        fi
        apt-get install -y -qq temurin-17-jdk
    fi
    # Pin javac (and java) to 17 so a co-installed JRE 21 doesn't shadow it.
    JDK17_HOME=$(ls -d /usr/lib/jvm/*temurin-17* /usr/lib/jvm/java-17-openjdk-* 2>/dev/null | head -1)
    if [ -n "$JDK17_HOME" ] && [ -x "$JDK17_HOME/bin/javac" ]; then
        update-alternatives --install /usr/bin/javac javac "$JDK17_HOME/bin/javac" 2000
        update-alternatives --install /usr/bin/java  java  "$JDK17_HOME/bin/java"  2000
        update-alternatives --set javac "$JDK17_HOME/bin/javac"
        update-alternatives --set java  "$JDK17_HOME/bin/java"
    fi
    info "Java installed: $(javac -version 2>&1)"
fi

# Maven (needed to build the DTM and AD Spring Boot apps)
if ! command -v mvn &> /dev/null; then
    apt-get install -y -qq maven
    info "Maven installed: $(mvn -version 2>&1 | head -1)"
else
    info "Maven already installed: $(mvn -version 2>&1 | head -1)"
fi

# Build the DTM and AD Java apps so their JARs exist for start.sh.
# First-time builds download the full Maven dependency graph (~5 minutes).
DTMAD_DIR="$BACKEND_DIR/dtmad"
DTM_JAR="$DTMAD_DIR/data-traffic-monitoring/target/data-traffic-monitoring-0.0.1-SNAPSHOT.jar"
AD_JAR="$DTMAD_DIR/anomaly-detection/target/anomaly-detection-0.0.1-SNAPSHOT.jar"
if [ ! -f "$DTM_JAR" ] || [ ! -f "$AD_JAR" ]; then
    info "Building DTM and AD Spring Boot apps (this can take a while on first run)..."
    # Explicitly point JAVA_HOME at a JDK so the forked Maven compiler finds
    # javac. update-alternatives alone is unreliable: on some hosts Temurin
    # never registers a javac alternative, JAVA_HOME stays empty, and the
    # build dies with 'Cannot run program "javac"'. Try, in order:
    # update-alternatives → the Temurin install dir → wherever javac resolves.
    JDK_HOME=$(update-alternatives --list javac 2>/dev/null | grep temurin | head -1 | sed 's|/bin/javac||')
    if [ -z "$JDK_HOME" ]; then
        JDK_HOME=$(ls -d /usr/lib/jvm/temurin-17-jdk-* 2>/dev/null | head -1)
    fi
    if [ -z "$JDK_HOME" ] && command -v javac &>/dev/null; then
        JDK_HOME=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
    fi
    if [ -z "$JDK_HOME" ] || [ ! -x "$JDK_HOME/bin/javac" ]; then
        error "Could not locate a JDK with javac (looked in update-alternatives, /usr/lib/jvm/temurin-17-jdk-*, PATH). Install temurin-17-jdk and retry."
    fi
    export JAVA_HOME="$JDK_HOME"
    info "Using JAVA_HOME: $JAVA_HOME"
    sudo -u "$REAL_USER" bash -c "cd '$DTMAD_DIR' && JAVA_HOME='$JAVA_HOME' PATH='$JAVA_HOME/bin':\$PATH mvn -B -q -DskipTests package"
    if [ ! -f "$DTM_JAR" ] || [ ! -f "$AD_JAR" ]; then
        error "DTM/AD JARs missing after Maven build — see output above."
    fi
    info "DTM and AD JARs built"
else
    info "DTM and AD JARs already present"
fi

# ──────────────────────────────────────────────
# 5. Configure frontend
# ──────────────────────────────────────────────
info "[5/7] Configuring frontend..."

cat > "$FRONTEND_DIR/.env" <<EOF
# MongoDB
MONGODB_URI=mongodb://admin:password@localhost:27017/intersoc-dashboard?authSource=admin
MONGODB_DB=intersoc-dashboard

# JWT
JWT_SECRET=$(openssl rand -hex 32)

# Backend API URLs
NEXT_PUBLIC_DARKWEB_API_URL=http://${SERVER_IP}:8001
NEXT_PUBLIC_REDFLAGS_API_URL=http://${SERVER_IP}:8002
NEXT_PUBLIC_SSL_API_URL=http://${SERVER_IP}:5000
NEXT_PUBLIC_PENTEST_API_URL=http://${SERVER_IP}:3001
NEXT_PUBLIC_VSP_API_URL=http://${SERVER_IP}:5002
NEXT_PUBLIC_SEUXDR_API_URL=https://${SERVER_IP}:8443
NEXT_PUBLIC_SQS_API_URL=http://${SERVER_IP}:8000
# DTM and AD are proxied server-side by Next.js rewrites in next.config.mjs
# (browser hits :3000 → Next.js fetches localhost:8087/5001 → returns to browser).
# Server-side vars only — no NEXT_PUBLIC_ prefix. Localhost is correct here:
# the Next.js process runs on the same host as the Spring Boot apps. This means
# ports 8087 and 5001 do NOT need to be opened in the host firewall.
DTM_API_URL=http://localhost:8087
AD_API_URL=http://localhost:5001
EOF

info "Frontend .env created with server IP: $SERVER_IP"

cd "$FRONTEND_DIR"
info "Installing frontend dependencies (npm ci)..."
# pipefail on (already from set -e wouldn't catch a failing left side of a pipe)
set -o pipefail
sudo -u "$REAL_USER" "$NPM_BIN" ci --silent 2>&1 | tail -1
info "Building frontend (npm run build)..."
sudo -u "$REAL_USER" "$NPM_BIN" run build 2>&1 | tail -10
# Verify the production build actually produced a BUILD_ID; otherwise next start fails.
if [ ! -f "$FRONTEND_DIR/.next/BUILD_ID" ]; then
    error "Frontend build did not produce $FRONTEND_DIR/.next/BUILD_ID — see output above."
fi
info "Frontend built"

# ──────────────────────────────────────────────
# 6. Verify pentest binary
# ──────────────────────────────────────────────
info "[6/7] Checking pre-built binaries..."
if [ -f "$BACKEND_DIR/pentest/bin/server" ]; then
    chmod +x "$BACKEND_DIR/pentest/bin/server"
    info "Pentest server binary found"
else
    warn "Pentest binary not found at $BACKEND_DIR/pentest/bin/server"
    warn "Pentest scans will not work. Rebuild with: cd backend/pentest && go build -o bin/server ."
fi

# Pin RPATH in the pentest .env to this checkout (the committed value is
# developer-specific and breaks nmap result import / ZAP volume mounting).
PENTEST_ENV="$BACKEND_DIR/pentest/.env"
if [ -f "$PENTEST_ENV" ]; then
    if grep -q '^RPATH=' "$PENTEST_ENV"; then
        sed -i "s|^RPATH=.*|RPATH=\"$BACKEND_DIR/pentest\"|" "$PENTEST_ENV"
    else
        echo "RPATH=\"$BACKEND_DIR/pentest\"" >> "$PENTEST_ENV"
    fi
    info "Pentest .env RPATH set to $BACKEND_DIR/pentest"
else
    warn "Pentest .env not found at $PENTEST_ENV; nmap result import may not work"
fi

# ──────────────────────────────────────────────
# 7. Make scripts executable + set ownership
# ──────────────────────────────────────────────
info "[7/7] Setting permissions..."
chmod +x "$BACKEND_DIR/start.sh" "$BACKEND_DIR/stop.sh" "$BACKEND_DIR/cleanup-logs.sh"

# Passwordless sudo for the user (needed for pentest server)
if [ "$REAL_USER" != "root" ]; then
    echo "$REAL_USER ALL=(ALL) NOPASSWD: ALL" > "/etc/sudoers.d/$REAL_USER"
    chmod 0440 "/etc/sudoers.d/$REAL_USER"
    chown -R "$REAL_USER:$REAL_USER" "$INSTALL_DIR"
    info "Granted passwordless sudo to $REAL_USER"
fi

# ──────────────────────────────────────────────
# Create systemd services
# ──────────────────────────────────────────────
info "Creating systemd services..."

cat > /etc/systemd/system/secureu-backend.service <<EOF
[Unit]
Description=SECUR-EU Backend Services
After=network.target docker.service
Wants=docker.service

[Service]
Type=oneshot
RemainAfterExit=true
User=$REAL_USER
WorkingDirectory=$BACKEND_DIR
ExecStart=$BACKEND_DIR/start.sh
ExecStop=$BACKEND_DIR/stop.sh
Environment=PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
# RPATH is read by the pentest Go server to locate scan results and parser scripts.
# Setting it here avoids relying on godotenv finding pentest/.env relative to a
# moving cwd.
Environment=RPATH=$BACKEND_DIR/pentest
# SERVER_IP is honored by start.sh when patching SEUXDR's manager.yaml so the
# manager domain / cert SANs use the operator-chosen address rather than the
# first interface from `hostname -I` (which is often the NAT IP).
Environment=SERVER_IP=$SERVER_IP
# First-run pulls ~3GB of images and runs Wazuh initialization, which can
# take 30-50 minutes on a fresh host with limited bandwidth. 60 min gives
# generous headroom; subsequent starts are fast.
TimeoutStartSec=3600
# stop.sh tears down many docker-compose stacks; default 90s is too tight.
TimeoutStopSec=300

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/secureu-frontend.service <<EOF
[Unit]
Description=SECUR-EU Dashboard (Next.js)
After=network.target secureu-backend.service
Wants=secureu-backend.service

[Service]
Type=simple
User=$REAL_USER
WorkingDirectory=$FRONTEND_DIR
ExecStart=$NPM_BIN start -- -p 3000
Restart=on-failure
RestartSec=10
Environment=NODE_ENV=production
Environment=PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/secureu-cleanup.service <<EOF
[Unit]
Description=SECUR-EU Log Cleanup

[Service]
Type=oneshot
User=root
ExecStart=$BACKEND_DIR/cleanup-logs.sh
Environment=PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
EOF

cat > /etc/systemd/system/secureu-cleanup.timer <<EOF
[Unit]
Description=SECUR-EU Log Cleanup (disk threshold monitor)

[Timer]
OnCalendar=*:0/15
Persistent=true

[Install]
WantedBy=timers.target
EOF

chmod +x "$BACKEND_DIR/cleanup-logs.sh"

systemctl daemon-reload
systemctl enable secureu-backend secureu-frontend secureu-cleanup.timer
systemctl start secureu-cleanup.timer

echo ""
echo "═══════════════════════════════════════════════════════════"
echo -e "  ${GREEN}SECUR-EU Platform Installed Successfully${NC}"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "  Start the platform:"
echo "    sudo systemctl start secureu-backend"
echo "    sudo systemctl start secureu-frontend"
echo ""
echo "  Or start manually:"
echo "    cd $BACKEND_DIR && ./start.sh"
echo "    cd $FRONTEND_DIR && npm start -- -p 3000"
echo ""
echo "  Dashboard:  http://${SERVER_IP}:3000"
echo "  SEUXDR API: https://${SERVER_IP}:8443"
echo ""
echo "  Manage services:"
echo "    sudo systemctl start|stop|status secureu-backend"
echo "    sudo systemctl start|stop|status secureu-frontend"
echo ""
echo "  Logs:"
echo "    journalctl -u secureu-backend -f"
echo "    journalctl -u secureu-frontend -f"
echo ""
echo "  NOTE: First backend start takes ~10 minutes (Wazuh install)"
echo "═══════════════════════════════════════════════════════════"
