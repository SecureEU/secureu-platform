---
sidebar_position: 1
---

# Installation

This guide walks you through installing the SECUR-EU platform on your system.

## Prerequisites

Before installing SECUR-EU, ensure you have the following:

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Node.js | >= 18.0 | Frontend runtime |
| Go | >= 1.21 | Backend runtime |
| Docker | Latest | Container management |
| Docker Compose | v2+ | Multi-container orchestration |
| MongoDB | >= 6.0 | Database |

### System Requirements

- **Operating System**: Linux (Ubuntu 22.04+ recommended), macOS, or Windows with WSL2
- **RAM**: 4GB minimum, 8GB recommended
- **Disk Space**: 20GB minimum for containers and data
- **Network**: Internet access for pulling Docker images

## Installation Steps

### 1. Clone the Repositories

```bash
# Clone the frontend repository
git clone https://github.com/secur-eu/secur-eu-dashboard.git
cd secur-eu-dashboard
git checkout production

# Clone the backend repository (in a separate directory)
git clone https://github.com/secur-eu/offensive-solutions.git
cd offensive-solutions
```

### 2. Install Frontend Dependencies

```bash
cd secur-eu-dashboard
npm install
```

### 3. Build the Backend

```bash
cd offensive-solutions
make build
```

This creates the `offensive_solutions` binary.

### 4. Configure Environment

Create a `.env` file in the backend directory:

```bash
# offensive-solutions/.env
RPATH=/path/to/your/reports
MONGO_URI=mongodb://localhost:27017
OLLAMA_URL=http://localhost:11434
```

### 5. Start MongoDB

Using Docker Compose:

```bash
cd offensive-solutions
docker compose up -d
```

This starts MongoDB with persistent storage.

### 6. Start the Backend

```bash
./offensive_solutions
```

The backend API will be available at `http://localhost:3001`.

### 7. Start the Frontend

```bash
cd secur-eu-dashboard
npm run dev
```

The frontend will be available at `http://localhost:3000`.

## Docker Installation (Alternative)

You can also run the entire stack using Docker:

```bash
# Build and start all services
docker compose -f docker-compose.full.yml up -d
```

## Verification

After installation, verify everything is working:

1. **Frontend**: Open `http://localhost:3000` in your browser
2. **Backend API**: Visit `http://localhost:3001/docs` for Swagger UI
3. **MongoDB**: Check connection with `docker exec -it mongodb mongosh`

## Next Steps

- [Configure the platform](/getting-started/configuration)
- [Quick Start Guide](/getting-started/quick-start)
- [Run your first scan](/user-guide/running-scans)

## Troubleshooting

### Port Conflicts

If ports 3000 or 3001 are in use:

```bash
# Find process using port
lsof -i :3000

# Kill the process
kill -9 <PID>
```

### MongoDB Connection Issues

```bash
# Check if MongoDB is running
docker ps | grep mongo

# Check logs
docker logs mongodb
```

### Docker Permission Errors

```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Log out and back in, or run
newgrp docker
```
