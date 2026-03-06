#!/bin/bash
set -e

# =============================================================================
# SEUXDR Docker Setup Script
# Automates the deployment of SEUXDR (Host-Based Intrusion Detection System)
# =============================================================================

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# =============================================================================
# SECTION 1: Color definitions & helper functions
# =============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${CYAN}=============================================${NC}"
    echo -e "${CYAN}       SEUXDR Docker Setup Script           ${NC}"
    echo -e "${CYAN}=============================================${NC}"
    echo ""
    echo -e "This script will help you set up SEUXDR with Docker."
    echo -e "It will configure certificates, ports, and start the containers."
    echo ""
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

# Spinner for long operations
spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='|/-\'
    while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
        local temp=${spinstr#?}
        printf " [%c]  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b\b"
    done
    printf "      \b\b\b\b\b\b"
}

# Cross-platform sed -i (macOS vs Linux compatibility)
sed_inplace() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

# =============================================================================
# SECTION 2: Prerequisites check
# =============================================================================

check_prerequisites() {
    print_step "Checking prerequisites..."

    local has_error=0

    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        has_error=1
    else
        print_success "Docker is installed"
    fi

    # Check Docker Compose (both forms)
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
        print_success "Docker Compose (plugin) is available"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
        print_success "Docker Compose (standalone) is available"
    else
        print_error "Docker Compose is not installed. Please install Docker Compose."
        has_error=1
    fi

    # Check OpenSSL
    if ! command -v openssl &> /dev/null; then
        print_error "OpenSSL is not installed. Please install OpenSSL."
        has_error=1
    else
        print_success "OpenSSL is installed"
    fi

    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running. Please start Docker."
        has_error=1
    else
        print_success "Docker daemon is running"
    fi

    # Check for existing SEUXDR containers
    if docker ps -a --format '{{.Names}}' | grep -q "seuxdr-manager\|frontend"; then
        print_warning "Existing SEUXDR containers detected. They will be stopped and rebuilt."
    fi

    if [ $has_error -eq 1 ]; then
        echo ""
        print_error "Prerequisites check failed. Please fix the issues above and try again."
        exit 1
    fi

    echo ""
    print_success "All prerequisites met!"
    echo ""
}

# =============================================================================
# SECTION 3: IP/Network detection
# =============================================================================

detect_ip() {
    local ip=""

    # Method 1: hostname -I (Linux)
    if command -v hostname &> /dev/null; then
        ip=$(hostname -I 2>/dev/null | awk '{print $1}')
    fi

    # Method 2: ip route (Linux fallback)
    if [ -z "$ip" ] && command -v ip &> /dev/null; then
        ip=$(ip route get 1 2>/dev/null | awk '{print $(NF-2);exit}')
    fi

    # Method 3: ifconfig (macOS/BSD fallback)
    if [ -z "$ip" ] && command -v ifconfig &> /dev/null; then
        ip=$(ifconfig 2>/dev/null | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | head -n1)
    fi

    echo "$ip"
}

check_port_available() {
    local port=$1

    # Validate port range
    if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
        return 1
    fi

    # Check if port is in use using ss or netstat
    if command -v ss &> /dev/null; then
        if ss -tuln | grep -q ":$port "; then
            return 1
        fi
    elif command -v netstat &> /dev/null; then
        if netstat -tuln | grep -q ":$port "; then
            return 1
        fi
    fi

    return 0
}

validate_port() {
    local port=$1
    local name=$2

    # Check if it's a number
    if ! [[ "$port" =~ ^[0-9]+$ ]]; then
        print_error "Invalid port: $port is not a number"
        return 1
    fi

    # Check range
    if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
        print_error "Invalid port: $port must be between 1 and 65535"
        return 1
    fi

    # Check availability
    if ! check_port_available "$port"; then
        print_warning "Port $port appears to be in use"
    fi

    return 0
}

# =============================================================================
# SECTION 4: User input collection
# =============================================================================

