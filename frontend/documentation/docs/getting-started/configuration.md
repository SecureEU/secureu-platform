---
sidebar_position: 2
---

# Configuration

Learn how to configure the SECUR-EU platform for your environment.

## Backend Configuration

### Environment Variables

The backend uses environment variables for configuration. Create a `.env` file:

```bash
# Database Configuration
MONGO_URI=mongodb://localhost:27017
MONGO_DB=secureu

# Report Storage
RPATH=/home/user/secur-eu-reports

# AI Assistant (Ollama)
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3

# Server Settings
PORT=3001
```

### Environment Variable Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGO_URI` | `mongodb://localhost:27017` | MongoDB connection string |
| `MONGO_DB` | `secureu` | Database name |
| `RPATH` | `/tmp/reports` | Report storage directory |
| `OLLAMA_URL` | `http://localhost:11434` | Ollama API endpoint |
| `OLLAMA_MODEL` | `llama3` | AI model to use |
| `PORT` | `3001` | Backend server port |

## Frontend Configuration

### API Endpoint

The frontend connects to the backend API. Configure in `next.config.js`:

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  env: {
    API_URL: process.env.API_URL || 'http://localhost:3001',
  },
};

module.exports = nextConfig;
```

### Environment Variables

Create `.env.local` for frontend configuration:

```bash
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:3001

# Feature Flags
NEXT_PUBLIC_ENABLE_AI=true
NEXT_PUBLIC_ENABLE_EXPLOITATION=true
```

## Docker Configuration

### Docker Compose

The `docker-compose.yml` configures container services:

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:6.0
    container_name: secur-eu-mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
    restart: unless-stopped

  backend:
    build: ./offensive-solutions
    container_name: secur-eu-backend
    ports:
      - "3001:3001"
    environment:
      - MONGO_URI=mongodb://mongodb:27017
      - RPATH=/app/reports
    volumes:
      - ./reports:/app/reports
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - mongodb
    restart: unless-stopped

  frontend:
    build: ./secur-eu-dashboard
    container_name: secur-eu-frontend
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_URL=http://backend:3001
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  mongodb_data:
```

## Scanner Configuration

### Nmap Settings

Configure Nmap scan profiles in the scans interface:

| Profile | Flags | Use Case |
|---------|-------|----------|
| Quick | `-T4 -F` | Fast host discovery |
| Standard | `-sV -sC` | Service detection |
| Comprehensive | `-A -T4` | Full audit |
| Stealth | `-sS -T2` | IDS evasion |

### ZAP Configuration

OWASP ZAP settings:

```yaml
# zap-config.yaml
spider:
  maxDuration: 60
  maxDepth: 5
  threadCount: 5

scanner:
  strength: MEDIUM
  threshold: MEDIUM
  maxDuration: 120
```

### Nuclei Templates

Configure Nuclei template directories:

```bash
# Template locations
NUCLEI_TEMPLATES=/home/user/nuclei-templates
NUCLEI_CUSTOM=/home/user/custom-templates
```

## Security Configuration

### CORS Settings

Configure allowed origins in the backend:

```go
e.Use(middleware.CORSWithConfig(middleware.CORSConfig{
    AllowOrigins: []string{
        "http://localhost:3000",
        "https://your-domain.com",
    },
    AllowMethods: []string{
        echo.GET, echo.PUT, echo.POST, echo.DELETE,
    },
    AllowHeaders: []string{
        echo.HeaderOrigin,
        echo.HeaderContentType,
        echo.HeaderAccept,
    },
}))
```

### Rate Limiting

Enable rate limiting for API endpoints:

```go
e.Use(middleware.RateLimiter(middleware.NewRateLimiterMemoryStore(
    rate.Limit(20), // 20 requests per second
)))
```

## Ollama AI Configuration

### Installing Ollama

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull a model
ollama pull llama3

# Start Ollama server
ollama serve
```

### Available Models

| Model | Size | Best For |
|-------|------|----------|
| `llama3` | 4.7GB | General analysis |
| `codellama` | 3.8GB | Code review |
| `mistral` | 4.1GB | Fast responses |

## Next Steps

- [Quick Start Guide](/getting-started/quick-start)
- [Running Scans](/user-guide/running-scans)
