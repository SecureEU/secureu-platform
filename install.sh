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

SERVER_IP=$(hostname -I | awk '{print $1}')
info "Detected server IP: $SERVER_IP"

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

# ──────────────────────────────────────────────
# 1. System packages
# ──────────────────────────────────────────────
info "[1/7] Installing system packages..."
apt-get update -qq
apt-get install -y -qq \
    apt-transport-https ca-certificates curl gnupg lsb-release \
    git openssl unzip wget net-tools software-properties-common acl
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

# ──────────────────────────────────────────────
# 4. Java 17
# ──────────────────────────────────────────────
info "[4/7] Installing Java 17..."
if java -version 2>&1 | grep -q "17"; then
    info "Java 17 already installed"
else
    if [ ! -f /etc/apt/keyrings/adoptium.gpg ]; then
        curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
        echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" > /etc/apt/sources.list.d/adoptium.list
        apt-get update -qq
    fi
    apt-get install -y -qq temurin-17-jdk
    info "Java installed: $(java -version 2>&1 | head -1)"
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
EOF

info "Frontend .env created with server IP: $SERVER_IP"

cd "$FRONTEND_DIR"
info "Installing frontend dependencies (npm ci)..."
sudo -u "$REAL_USER" npm ci --silent 2>&1 | tail -1
info "Building frontend (npm run build)..."
sudo -u "$REAL_USER" npm run build 2>&1 | tail -3
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

# ──────────────────────────────────────────────
# 7. Make scripts executable + set ownership
# ──────────────────────────────────────────────
info "[7/7] Setting permissions..."
chmod +x "$BACKEND_DIR/start.sh" "$BACKEND_DIR/stop.sh"

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
TimeoutStartSec=600

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
ExecStart=/usr/bin/npm start -- -p 3000
Restart=on-failure
RestartSec=10
Environment=NODE_ENV=production
Environment=PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable secureu-backend secureu-frontend

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