collect_user_input() {
    print_step "Collecting configuration..."
    echo ""

    # Detect and confirm IP
    DETECTED_IP=$(detect_ip)
    if [ -n "$DETECTED_IP" ]; then
        echo -e "Detected IP address: ${GREEN}$DETECTED_IP${NC}"
        read -p "Use this IP? [Y/n] or enter a different IP: " ip_input
        if [ -z "$ip_input" ] || [ "$ip_input" = "Y" ] || [ "$ip_input" = "y" ]; then
            SERVER_IP="$DETECTED_IP"
        else
            SERVER_IP="$ip_input"
        fi
    else
        read -p "Enter your server IP address: " SERVER_IP
    fi
    echo ""

    # Optional domain name
    read -p "Enter domain name (leave empty if using IP only): " DOMAIN_NAME
    if [ -n "$DOMAIN_NAME" ]; then
        echo -e "Using domain: ${GREEN}$DOMAIN_NAME${NC}"
    fi
    echo ""

    # Container hostname
    DEFAULT_HOSTNAME=$(hostname)
    read -p "Enter container hostname [$DEFAULT_HOSTNAME]: " CONTAINER_HOSTNAME
    CONTAINER_HOSTNAME=${CONTAINER_HOSTNAME:-$DEFAULT_HOSTNAME}
    echo ""

    # Port configuration
    echo "Port Configuration:"
    echo "-------------------"

    # Frontend port
    read -p "Frontend port [8080]: " PORT_FRONTEND
    PORT_FRONTEND=${PORT_FRONTEND:-8080}
    while ! validate_port "$PORT_FRONTEND" "Frontend"; do
        read -p "Enter a valid frontend port: " PORT_FRONTEND
    done

    # TLS API port
    read -p "Manager TLS API port [8443]: " PORT_TLS
    PORT_TLS=${PORT_TLS:-8443}
    while ! validate_port "$PORT_TLS" "TLS API"; do
        read -p "Enter a valid TLS API port: " PORT_TLS
    done

    # mTLS port
    read -p "Manager mTLS port [8081]: " PORT_MTLS
    PORT_MTLS=${PORT_MTLS:-8081}
    while ! validate_port "$PORT_MTLS" "mTLS"; do
        read -p "Enter a valid mTLS port: " PORT_MTLS
    done
    echo ""

    # Certificate mode
    echo "Certificate Mode:"
    echo "-----------------"
    echo "1) Self-signed certificates (recommended for development/testing)"
    echo "2) Custom certificates (for production with real CA)"
    read -p "Select certificate mode [1]: " CERT_MODE
    CERT_MODE=${CERT_MODE:-1}

    if [ "$CERT_MODE" = "2" ]; then
        USE_CUSTOM_CERTS="y"
        echo ""
        echo "Please provide paths to your certificates:"

        # Manager TLS cert
        read -p "Manager TLS certificate path (.crt/.pem): " CUSTOM_MANAGER_CERT
        while [ ! -f "$CUSTOM_MANAGER_CERT" ]; do
            print_error "File not found: $CUSTOM_MANAGER_CERT"
            read -p "Manager TLS certificate path: " CUSTOM_MANAGER_CERT
        done

        # Manager TLS key
        read -p "Manager TLS private key path (.key/.pem): " CUSTOM_MANAGER_KEY
        while [ ! -f "$CUSTOM_MANAGER_KEY" ]; do
            print_error "File not found: $CUSTOM_MANAGER_KEY"
            read -p "Manager TLS private key path: " CUSTOM_MANAGER_KEY
        done

        # Frontend TLS cert
        read -p "Frontend TLS certificate path (.crt/.pem): " CUSTOM_FRONTEND_CERT
        while [ ! -f "$CUSTOM_FRONTEND_CERT" ]; do
            print_error "File not found: $CUSTOM_FRONTEND_CERT"
            read -p "Frontend TLS certificate path: " CUSTOM_FRONTEND_CERT
        done

        # Frontend TLS key
        read -p "Frontend TLS private key path (.key/.pem): " CUSTOM_FRONTEND_KEY
        while [ ! -f "$CUSTOM_FRONTEND_KEY" ]; do
            print_error "File not found: $CUSTOM_FRONTEND_KEY"
            read -p "Frontend TLS private key path: " CUSTOM_FRONTEND_KEY
        done

        # Optional CA cert
        read -p "CA certificate path (optional, press Enter to skip): " CUSTOM_CA_CERT
        if [ -n "$CUSTOM_CA_CERT" ] && [ ! -f "$CUSTOM_CA_CERT" ]; then
            print_warning "CA certificate not found, will be skipped"
            CUSTOM_CA_CERT=""
        fi
    else
        USE_CUSTOM_CERTS="n"
    fi
    echo ""

    # Determine the domain/IP to use for configuration
    if [ -n "$DOMAIN_NAME" ]; then
        CONFIG_DOMAIN="$DOMAIN_NAME"
    else
        CONFIG_DOMAIN="$SERVER_IP"
    fi

    # Display summary
    echo ""
    echo -e "${CYAN}=============================================${NC}"
    echo -e "${CYAN}        Configuration Summary               ${NC}"
    echo -e "${CYAN}=============================================${NC}"
    echo ""
    echo -e "Server IP:          ${GREEN}$SERVER_IP${NC}"
    if [ -n "$DOMAIN_NAME" ]; then
        echo -e "Domain Name:        ${GREEN}$DOMAIN_NAME${NC}"
    fi
    echo -e "Container Hostname: ${GREEN}$CONTAINER_HOSTNAME${NC}"
    echo ""
    echo -e "Frontend Port:      ${GREEN}$PORT_FRONTEND${NC}"
    echo -e "Manager TLS Port:   ${GREEN}$PORT_TLS${NC}"
    echo -e "Manager mTLS Port:  ${GREEN}$PORT_MTLS${NC}"
    echo ""
    if [ "$USE_CUSTOM_CERTS" = "y" ]; then
        echo -e "Certificate Mode:   ${GREEN}Custom certificates${NC}"
    else
        echo -e "Certificate Mode:   ${GREEN}Self-signed certificates${NC}"
    fi
    echo ""

    read -p "Proceed with this configuration? [Y/n]: " confirm
    if [ "$confirm" = "n" ] || [ "$confirm" = "N" ]; then
        echo "Setup cancelled."
        exit 0
    fi
    echo ""
}

