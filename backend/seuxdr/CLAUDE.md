# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SEUXDR is an open-source host-based intrusion detection system (HIDS) written in Go with a React frontend. It performs real-time log analysis, integrity checking, Windows registry monitoring, rootkit detection, and active response to threats across Linux, Windows, and macOS platforms.

## Architecture

The system follows a distributed agent-manager architecture:

### Core Components
- **Manager (`/manager/`)**: Central server that coordinates agents, provides web API, and manages security policies
  - TLS server (port 8443) for web interface and API
  - mTLS server (port 8081) for secure agent registration and communication
  - SQLite database with GORM ORM
  - Role-based access control (RBAC)
  - JWT-based authentication

- **Agent (`/agent/`)**: Lightweight daemon that runs on monitored hosts
  - Cross-platform service (Windows/Linux/macOS)
  - Log monitoring and analysis
  - File integrity monitoring
  - Secure communication with manager via mTLS
  - Self-updating capability

- **Frontend (`/manager_front/`)**: React-based web interface
  - Vite build system with TypeScript
  - Ant Design UI components
  - Chart.js for data visualization
  - Zustand for state management

### Security Architecture
- **mTLS Communication**: Agent registration uses mutual TLS authentication for the manager to hand off encryption keys to agent, then subsequent agent-manager communication uses TLS with encrypted messages
- **Certificate Management**: Automated certificate generation and rotation
- **Encryption**: AES encryption for sensitive data, RSA keys for key exchange
- **Authentication**: JWT tokens with RSA signatures
- **RBAC**: Fine-grained permissions system with organizations, groups, and roles

## Development Commands

### Manager (Go Backend)
```bash
# Run manager server
cd manager
go run main.go

# Database migrations
make migration_up      # Apply migrations
make migration_down    # Rollback migrations
make migration_fix     # Fix migration issues

# Run tests
go test ./...
```

### Agent (Go Service)
```bash
# Run agent in test mode
go run ./agent

# Install as system service
go run ./agent install

# Run agent standalone
go run ./agent run
```

### Frontend (React)
```bash
cd manager_front
npm install
npm run dev         # Development server
npm run build       # Production build
npm run lint        # ESLint checking
```

### Docker Deployment
```bash
# Generate certificates first
sh gen-certs.sh

# Start services
docker compose up --build -d

# Initialize wazuh integration
docker exec -it seuxdr-manager /usr/local/bin/startup.sh TEST
```

## Key Configuration Files

- **`manager/manager.yaml`**: Main configuration for manager server
  - TLS/mTLS certificate paths
  - Database settings
  - Agent generation settings
  - Installation script templates

- **`agent/config/agent_base_config.yml`**: Base agent configuration template
- **`docker-compose.yml`**: Container orchestration setup
- **`manager_front/.env`**: Frontend environment variables

## Testing

### Go Tests
- Unit tests are located alongside source files (`*_test.go`)
- Mocks are generated in `manager/mocks/` using `go generate`
- Test utilities in `manager/utils/testutils.go`

### Common Test Commands
```bash
# Run all tests
go test ./...

# Run specific package tests
go test ./manager/db/...

# Run with coverage
go test -cover ./...

# Generate mocks
go generate ./...
```

## Database

- **Type**: SQLite with GORM ORM
- **Migrations**: Located in `manager/database/migrations/`
- **Models**: Database models in `manager/models/models.go`
- **Repositories**: Data access layer in `manager/db/`

### Key Tables
- `agents`: Registered agent information
- `users`: System users with RBAC
- `organisations`: Multi-tenant organization structure
- `groups`: Agent grouping for management
- `roles`: Permission-based roles
- `sessions`: User session management

## Build and Deployment

### Agent Binary Generation
The manager can generate platform-specific agent binaries:
- **TEST mode**: Generates configs and certs in local directories
- **PROD mode**: Embeds configs/certs into executable and creates installers

### Platform Support
- **Linux**: systemd service, DEB/RPM packages
- **Windows**: Windows service, PowerShell installers
- **macOS**: launchd service, shell installers

## Security Considerations

- Certificate paths are configurable in `manager.yaml`
- Encryption keys are generated separately for each environment
- Agent-manager communication requires valid client certificates
- All sensitive data is encrypted before database storage
- JWT tokens have configurable expiration
- Rate limiting is implemented on all API endpoints

## Integration with Wazuh

The system integrates with Wazuh SIEM for enhanced log analysis:
- Logs are forwarded to `/var/seuxdr/manager/queue/`
- Wazuh monitors this directory for analysis
- OpenSearch/Elasticsearch backend for log storage and querying

## Common Development Patterns

- **Dependency Injection**: Services are injected into handlers
- **Repository Pattern**: Database access is abstracted through repositories
- **Middleware**: Authentication, logging, and rate limiting via Gin middleware
- **Embedded Resources**: Static files and configs are embedded using `embed.FS`
- **Cross-Platform Code**: Build tags separate platform-specific implementations

## Environment Variables

Key environment variables (see `manager_front/.env`):
- `VITE_API_URL`: Backend API URL for frontend
- Certificate paths and TLS settings in `manager.yaml`