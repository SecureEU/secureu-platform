---
sidebar_position: 2
---

# Security Scanning

SECUR-EU integrates multiple security scanning tools to provide comprehensive vulnerability assessment.

## Supported Scanners

### Nmap - Network Scanner

Network discovery and security auditing tool.

**Capabilities:**
- Host discovery
- Port scanning
- Service/version detection
- OS detection
- Script scanning (NSE)

**Supported Scan Types:**

| Type | Command | Use Case |
|------|---------|----------|
| Quick | `-T4 -F` | Fast host discovery |
| Standard | `-sV -sC` | Service detection with default scripts |
| Comprehensive | `-A -T4` | Full audit with OS detection |
| Stealth | `-sS -T2` | SYN scan, slower but stealthier |
| UDP | `-sU` | UDP port scanning |

### OWASP ZAP - Web Scanner

Web application security scanner.

**Capabilities:**
- Spider/crawler
- Passive scanning
- Active scanning
- AJAX spider
- Authentication handling

**Scan Modes:**

| Mode | Description |
|------|-------------|
| Spider | Crawl application to discover content |
| Passive | Analyze traffic without attacking |
| Active | Test for vulnerabilities actively |
| Full | Combined spider + passive + active |

### Nuclei - Template Scanner

Fast vulnerability scanner using templates.

**Capabilities:**
- Template-based scanning
- Custom template support
- Multi-protocol support
- Fast parallel execution

**Template Categories:**

- `cves/` - Known CVE checks
- `vulnerabilities/` - Generic vulnerability checks
- `misconfigurations/` - Configuration issues
- `exposures/` - Sensitive data exposure
- `technologies/` - Technology detection

## Creating Scans

### Network Scan

```bash
# API Request
POST /scan/nmap
Content-Type: application/json

{
  "target": "192.168.1.0/24",
  "scanType": "standard",
  "options": {
    "serviceDetection": true,
    "scriptScan": true,
    "osDetection": false
  }
}
```

### Web Application Scan

```bash
# API Request
POST /scan/zap
Content-Type: application/json

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

### Nuclei Scan

```bash
# API Request
POST /scan/nuclei
Content-Type: application/json

{
  "target": "https://example.com",
  "templates": ["cves", "vulnerabilities"],
  "severity": ["critical", "high", "medium"]
}
```

## Scan Lifecycle

### Status Flow

```
┌──────────┐    ┌─────────┐    ┌───────────┐    ┌───────────┐
│  Queued  │───►│ Running │───►│ Processing│───►│ Completed │
└──────────┘    └─────────┘    └───────────┘    └───────────┘
                     │                               │
                     │         ┌────────┐           │
                     └────────►│ Failed │◄──────────┘
                               └────────┘
```

### Status Descriptions

| Status | Description |
|--------|-------------|
| Queued | Scan submitted, waiting for resources |
| Running | Scan actively executing |
| Processing | Parsing results and storing |
| Completed | Scan finished successfully |
| Failed | Error occurred during scan |

## Viewing Results

### Scan Summary

Each completed scan shows:

- **Total Findings**: Number of vulnerabilities found
- **Severity Breakdown**: Count per severity level
- **Scan Duration**: Time taken to complete
- **Target Information**: Host/URL details

### Finding Details

Each finding includes:

```json
{
  "id": "finding-001",
  "title": "SQL Injection",
  "severity": "high",
  "cvss": 8.6,
  "description": "SQL injection vulnerability in login form",
  "evidence": {
    "url": "https://example.com/login",
    "parameter": "username",
    "payload": "' OR '1'='1"
  },
  "remediation": "Use parameterized queries",
  "references": [
    "https://owasp.org/www-community/attacks/SQL_Injection"
  ]
}
```

## Scan Configuration

### Profiles

Save scan configurations as profiles:

```json
{
  "name": "Weekly Web Audit",
  "scanner": "zap",
  "config": {
    "scanType": "full",
    "maxDuration": 7200,
    "spider": {
      "maxDepth": 10,
      "maxDuration": 1800
    }
  }
}
```

### Scheduling

Schedule recurring scans:

```json
{
  "profile": "weekly-web-audit",
  "schedule": {
    "frequency": "weekly",
    "day": "sunday",
    "time": "02:00",
    "timezone": "UTC"
  }
}
```

## Multi-Target Scanning

### Batch Scanning

Scan multiple targets in one request:

```bash
POST /scan/multi
Content-Type: application/json

{
  "scanner": "nmap",
  "targets": [
    "192.168.1.10",
    "192.168.1.20",
    "192.168.1.30"
  ],
  "scanType": "standard"
}
```

### Concurrent Execution

Multi-scans run in parallel containers:

- Each target gets its own container
- Results are aggregated after completion
- Failed scans don't affect others

## Container Management

### Scan Containers

Scans run in isolated Docker containers:

```yaml
# Scanner container example
container:
  image: nmap-scanner:latest
  resources:
    memory: 512m
    cpu: 0.5
  timeout: 3600s
```

### Monitoring

View running containers:

```bash
# List running scan containers
docker ps --filter "label=secur-eu-scan"

# View container logs
docker logs <container-id>
```

## Best Practices

### Scanning Guidelines

1. **Get Authorization**: Always have permission to scan targets
2. **Start Small**: Begin with quick scans, then comprehensive
3. **Schedule Off-Hours**: Run intensive scans during low-traffic periods
4. **Review Results**: Don't just collect - analyze and act
5. **Track Changes**: Compare results over time

### Performance Tips

- Limit concurrent scans to available resources
- Use appropriate timeouts
- Exclude known-safe paths in web scans
- Use scan profiles for consistency

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Scan timeout | Target slow or unreachable | Increase timeout, check connectivity |
| No results | Firewall blocking | Check firewall rules, try different scan type |
| Container crash | Resource exhaustion | Reduce scan scope, increase resources |

### Debug Mode

Enable verbose logging:

```bash
# Set environment variable
SCAN_DEBUG=true ./offensive_solutions
```

## Related

- [Quick Start](/getting-started/quick-start)
- [Managing Assets](/user-guide/managing-assets)
- [API Reference](/api/endpoints)