# =============================================================================
# SECTION 5: Configuration backup
# =============================================================================

backup_configs() {
    print_step "Backing up existing configuration files..."

    BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"

    # Backup existing config files
    [ -f "localhost.ext" ] && cp "localhost.ext" "$BACKUP_DIR/"
    [ -f "manager/manager.yaml" ] && cp "manager/manager.yaml" "$BACKUP_DIR/"
    [ -f "manager_front/.env" ] && cp "manager_front/.env" "$BACKUP_DIR/"
    [ -f "docker-compose.yml" ] && cp "docker-compose.yml" "$BACKUP_DIR/"
    [ -f "Dockerfile" ] && cp "Dockerfile" "$BACKUP_DIR/"

    print_success "Configuration backed up to: $BACKUP_DIR"
    echo ""
}

# =============================================================================
# SECTION 6: Configuration updates
# =============================================================================

update_localhost_ext() {
    print_step "Updating localhost.ext..."

    # Build the new content
    local content="authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost"

    local dns_count=1

    # Add domain if provided
    if [ -n "$DOMAIN_NAME" ]; then
        dns_count=$((dns_count + 1))
        content="$content
DNS.$dns_count = $DOMAIN_NAME"
    fi

    # Add IP
    content="$content
IP.1 = $SERVER_IP"

    echo "$content" > localhost.ext

    print_success "Updated localhost.ext"
}

