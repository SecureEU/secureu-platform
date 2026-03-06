---
sidebar_position: 6
---

# STIX Integration

SECUR-EU supports STIX (Structured Threat Information Expression) for threat intelligence sharing and analysis.

## Overview

STIX integration enables:

- Import threat intelligence feeds
- Correlate vulnerabilities with threat data
- Export findings in STIX format
- Threat actor tracking
- Indicator of Compromise (IoC) management

## What is STIX?

STIX is a standardized language for describing cyber threat information:

```
┌─────────────────────────────────────────────────────────┐
│                    STIX 2.1 Objects                      │
├─────────────────────────────────────────────────────────┤
│  Attack Pattern   │  Campaign         │  Course of Action│
│  Identity         │  Indicator        │  Intrusion Set   │
│  Malware          │  Observed Data    │  Report          │
│  Threat Actor     │  Tool             │  Vulnerability   │
└─────────────────────────────────────────────────────────┘
```

## Importing Threat Intelligence

### STIX Bundle Import

```bash
POST /stix/import
Content-Type: application/json

{
  "bundle": {
    "type": "bundle",
    "id": "bundle--example",
    "objects": [
      {
        "type": "indicator",
        "id": "indicator--example",
        "pattern": "[ipv4-addr:value = '192.168.1.100']",
        "pattern_type": "stix",
        "valid_from": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

### Feed Integration

Connect to threat intelligence feeds:

```bash
POST /stix/feeds
Content-Type: application/json

{
  "name": "AlienVault OTX",
  "url": "https://otx.alienvault.com/api/v1/pulses/subscribed",
  "type": "taxii",
  "schedule": "hourly",
  "apiKey": "your-api-key"
}
```

## STIX Objects

### Indicators

Patterns for detecting malicious activity:

```json
{
  "type": "indicator",
  "id": "indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f",
  "name": "Malicious IP",
  "description": "Known C2 server",
  "pattern": "[ipv4-addr:value = '198.51.100.1']",
  "pattern_type": "stix",
  "valid_from": "2024-01-01T00:00:00Z",
  "kill_chain_phases": [
    {
      "kill_chain_name": "lockheed-martin-cyber-kill-chain",
      "phase_name": "command-and-control"
    }
  ]
}
```

### Threat Actors

```json
{
  "type": "threat-actor",
  "id": "threat-actor--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f",
  "name": "APT28",
  "description": "Russian threat actor",
  "threat_actor_types": ["nation-state"],
  "aliases": ["Fancy Bear", "Sofacy"],
  "primary_motivation": "espionage"
}
```

### Attack Patterns

```json
{
  "type": "attack-pattern",
  "id": "attack-pattern--example",
  "name": "Spearphishing Attachment",
  "description": "Adversaries send emails with malicious attachments",
  "external_references": [
    {
      "source_name": "mitre-attack",
      "external_id": "T1566.001"
    }
  ]
}
```

### Vulnerabilities

```json
{
  "type": "vulnerability",
  "id": "vulnerability--example",
  "name": "CVE-2024-1234",
  "description": "Remote code execution vulnerability",
  "external_references": [
    {
      "source_name": "cve",
      "external_id": "CVE-2024-1234"
    }
  ]
}
```

## Correlation

### Scan-to-Threat Correlation

Correlate scan findings with threat intelligence:

```bash
POST /stix/correlate
Content-Type: application/json

{
  "scanId": "scan-12345",
  "correlationTypes": ["indicators", "vulnerabilities", "attack-patterns"]
}
```

### Response

```json
{
  "correlations": [
    {
      "finding": {
        "id": "finding-001",
        "type": "vulnerability",
        "cve": "CVE-2024-1234"
      },
      "threatIntel": {
        "type": "attack-pattern",
        "id": "attack-pattern--example",
        "name": "Exploit Public-Facing Application",
        "threatActors": ["APT28", "APT29"],
        "confidence": "high"
      }
    }
  ]
}
```

### IoC Matching

Match network indicators against scan data:

```bash
GET /stix/match?scanId=scan-12345

# Response
{
  "matches": [
    {
      "indicator": "indicator--abc123",
      "pattern": "[ipv4-addr:value = '198.51.100.1']",
      "matchedIn": "finding-007",
      "severity": "critical",
      "description": "Known C2 infrastructure detected"
    }
  ]
}
```

## Exporting STIX

### Export Scan Results

```bash
GET /stix/export/scan/scan-12345

# Returns STIX bundle
{
  "type": "bundle",
  "id": "bundle--scan-12345",
  "objects": [
    {
      "type": "vulnerability",
      "id": "vulnerability--...",
      "name": "CVE-2024-1234"
    },
    {
      "type": "relationship",
      "source_ref": "vulnerability--...",
      "target_ref": "identity--target-system"
    }
  ]
}
```

### Export Options

```bash
GET /stix/export/scan/scan-12345?options=full

# Options:
# - minimal: Core objects only
# - standard: Objects + relationships
# - full: Include sightings, opinions, notes
```

## TAXII Integration

### TAXII Server

SECUR-EU can act as a TAXII server:

```bash
# Discovery endpoint
GET /taxii2/

# API Root
GET /taxii2/api/

# Collections
GET /taxii2/api/collections/

# Get objects
GET /taxii2/api/collections/{id}/objects/
```

### TAXII Client

Fetch from TAXII servers:

```bash
POST /stix/taxii/fetch
Content-Type: application/json

{
  "serverUrl": "https://taxii.example.com/taxii2/",
  "collectionId": "collection-123",
  "apiKey": "your-api-key"
}
```

## Visualization

### Threat Graph

View relationships between STIX objects:

```
                    ┌─────────────┐
                    │ Threat Actor│
                    │   APT28     │
                    └──────┬──────┘
                           │ uses
                    ┌──────▼──────┐
                    │Attack Pattern│
                    │ Spearphishing│
                    └──────┬──────┘
                           │ targets
                    ┌──────▼──────┐
                    │Vulnerability │
                    │CVE-2024-1234 │
                    └──────┬──────┘
                           │ affects
                    ┌──────▼──────┐
                    │   System    │
                    │ web-server  │
                    └─────────────┘
```

### Timeline View

View threat activity over time:

```bash
GET /stix/timeline?objectId=threat-actor--apt28

# Returns chronological activity
```

## API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/stix/import` | POST | Import STIX bundle |
| `/stix/export/{type}/{id}` | GET | Export as STIX |
| `/stix/correlate` | POST | Correlate with threat intel |
| `/stix/match` | GET | Match IoCs |
| `/stix/feeds` | GET/POST | Manage feeds |
| `/stix/objects` | GET | List STIX objects |

### Query Parameters

```bash
# Filter objects
GET /stix/objects?type=indicator&created_after=2024-01-01

# Search patterns
GET /stix/search?pattern=192.168.1.
```

## Best Practices

### Effective Threat Intelligence

1. **Multiple sources** - Use diverse intelligence feeds
2. **Regular updates** - Schedule feed refreshes
3. **Validate data** - Review imported intelligence
4. **Correlate findings** - Link scans to threat data
5. **Share responsibly** - Export with appropriate TLP markings

### Data Quality

- Deduplicate imported objects
- Validate STIX syntax
- Maintain object versioning
- Archive historical data

## Related

- [Security Scanning](/features/scans)
- [AI Assistant](/features/ai-assistant)
- [Dashboard](/features/dashboard)
