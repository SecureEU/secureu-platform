---
sidebar_position: 3
---

# Quick Start

Get up and running with SECUR-EU in minutes. This guide walks you through the essential workflows.

## Starting the Platform

### 1. Start Services

```bash
# Start MongoDB
docker compose up -d

# Start Backend (terminal 1)
./offensive_solutions

# Start Frontend (terminal 2)
cd secur-eu-dashboard
npm run dev
```

### 2. Access the Dashboard

Open your browser and navigate to:

- **Dashboard**: [http://localhost:3000](http://localhost:3000)
- **API Docs**: [http://localhost:3001/docs](http://localhost:3001/docs)

## Your First Scan

### Network Scan with Nmap

1. Navigate to **Scans** in the sidebar
2. Click **New Scan**
3. Select **Network Scan (Nmap)**
4. Enter target details:
   - **Target**: `192.168.1.0/24` or a specific IP
   - **Scan Type**: Standard
   - **Options**: Enable service detection
5. Click **Start Scan**

```bash
# Example Nmap command generated
nmap -sV -sC -oX output.xml 192.168.1.0/24
```

### Web Application Scan

1. Navigate to **Scans**
2. Click **New Scan**
3. Select **Web Scan (ZAP)**
4. Enter target URL:
   - **Target URL**: `https://example.com`
   - **Scan Type**: Active Scan
   - **Spider**: Enable
5. Click **Start Scan**

## Viewing Results

### Dashboard Overview

The dashboard provides a security posture overview:

- **Total Vulnerabilities**: Aggregate count across all scans
- **Severity Distribution**: Critical, High, Medium, Low breakdown
- **Recent Scans**: Latest scan activities
- **Trend Charts**: Security trends over time

### Scan Details

Click on any scan to view:

- **Summary**: Quick vulnerability overview
- **Findings**: Detailed vulnerability list
- **Raw Output**: Original scanner output
- **Recommendations**: AI-generated fix suggestions

## Running Exploitation Tests

:::caution Authorization Required
Only run exploitation tests against systems you own or have explicit authorization to test.
:::

### Metasploit Module Execution

1. Navigate to **Exploitation**
2. Click **New Test**
3. Select a vulnerability to exploit
4. Configure the module:
   - **Module**: `exploit/multi/http/example`
   - **RHOSTS**: Target IP
   - **RPORT**: Target port
5. Click **Run Exploit**

### Viewing Exploitation Results

Results include:
- Session information
- Payload execution status
- Evidence collected
- Remediation recommendations

## Using the AI Assistant

### Vulnerability Analysis

1. Navigate to **AI Assistant**
2. Select a scan or paste vulnerability data
3. Ask questions like:
   - "What are the most critical vulnerabilities?"
   - "How do I fix CVE-2024-1234?"
   - "Explain this vulnerability in simple terms"

### Example Prompts

```
Analyze the security posture based on the latest scan results.

What remediation steps should I prioritize?

Generate a security report for management.
```

## Managing Assets

### Adding Hosts

1. Navigate to **Assets**
2. Click **Add Host**
3. Enter host details:
   - **Hostname/IP**: Target identifier
   - **Tags**: Environment labels
   - **Notes**: Additional context

### Organizing with Tags

Use tags to organize assets:
- `production` / `staging` / `development`
- `web-server` / `database` / `api`
- `critical` / `standard`

## Compliance Checking

### Running CRA Compliance

1. Navigate to **Compliance**
2. Select **CRA Assessment**
3. Choose target systems
4. Click **Run Assessment**

### Understanding Results

Compliance results show:
- ✅ **Pass**: Requirements met
- ⚠️ **Warning**: Partial compliance
- ❌ **Fail**: Non-compliant items
- 📋 **Recommendations**: Steps to achieve compliance

## Quick Tips

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl/Cmd + K` | Quick search |
| `Ctrl/Cmd + N` | New scan |
| `Escape` | Close modal |

### Best Practices

1. **Regular Scans**: Schedule weekly vulnerability scans
2. **Asset Management**: Keep host inventory updated
3. **Prioritize Fixes**: Address critical vulnerabilities first
4. **Document Changes**: Use notes for tracking remediation
5. **Review AI Suggestions**: Validate AI recommendations

## Next Steps

- [Detailed Scan Guide](/user-guide/running-scans)
- [Exploitation Testing](/user-guide/exploitation-testing)
- [Generating Reports](/user-guide/generating-reports)
- [API Integration](/api/overview)