update_manager_yaml() {
    print_step "Updating manager/manager.yaml..."

    # Use sed to update the manager.yaml file
    # Update tls_server and mtls_server to 0.0.0.0 (bind to all interfaces inside container)
    sed_inplace "s|^tls_server:.*|tls_server: \"0.0.0.0\"|" manager/manager.yaml
    sed_inplace "s|^mtls_server:.*|mtls_server: \"0.0.0.0\"|" manager/manager.yaml

    # Update domain (external address for agents to connect)
    sed_inplace "s|^domain:.*|domain: \"$CONFIG_DOMAIN\"|" manager/manager.yaml

    # Update tls_port
    sed_inplace "s|^tls_port:.*|tls_port: $PORT_TLS|" manager/manager.yaml

    # Update mtls_port
    sed_inplace "s|^mtls_port:.*|mtls_port: $PORT_MTLS|" manager/manager.yaml

    # Update frontend_port
    sed_inplace "s|^frontend_port:.*|frontend_port: $PORT_FRONTEND|" manager/manager.yaml

    # Update use_system_ca based on certificate mode
    if [ "$USE_CUSTOM_CERTS" = "y" ]; then
        sed_inplace "s|^use_system_ca:.*|use_system_ca: true|" manager/manager.yaml
    else
        sed_inplace "s|^use_system_ca:.*|use_system_ca: false|" manager/manager.yaml
    fi

    # Update IP_ADDRESSES in ca_settings (line 57-59)
    # This is more complex due to YAML array format, we'll use a Python/awk approach
    local temp_file=$(mktemp)
    awk -v ip="$SERVER_IP" '
    BEGIN { in_ca_settings = 0; in_ip_addresses = 0; }
    /ca_settings:/ { in_ca_settings = 1 }
    /server_settings:/ { in_ca_settings = 0 }
    in_ca_settings && /IP_ADDRESSES:/ {
        print
        in_ip_addresses = 1
        next
    }
    in_ip_addresses && /^[[:space:]]*-/ {
        next
    }
    in_ip_addresses && !/^[[:space:]]*-/ {
        printf "        - \"0.0.0.0\"\n"
        printf "        - \"%s\"\n", ip
        in_ip_addresses = 0
    }
    { print }
    ' manager/manager.yaml > "$temp_file"
    mv "$temp_file" manager/manager.yaml

    # Update IP_ADDRESSES in server_settings
    temp_file=$(mktemp)
    awk -v ip="$SERVER_IP" '
    BEGIN { in_server_settings = 0; in_ip_addresses = 0; }
    /server_settings:/ { in_server_settings = 1 }
    /client_settings:/ { in_server_settings = 0 }
    in_server_settings && /IP_ADDRESSES:/ {
        print
        in_ip_addresses = 1
        next
    }
    in_ip_addresses && /^[[:space:]]*-/ {
        next
    }
    in_ip_addresses && !/^[[:space:]]*-/ {
        printf "        - \"0.0.0.0\"\n"
        printf "        - \"%s\"\n", ip
        in_ip_addresses = 0
    }
    { print }
    ' manager/manager.yaml > "$temp_file"
    mv "$temp_file" manager/manager.yaml

    # Update IP_ADDRESSES in client_settings
    temp_file=$(mktemp)
    awk -v ip="$SERVER_IP" '
    BEGIN { in_client_settings = 0; in_ip_addresses = 0; }
    /client_settings:/ { in_client_settings = 1 }
    /tls:/ { in_client_settings = 0 }
    in_client_settings && /IP_ADDRESSES:/ {
        print
        in_ip_addresses = 1
        next
    }
    in_ip_addresses && /^[[:space:]]*-/ {
        next
    }
    in_ip_addresses && !/^[[:space:]]*-/ {
        printf "        - \"0.0.0.0\"\n"
        printf "        - \"%s\"\n", ip
        in_ip_addresses = 0
    }
    { print }
    ' manager/manager.yaml > "$temp_file"
    mv "$temp_file" manager/manager.yaml

    # Add domain to DNSNAMES if provided
    if [ -n "$DOMAIN_NAME" ]; then
        # We need to add the domain to each DNSNAMES section
        # This is a simplified approach - adds to the first occurrence
        if ! grep -q "$DOMAIN_NAME" manager/manager.yaml; then
            sed_inplace "/DNSNAMES:/a\\
        - \"$DOMAIN_NAME\"" manager/manager.yaml
        fi
    fi

    print_success "Updated manager/manager.yaml"
}

