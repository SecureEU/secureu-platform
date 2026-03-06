---
sidebar_position: 1
---

# Running Scans

This guide covers how to run security scans effectively using SECUR-EU.

## Scan Types Overview

| Scanner | Best For | Output |
|---------|----------|--------|
| Nmap | Network discovery, port scanning | Host info, services, OS |
| ZAP | Web application testing | Vulnerabilities, endpoints |
| Nuclei | Template-based scanning | CVEs, misconfigurations |

## Network Scanning with Nmap

### Starting a Network Scan

1. Navigate to **Scans** in the sidebar
2. Click **New Scan** button
3. Select **Network Scan (Nmap)**
4. Configure the scan:

| Field | Description | Example |
|-------|-------------|---------|
| Target | IP address, range, or hostname | `192.168.1.0/24` |
| Scan Type | Scanning intensity | Standard |
| Port Range | Specific ports to scan | `1-1000` or `all` |

5. Click **Start Scan**

### Scan Types Explained

**Quick Scan**
```bash
nmap -T4 -F <target>
```
- Fast host discovery
- Top 100 ports only
- Best for: Initial reconnaissance

**Standard Scan**
```bash
nmap -sV -sC <target>
```
- Service version detection
- Default NSE scripts
- Best for: Regular assessments

**Comprehensive Scan**
```bash
nmap -A -T4 <target>
```
- OS detection
- Version detection
- Script scanning
- Traceroute
- Best for: Full audits

**Stealth Scan**
```bash
nmap -sS -T2 <target>
```
- SYN scan (half-open)
- Slower timing
- Best for: IDS-sensitive environments

### Monitoring Progress

While a scan runs:

- **Progress bar** shows completion percentage
- **Live output** displays real-time results
- **Status** indicates current phase

### Understanding Results

After completion, view:

- **Host Summary**: Discovered hosts and their status
- **Port Details**: Open ports with services
- **Service Info**: Version information
- **Vulnerabilities**: Associated CVEs

## Web Application Scanning with ZAP

### Starting a Web Scan

1. Navigate to **Scans**
2. Click **New Scan**
3. Select **Web Scan (ZAP)**
4. Configure:

| Field | Description | Example |
|-------|-------------|---------|
| Target URL | Full URL to scan | `https://example.com` |
| Scan Type | Spider, Passive, Active, Full | Full |
| Authentication | Login credentials if needed | (optional) |

5. Click **Start Scan**

### Scan Phases

1. **Spider**: Crawls the application to discover pages
2. **Passive Scan**: Analyzes traffic for issues
3. **Active Scan**: Tests for vulnerabilities

### Configuration Options

**Spider Settings**
```json
{
  "maxDepth": 10,
  "maxDuration": 1800,
  "handleOData": true,
  "parseComments": true
}
```

**Active Scan Settings**
```json
{
  "strength": "MEDIUM",
  "threshold": "MEDIUM",
  "maxRuleDuration": 300
}
```

### Viewing Web Scan Results

Results include:

- **Alerts**: Categorized by risk level
- **Request/Response**: Evidence for each finding
- **Recommendations**: How to fix issues

## Template Scanning with Nuclei

### Starting a Nuclei Scan

1. Navigate to **Scans**
2. Click **New Scan**
3. Select **Template Scan (Nuclei)**
4. Configure:

| Field | Description | Example |
|-------|-------------|---------|
| Target | URL or host to scan | `https://example.com` |
| Templates | Template categories | CVEs, vulnerabilities |
| Severity | Filter by severity | Critical, High |

5. Click **Start Scan**

### Template Categories

- **cves**: Known CVE checks
- **vulnerabilities**: Generic vulnerability patterns
- **misconfigurations**: Configuration issues
- **exposures**: Sensitive data exposure
- **technologies**: Technology detection
- **default-logins**: Default credential checks

### Custom Templates

Use custom templates:

```yaml
id: custom-check

info:
  name: Custom Security Check
  severity: medium

http:
  - method: GET
    path:
      - "{{BaseURL}}/admin"
    matchers:
      - type: status
        status:
          - 200
```

## Multi-Target Scanning

### Batch Scans

Scan multiple targets simultaneously:

1. Navigate to **Scans**
2. Click **Multi-Scan**
3. Enter targets (one per line or upload file)
4. Select scanner and configuration
5. Click **Start Multi-Scan**

### Parallel Execution

Multi-scans run in parallel:

- Each target gets a separate container
- Results aggregate automatically
- Progress tracked per target

## Scheduling Scans

### Creating a Schedule

1. Navigate to **Scans** → **Schedules**
2. Click **New Schedule**
3. Configure:

| Field | Description |
|-------|-------------|
| Name | Schedule identifier |
| Scan Profile | Pre-configured scan settings |
| Frequency | Daily, Weekly, Monthly |
| Time | When to run |
| Targets | What to scan |

4. Click **Save Schedule**

### Managing Schedules

- **Enable/Disable**: Toggle schedule status
- **Edit**: Modify schedule settings
- **History**: View past executions
- **Delete**: Remove schedule

## Best Practices

### Before Scanning

1. ✅ Verify you have authorization
2. ✅ Define clear scope
3. ✅ Choose appropriate scan type
4. ✅ Consider timing (off-peak hours)
5. ✅ Notify relevant stakeholders

### During Scanning

1. ✅ Monitor scan progress
2. ✅ Watch for errors
3. ✅ Check resource usage
4. ✅ Be ready to stop if needed

### After Scanning

1. ✅ Review all findings
2. ✅ Prioritize by severity
3. ✅ Assign remediation tasks
4. ✅ Export reports
5. ✅ Compare with previous scans

## Troubleshooting

### Scan Won't Start

| Issue | Solution |
|-------|----------|
| Docker not running | Start Docker daemon |
| Port conflict | Change scan port or stop conflicting service |
| Invalid target | Verify target format |

### Scan Stuck

| Issue | Solution |
|-------|----------|
| Target unresponsive | Check target availability |
| Timeout too short | Increase timeout value |
| Resource exhaustion | Reduce scan scope |

### No Results

| Issue | Solution |
|-------|----------|
| Firewall blocking | Check firewall rules |
| Wrong port | Specify correct port range |
| Target down | Verify target is online |

## Related

- [Dashboard](/features/dashboard)
- [Managing Assets](/user-guide/managing-assets)
- [Generating Reports](/user-guide/generating-reports)
