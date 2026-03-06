---
sidebar_position: 1
---

# Dashboard

The SECUR-EU Dashboard provides a comprehensive overview of your security posture, aggregating data from all scans and assessments.

## Overview

The dashboard is your command center for security operations, displaying:

- Real-time vulnerability statistics
- Severity distribution charts
- Recent scan activity
- Security trend analysis

![Dashboard Overview](/img/dashboard-overview.png)

## Key Metrics

### Vulnerability Count

The main metrics panel shows:

| Metric | Description |
|--------|-------------|
| **Total Vulnerabilities** | Sum of all identified vulnerabilities |
| **Critical** | Requires immediate attention (CVSS 9.0-10.0) |
| **High** | Should be fixed soon (CVSS 7.0-8.9) |
| **Medium** | Plan for remediation (CVSS 4.0-6.9) |
| **Low** | Fix when possible (CVSS 0.1-3.9) |

### Severity Distribution

The pie chart visualizes vulnerability distribution by severity:

```
Critical ████████░░░░░░░░░░░░ 15%
High     ████████████░░░░░░░░ 25%
Medium   ████████████████░░░░ 40%
Low      ████████░░░░░░░░░░░░ 20%
```

## Recent Scans

The recent scans panel displays:

- **Scan Type**: Nmap, ZAP, Nuclei, etc.
- **Target**: Scanned host or URL
- **Status**: Running, Completed, Failed
- **Findings**: Vulnerability count
- **Timestamp**: When the scan ran

### Scan Status Indicators

| Status | Indicator | Meaning |
|--------|-----------|---------|
| Running | 🔵 Blue spinner | Scan in progress |
| Completed | ✅ Green check | Scan finished successfully |
| Failed | ❌ Red X | Scan encountered errors |
| Queued | ⏳ Yellow clock | Waiting to start |

## Trend Analysis

### Security Trend Chart

The trend chart shows vulnerability counts over time:

- **X-Axis**: Date/time
- **Y-Axis**: Vulnerability count
- **Lines**: Separate lines per severity

### Interpreting Trends

- **Downward trend**: Security improving, vulnerabilities being fixed
- **Upward trend**: New vulnerabilities discovered, needs attention
- **Flat line**: Stable state, no significant changes

## Widgets

### Top Vulnerabilities

Lists the most critical findings:

1. Vulnerability name
2. Affected systems count
3. CVSS score
4. Quick link to details

### Asset Summary

Quick overview of managed assets:

- Total hosts monitored
- Hosts with critical vulnerabilities
- Recently added assets
- Asset health score

## Filtering & Search

### Time Range Filter

Filter dashboard data by time:

- Last 24 hours
- Last 7 days
- Last 30 days
- Custom range

### Severity Filter

Focus on specific severity levels:

```jsx
// Example filter usage
const filters = {
  severity: ['critical', 'high'],
  timeRange: '7d',
  scanType: 'all'
};
```

## Dashboard Actions

### Quick Actions

| Action | Description |
|--------|-------------|
| **New Scan** | Start a new security scan |
| **Export Report** | Download PDF/CSV report |
| **Refresh** | Update dashboard data |
| **Settings** | Configure dashboard |

### Refresh Behavior

The dashboard auto-refreshes:

- Every 30 seconds when scans are running
- Every 5 minutes in idle state
- Manual refresh available

## Customization

### Widget Arrangement

Drag and drop widgets to customize layout:

1. Click the widget header
2. Drag to new position
3. Release to place

### Display Options

Configure what's shown:

```javascript
const dashboardConfig = {
  showTrendChart: true,
  showRecentScans: true,
  showTopVulns: true,
  maxRecentScans: 10,
  chartTimeRange: '30d'
};
```

## API Integration

### Fetching Dashboard Data

```bash
# Get overview statistics
curl http://localhost:3001/overview

# Response
{
  "totalVulnerabilities": 127,
  "critical": 12,
  "high": 35,
  "medium": 55,
  "low": 25,
  "lastUpdated": "2024-01-15T10:30:00Z"
}
```

### Real-time Updates

The dashboard uses polling for updates:

```javascript
useEffect(() => {
  const interval = setInterval(() => {
    fetchDashboardData();
  }, 30000);

  return () => clearInterval(interval);
}, []);
```

## Best Practices

1. **Regular Review**: Check dashboard daily
2. **Act on Critical**: Prioritize critical vulnerabilities
3. **Track Trends**: Monitor security trends weekly
4. **Export Reports**: Generate reports for stakeholders
5. **Set Alerts**: Configure notifications for new criticals

## Related

- [Running Scans](/user-guide/running-scans)
- [Managing Assets](/user-guide/managing-assets)
- [Generating Reports](/user-guide/generating-reports)