update_frontend_env() {
    print_step "Updating manager_front/.env..."

    # Update VITE_ROOT_URI
    local new_uri="https://$CONFIG_DOMAIN:$PORT_TLS"

    cat > manager_front/.env << EOF
VITE_ROOT_URI=$new_uri
VITE_HTTPS_KEY=certs/frontend.key
VITE_HTTPS_CERT=certs/frontend.crt
VITE_USE_TLS=false
EOF

    print_success "Updated manager_front/.env"
}

update_docker_compose() {
    print_step "Updating docker-compose.yml..."

    # Update hostname
    sed_inplace "s|hostname:.*|hostname: $CONTAINER_HOSTNAME|" docker-compose.yml

    # Update ports for manager service
    # The ports are in format "host:container"
    sed_inplace "s|\"[0-9]*:8443\"|\"$PORT_TLS:8443\"|" docker-compose.yml
    sed_inplace "s|\"[0-9]*:8081\"|\"$PORT_MTLS:8081\"|" docker-compose.yml

    # Update ports for frontend service
    sed_inplace "s|\"[0-9]*:8080\"|\"$PORT_FRONTEND:8080\"|" docker-compose.yml

    print_success "Updated docker-compose.yml"
}

update_dockerfile() {
    print_step "Updating Dockerfile..."

    # Update hostname in Dockerfile
    sed_inplace "s|RUN echo \".*\" > /etc/hostname|RUN echo \"$CONTAINER_HOSTNAME\" > /etc/hostname|" Dockerfile

    print_success "Updated Dockerfile"
}

# =============================================================================
# SECTION 7: Certificate handling
# =============================================================================

generate_certificates() {
    print_step "Setting up certificates..."

    # Create cert directories
    mkdir -p manager/certs
    mkdir -p manager_front/certs
    mkdir -p agent/certs

    if [ "$USE_CUSTOM_CERTS" = "y" ]; then
        # Copy custom certificates
        print_info "Copying custom certificates..."

        # Manager certs
        cp "$CUSTOM_MANAGER_CERT" manager/certs/server.crt
        cp "$CUSTOM_MANAGER_KEY" manager/certs/server.key

        # Frontend certs
        cp "$CUSTOM_FRONTEND_CERT" manager_front/certs/frontend.crt
        cp "$CUSTOM_FRONTEND_KEY" manager_front/certs/frontend.key

        # CA cert if provided
        if [ -n "$CUSTOM_CA_CERT" ]; then
            cp "$CUSTOM_CA_CERT" manager/certs/server-ca.crt
            cp "$CUSTOM_CA_CERT" agent/certs/server-ca.crt
        fi

        print_success "Custom certificates copied"
    else
        # Use self-signed certificates
        print_info "Generating self-signed certificates..."

        # Remove existing certs to regenerate with new IP/domain
        rm -f manager/certs/server.key manager/certs/server.crt manager/certs/server.req
        rm -f manager/certs/server-ca.key manager/certs/server-ca.crt
        rm -f manager_front/certs/frontend.key manager_front/certs/frontend.crt manager_front/certs/frontend.req
        rm -f agent/certs/server-ca.crt

        # Run gen-certs.sh
        if [ -f "gen-certs.sh" ]; then
            bash gen-certs.sh
        else
            print_error "gen-certs.sh not found!"
            exit 1
        fi

        print_success "Self-signed certificates generated"
    fi

    # Always generate JWT and encryption keys if missing
    if [ ! -f manager/certs/encryption_key.pem ]; then
        print_info "Generating encryption keys..."
        openssl genrsa -out manager/certs/encryption_key.pem 2048
        openssl rsa -in manager/certs/encryption_key.pem -pubout -out manager/certs/encryption_pubkey.pem
    fi

    if [ ! -f manager/certs/jwt_private.key ]; then
        print_info "Generating JWT keys..."
        openssl genrsa -out manager/certs/jwt_private.key 2048
        openssl rsa -in manager/certs/jwt_private.key -pubout -out manager/certs/jwt_public.key
    fi

    echo ""
}

# =============================================================================
# SECTION 8: Docker operations
# =============================================================================

