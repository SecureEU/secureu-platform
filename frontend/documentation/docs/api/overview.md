---
sidebar_position: 1
---

# API Overview

The SECUR-EU API provides programmatic access to all platform features.

## Base URL

```
http://localhost:3001
```

## Interactive Documentation

Access the Swagger UI for interactive API exploration:

```
http://localhost:3001/docs
```

## Response Format

All API responses use JSON format:

```json
{
  "data": { ... },
  "status": "success",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Error Responses

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Description of the error"
  },
  "status": "error",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 404 | Not Found |
| 500 | Server Error |

## API Categories

### General

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Health check |
| `/overview` | GET | Dashboard statistics |

### Scans

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/scans` | GET | List all scans |
| `/scans/:id` | GET | Get scan details |
| `/scan/nmap` | POST | Start Nmap scan |
| `/scan/zap` | POST | Start ZAP scan |
| `/scan/nuclei` | POST | Start Nuclei scan |

### Hosts

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/hosts` | GET | List all hosts |
| `/hosts` | POST | Create host |
| `/hosts/:id` | PUT | Update host |
| `/hosts/:id` | DELETE | Delete host |

### Exploitation

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/metasploit/modules` | GET | List modules |
| `/metasploit/exploit` | POST | Run exploit |
| `/metasploit/sessions` | GET | List sessions |

### AI Assistant

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/ai/chat` | POST | Chat with AI |
| `/ai/analyze` | POST | Analyze vulnerability |

### Compliance

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/compliance/cra` | GET | Get CRA status |
| `/compliance/cra/assess` | POST | Run assessment |

### STIX

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/stix/import` | POST | Import STIX bundle |
| `/stix/export` | GET | Export as STIX |

## Request Headers

### Required Headers

```
Content-Type: application/json
```

### Optional Headers

```
ngrok-skip-browser-warning: true  # For ngrok tunnels
```

## Rate Limiting

The API implements rate limiting:

- **Default**: 100 requests per minute
- **Scans**: 10 concurrent scan operations
- **AI**: 20 requests per minute

## Pagination

List endpoints support pagination:

```bash
GET /scans?limit=10&offset=20

# Response includes
{
  "data": [...],
  "pagination": {
    "total": 100,
    "limit": 10,
    "offset": 20
  }
}
```

## Filtering

Filter results with query parameters:

```bash
# Filter by status
GET /scans?status=completed

# Filter by severity
GET /scans?severity=critical

# Multiple filters
GET /scans?status=completed&severity=high
```

## Quick Start

### Get Dashboard Overview

```bash
curl http://localhost:3001/overview
```

### Start a Network Scan

```bash
curl -X POST http://localhost:3001/scan/nmap \
  -H "Content-Type: application/json" \
  -d '{"target": "192.168.1.0/24", "scanType": "standard"}'
```

### List All Scans

```bash
curl http://localhost:3001/scans
```

## SDKs and Libraries

### JavaScript/TypeScript

```javascript
// Example using fetch
const response = await fetch('http://localhost:3001/overview');
const data = await response.json();
```

### Python

```python
import requests

response = requests.get('http://localhost:3001/overview')
data = response.json()
```

### Go

```go
resp, err := http.Get("http://localhost:3001/overview")
if err != nil {
    log.Fatal(err)
}
defer resp.Body.Close()
```

## Next Steps

- [Authentication](/api/authentication)
- [Endpoint Reference](/api/endpoints)
- [Swagger UI](http://localhost:3001/docs)
