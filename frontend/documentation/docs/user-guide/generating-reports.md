---
sidebar_position: 4
---

# Generating Reports

Learn how to create comprehensive security reports from SECUR-EU data.

## Report Types

| Type | Audience | Content |
|------|----------|---------|
| Executive | Management | High-level summary, risk overview |
| Technical | Security Team | Detailed findings, remediation steps |
| Compliance | Auditors | Control status, evidence |
| Vulnerability | IT Operations | Specific vuln details, fixes |

## Creating Reports

### From Dashboard

1. Navigate to **Dashboard**
2. Click **Export Report**
3. Select report type
4. Choose format (PDF, HTML, JSON, CSV)
5. Click **Generate**

### From Scan Results

1. Navigate to **Scans**
2. Select completed scan
3. Click **Generate Report**
4. Configure options
5. Download report

### From Exploitation Results

1. Navigate to **Exploitation**
2. Select test results
3. Click **Generate Report**
4. Include evidence options
5. Download report

## Report Configuration

### Executive Report

```json
{
  "type": "executive",
  "sections": {
    "summary": true,
    "riskOverview": true,
    "trends": true,
    "recommendations": true
  },
  "dateRange": "30d",
  "includeCharts": true
}
```

### Technical Report

```json
{
  "type": "technical",
  "sections": {
    "methodology": true,
    "findings": true,
    "evidence": true,
    "remediation": true,
    "appendices": true
  },
  "severityFilter": ["critical", "high", "medium"],
  "includeRawOutput": false
}
```

### Compliance Report

```json
{
  "type": "compliance",
  "framework": "cra",
  "sections": {
    "scope": true,
    "controlStatus": true,
    "evidence": true,
    "gaps": true,
    "roadmap": true
  }
}
```

## Report Sections

### Executive Summary

- Overall security posture score
- Key findings count by severity
- Risk trends over time
- Top recommendations

### Findings Detail

For each finding:

| Field | Description |
|-------|-------------|
| Title | Vulnerability name |
| Severity | Critical/High/Medium/Low |
| CVSS | Numeric score |
| Affected Assets | Impacted systems |
| Description | What the issue is |
| Evidence | Proof of vulnerability |
| Remediation | How to fix |
| References | CVE, CWE links |

### Risk Analysis

- Severity distribution charts
- Asset risk matrix
- Trend analysis
- Comparison with benchmarks

### Remediation Roadmap

- Prioritized fix list
- Effort estimates
- Dependencies
- Timeline suggestions

## Customization

### Branding

Add your organization's branding:

```json
{
  "branding": {
    "logo": "/path/to/logo.png",
    "companyName": "Your Company",
    "primaryColor": "#2563eb",
    "footerText": "Confidential - Internal Use Only"
  }
}
```

### Templates

Create reusable templates:

1. Navigate to **Settings** → **Report Templates**
2. Click **New Template**
3. Configure sections and styling
4. Save template
5. Use when generating reports

### Custom Sections

Add custom content:

```json
{
  "customSections": [
    {
      "title": "Testing Scope",
      "content": "This assessment covered...",
      "position": "after:summary"
    }
  ]
}
```

## Export Formats

### PDF

Best for:
- Sharing with stakeholders
- Printing
- Archiving

Options:
- Page size (A4, Letter)
- Orientation
- Table of contents
- Page numbers

### HTML

Best for:
- Interactive viewing
- Web hosting
- Email embedding

Options:
- Standalone (embedded CSS)
- Navigation sidebar
- Expandable sections

### JSON

Best for:
- Integration with other tools
- Programmatic processing
- Data analysis

### CSV

Best for:
- Spreadsheet analysis
- Bulk data export
- Simple imports

## Scheduled Reports

### Creating a Schedule

1. Navigate to **Reports** → **Schedules**
2. Click **New Schedule**
3. Configure:

| Field | Description |
|-------|-------------|
| Name | Schedule identifier |
| Template | Report template to use |
| Frequency | Daily, Weekly, Monthly |
| Recipients | Email addresses |
| Format | PDF, HTML, etc. |

4. Click **Save**

### Example Schedule

```json
{
  "name": "Weekly Security Summary",
  "template": "executive-weekly",
  "frequency": "weekly",
  "dayOfWeek": "monday",
  "time": "08:00",
  "recipients": [
    "ciso@company.com",
    "security-team@company.com"
  ],
  "format": "pdf"
}
```

## API Report Generation

### Generate Report

```bash
POST /reports/generate
Content-Type: application/json

{
  "type": "technical",
  "scanIds": ["scan-001", "scan-002"],
  "format": "pdf",
  "options": {
    "includeEvidence": true,
    "severityFilter": ["critical", "high"]
  }
}
```

### Response

```json
{
  "reportId": "report-12345",
  "status": "generating",
  "estimatedTime": 30
}
```

### Download Report

```bash
GET /reports/download/report-12345

# Returns report file
```

### List Reports

```bash
GET /reports

# Response
{
  "reports": [
    {
      "id": "report-12345",
      "type": "technical",
      "createdAt": "2024-01-15T10:30:00Z",
      "format": "pdf",
      "size": 1048576
    }
  ]
}
```

## Best Practices

### Effective Reports

1. **Know your audience** - Tailor content appropriately
2. **Prioritize findings** - Lead with most important
3. **Be actionable** - Include clear remediation
4. **Use visuals** - Charts aid understanding
5. **Keep it concise** - Executive summaries matter

### Report Distribution

1. **Secure delivery** - Use encrypted channels
2. **Need to know** - Limit distribution
3. **Version control** - Track report versions
4. **Acknowledgment** - Confirm receipt
5. **Follow up** - Track remediation

### Data Sensitivity

Consider classification:

| Level | Distribution |
|-------|--------------|
| Public | Anyone |
| Internal | Employees only |
| Confidential | Specific teams |
| Restricted | Named individuals |

## Troubleshooting

### Report Generation Fails

| Issue | Solution |
|-------|----------|
| Timeout | Reduce scope or data range |
| Memory error | Generate in parts |
| Missing data | Verify source scans exist |

### Formatting Issues

| Issue | Solution |
|-------|----------|
| Charts missing | Enable chart generation |
| Layout broken | Use supported template |
| Images not loading | Check image paths |

## Related

- [Dashboard](/features/dashboard)
- [Security Scanning](/features/scans)
- [Compliance](/features/compliance)