build_and_start() {
    print_step "Building and starting Docker containers..."
    echo ""

    # Stop existing containers
    print_info "Stopping existing containers (if any)..."
    $COMPOSE_CMD down 2>/dev/null || true

    # Build and start
    print_info "Building containers (this may take a few minutes)..."
    $COMPOSE_CMD up --build -d

    # Wait for containers to start
    print_info "Waiting for containers to start..."
    sleep 10

    # Verify manager container
    if docker ps --format '{{.Names}}' | grep -q "seuxdr-manager"; then
        print_success "Manager container is running"
    else
        print_error "Manager container failed to start"
        echo "Checking logs..."
        docker logs seuxdr-manager 2>&1 | tail -20
        exit 1
    fi

    # Verify frontend container
    if docker ps --format '{{.Names}}' | grep -q "frontend"; then
        print_success "Frontend container is running"
    else
        print_warning "Frontend container may not be running"
    fi

    echo ""
}

# =============================================================================
# SECTION 9: Wazuh initialization
# =============================================================================

initialize_wazuh() {
    echo ""
    read -p "Do you want to initialize Wazuh integration? (takes several minutes) [y/N]: " init_wazuh

    if [ "$init_wazuh" = "y" ] || [ "$init_wazuh" = "Y" ]; then
        print_step "Initializing Wazuh integration..."
        print_warning "This process will take several minutes. Please wait..."
        echo ""

        # Run startup.sh inside the container
        docker exec -it seuxdr-manager /usr/local/bin/startup.sh TEST

        print_success "Wazuh initialization complete"
    else
        print_info "Skipping Wazuh initialization"
        print_info "You can initialize Wazuh later by running:"
        echo "  docker exec -it seuxdr-manager /usr/local/bin/startup.sh TEST"
    fi
    echo ""
}

# =============================================================================
# SECTION 10: Summary
# =============================================================================

print_summary() {
    echo ""
    echo -e "${CYAN}=============================================${NC}"
    echo -e "${CYAN}        Setup Complete!                     ${NC}"
    echo -e "${CYAN}=============================================${NC}"
    echo ""

    # Determine the access URL
    local access_domain="$CONFIG_DOMAIN"

    echo -e "${GREEN}Access URLs:${NC}"
    echo -e "  Frontend:    https://$access_domain:$PORT_FRONTEND"
    echo -e "  Manager API: https://$access_domain:$PORT_TLS"
    echo ""

    if [ "$USE_CUSTOM_CERTS" != "y" ]; then
        echo -e "${YELLOW}Self-Signed Certificate Note:${NC}"
        echo -e "  Your browser will show a security warning because the certificate"
        echo -e "  is self-signed. You can safely proceed or add the CA certificate"
        echo -e "  to your system's trusted certificates."
        echo ""
        echo -e "  CA Certificate: ${CYAN}$SCRIPT_DIR/manager/certs/server-ca.crt${NC}"
        echo ""
    fi

    echo -e "${GREEN}Useful Commands:${NC}"
    echo -e "  View logs:        docker logs -f seuxdr-manager"
    echo -e "  View frontend:    docker logs -f frontend"
    echo -e "  Stop containers:  $COMPOSE_CMD down"
    echo -e "  Restart:          $COMPOSE_CMD restart"
    echo -e "  Shell into mgr:   docker exec -it seuxdr-manager bash"
    echo ""

    echo -e "${GREEN}Container Status:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|seuxdr-manager|frontend"
    echo ""

    if [ "$USE_CUSTOM_CERTS" != "y" ]; then
        echo -e "${YELLOW}Next Steps:${NC}"
        echo -e "  1. Add the CA certificate to your browser/system to avoid warnings"
        echo -e "  2. Access the frontend at https://$access_domain:$PORT_FRONTEND"
        echo -e "  3. Register agents to start monitoring"
    fi
    echo ""
}

# =============================================================================
# MAIN
# =============================================================================

main() {
    print_header
    check_prerequisites
    collect_user_input
    backup_configs

    print_step "Updating configuration files..."
    update_localhost_ext
    update_manager_yaml
    update_frontend_env
    update_docker_compose
    update_dockerfile
    echo ""

    generate_certificates
    build_and_start
    initialize_wazuh
    print_summary
}

# Run main function
main "$@"
