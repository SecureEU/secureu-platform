---
sidebar_position: 5
---

# Compliance Management

SECUR-EU provides compliance checking and reporting capabilities to help organizations meet regulatory requirements.

## Overview

The compliance module supports:

- Cyber Resilience Act (CRA) assessment
- Automated compliance checking
- Gap analysis reporting
- Remediation tracking
- Audit documentation

## Supported Frameworks

### Cyber Resilience Act (CRA)

The EU Cyber Resilience Act sets cybersecurity requirements for products with digital elements.

**Key Requirements:**
- Vulnerability handling processes
- Security update mechanisms
- Secure by default configuration
- Documentation and transparency

### Additional Frameworks (Coming Soon)

- NIST Cybersecurity Framework
- ISO 27001
- SOC 2
- PCI DSS
- GDPR Technical Controls

## Running Assessments

### CRA Assessment

```bash
POST /compliance/cra/assess
Content-Type: application/json

{
  "targetId": "product-001",
  "scope": {
    "vulnerabilityManagement": true,
    "secureDefault": true,
    "updateMechanism": true,
    "documentation": true
  }
}
```

### Response

```json
{
  "assessmentId": "cra-assess-001",
  "status": "completed",
  "score": 72,
  "results": {
    "pass": 18,
    "fail": 5,
    "warning": 3,
    "notApplicable": 2
  },
  "completedAt": "2024-01-15T10:30:00Z"
}
```

## Assessment Results

### Result Structure

Each assessment produces detailed results:

```json
{
  "requirement": "CRA-VM-001",
  "title": "Vulnerability Identification",
  "description": "Products shall identify and document vulnerabilities",
  "status": "pass",
  "evidence": [
    "Scan results show vulnerability tracking",
    "CVE correlation enabled"
  ],
  "recommendations": []
}
```

### Status Types

| Status | Icon | Description |
|--------|------|-------------|
| Pass | ✅ | Requirement fully met |
| Fail | ❌ | Requirement not met |
| Warning | ⚠️ | Partial compliance |
| N/A | ➖ | Not applicable |

## Compliance Dashboard

### Overview Metrics

The compliance dashboard shows:

- **Overall Score**: Percentage of requirements met
- **By Category**: Breakdown per compliance area
- **Trend**: Score changes over time
- **Open Items**: Requirements needing attention

### Category Breakdown

```
Vulnerability Management  ████████████████░░░░ 80%
Secure Configuration      ██████████████░░░░░░ 70%
Update Mechanism          ████████████████████ 100%
Documentation             ████████████░░░░░░░░ 60%
```

## Gap Analysis

### Identifying Gaps

The gap analysis report identifies:

1. **Missing controls** - Requirements with no implementation
2. **Partial implementations** - Controls needing improvement
3. **Evidence gaps** - Lacking documentation

### Gap Report

```bash
GET /compliance/cra/gaps/assess-001

# Response
{
  "gaps": [
    {
      "requirement": "CRA-SD-003",
      "title": "Secure Default Passwords",
      "severity": "high",
      "currentState": "Default credentials in use",
      "requiredState": "Unique credentials per device",
      "remediation": "Implement unique password generation"
    }
  ]
}
```

## Remediation Tracking

### Creating Remediation Tasks

```bash
POST /compliance/remediation
Content-Type: application/json

{
  "gapId": "gap-001",
  "assignee": "security-team",
  "priority": "high",
  "dueDate": "2024-02-15"
}
```

### Tracking Progress

```json
{
  "remediationId": "rem-001",
  "status": "in_progress",
  "progress": 60,
  "activities": [
    {
      "date": "2024-01-20",
      "action": "Password policy updated",
      "by": "admin"
    }
  ]
}
```

## Reporting

### Compliance Report

Generate compliance documentation:

```bash
GET /compliance/report/assess-001?format=pdf

# Returns PDF report with:
# - Executive summary
# - Detailed findings
# - Evidence documentation
# - Remediation roadmap
```

### Report Types

| Type | Audience | Content |
|------|----------|---------|
| Executive | Management | High-level summary, scores, trends |
| Technical | IT Team | Detailed findings, remediation steps |
| Audit | Auditors | Evidence, control mappings |

## Integration with Scans

### Automated Evidence

Scan results automatically provide compliance evidence:

```json
{
  "requirement": "CRA-VM-002",
  "title": "Vulnerability Assessment",
  "evidence": {
    "source": "scan",
    "scanId": "scan-12345",
    "findings": "Regular vulnerability scanning performed",
    "lastScan": "2024-01-14T08:00:00Z"
  }
}
```

### Continuous Compliance

Set up continuous compliance monitoring:

```json
{
  "schedule": {
    "frequency": "weekly",
    "assessments": ["cra"],
    "notifications": {
      "onFailure": true,
      "recipients": ["security@company.com"]
    }
  }
}
```

## CRA Requirement Categories

### Vulnerability Management

| ID | Requirement |
|----|-------------|
| CRA-VM-001 | Vulnerability identification |
| CRA-VM-002 | Vulnerability assessment |
| CRA-VM-003 | Vulnerability remediation |
| CRA-VM-004 | Disclosure process |

### Secure by Default

| ID | Requirement |
|----|-------------|
| CRA-SD-001 | Minimal attack surface |
| CRA-SD-002 | Secure configuration |
| CRA-SD-003 | No default passwords |
| CRA-SD-004 | Data protection |

### Update Mechanism

| ID | Requirement |
|----|-------------|
| CRA-UM-001 | Security update capability |
| CRA-UM-002 | Timely updates |
| CRA-UM-003 | Update integrity |
| CRA-UM-004 | Rollback capability |

## Best Practices

### Effective Compliance

1. **Regular assessments** - Run weekly or monthly
2. **Track trends** - Monitor score changes
3. **Prioritize gaps** - Address high-severity first
4. **Document everything** - Maintain evidence
5. **Automate where possible** - Use scan integration

### Audit Preparation

1. Generate comprehensive reports
2. Ensure evidence is current
3. Document remediation activities
4. Prepare control narratives
5. Review with stakeholders

## Related

- [Security Scanning](/features/scans)
- [Dashboard](/features/dashboard)
- [Generating Reports](/user-guide/generating-reports)
