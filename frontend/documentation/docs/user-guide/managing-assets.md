---
sidebar_position: 2
---

# Managing Assets

Learn how to manage hosts and assets in SECUR-EU for organized security monitoring.

## Asset Overview

Assets in SECUR-EU represent:

- Network hosts (servers, workstations)
- Web applications
- Network devices
- Cloud resources

## Adding Assets

### Manual Addition

1. Navigate to **Assets** in the sidebar
2. Click **Add Asset**
3. Fill in asset details:

| Field | Required | Description |
|-------|----------|-------------|
| Name | Yes | Friendly name for the asset |
| IP/Hostname | Yes | Network address |
| Type | Yes | Server, Workstation, Network Device, etc. |
| Tags | No | Labels for organization |
| Description | No | Additional notes |

4. Click **Save Asset**

### Bulk Import

Import multiple assets via CSV:

```csv
name,address,type,tags,description
Web Server,192.168.1.10,server,production;web,Main web server
Database,192.168.1.20,server,production;database,PostgreSQL database
```

1. Navigate to **Assets**
2. Click **Import**
3. Upload CSV file
4. Review and confirm

### Auto-Discovery

Assets discovered during scans can be added automatically:

1. Run a network scan
2. View discovered hosts
3. Click **Add to Assets** on desired hosts

## Asset Properties

### Basic Information

| Property | Description |
|----------|-------------|
| Name | Display name |
| Address | IP or hostname |
| Type | Asset category |
| Status | Online, Offline, Unknown |
| Last Seen | Most recent activity |

### Extended Properties

| Property | Description |
|----------|-------------|
| Operating System | Detected OS |
| Open Ports | Discovered services |
| Vulnerabilities | Associated findings |
| Tags | Organization labels |
| Notes | Free-form documentation |

## Organizing Assets

### Using Tags

Tags help categorize assets:

**Environment Tags**
- `production`
- `staging`
- `development`
- `testing`

**Function Tags**
- `web-server`
- `database`
- `api`
- `load-balancer`

**Criticality Tags**
- `critical`
- `high-priority`
- `standard`

### Creating Tag Groups

```json
{
  "group": "Environment",
  "tags": ["production", "staging", "development"],
  "color": "#3b82f6",
  "exclusive": true
}
```

### Filtering by Tags

```
# View production servers
tag:production AND type:server

# View critical web assets
tag:critical AND tag:web-server

# Exclude development
NOT tag:development
```

## Asset Groups

### Creating Groups

Organize related assets:

1. Navigate to **Assets** → **Groups**
2. Click **New Group**
3. Configure:

| Field | Description |
|-------|-------------|
| Name | Group name |
| Description | Group purpose |
| Members | Selected assets |
| Auto-membership | Tag-based rules |

### Dynamic Groups

Auto-populate groups based on rules:

```json
{
  "name": "Production Servers",
  "rules": {
    "all": [
      {"tag": "production"},
      {"type": "server"}
    ]
  }
}
```

## Asset Details View

### Overview Tab

- Basic asset information
- Current status
- Quick stats (vulnerabilities, scans)

### Vulnerabilities Tab

- List of findings associated with this asset
- Severity breakdown
- Remediation status

### Scan History Tab

- Previous scans targeting this asset
- Trend data
- Comparison view

### Activity Tab

- Recent changes
- Scan events
- Status changes

## Monitoring Assets

### Health Status

| Status | Meaning |
|--------|---------|
| 🟢 Online | Asset reachable |
| 🔴 Offline | Asset unreachable |
| 🟡 Degraded | Partial availability |
| ⚪ Unknown | Not recently scanned |

### Alerts

Configure alerts for assets:

```json
{
  "assetId": "asset-001",
  "alerts": {
    "newCriticalVuln": true,
    "statusChange": true,
    "newHighVuln": true
  },
  "recipients": ["security@example.com"]
}
```

## Asset Relationships

### Dependencies

Map asset relationships:

```
┌─────────────┐
│ Load Balancer│
└──────┬──────┘
       │
   ┌───┴───┐
   │       │
┌──▼──┐ ┌──▼──┐
│Web 1│ │Web 2│
└──┬──┘ └──┬──┘
   │       │
   └───┬───┘
       │
   ┌───▼───┐
   │Database│
   └────────┘
```

### Mapping Dependencies

1. Open asset details
2. Click **Dependencies**
3. Add upstream/downstream relationships
4. Save changes

## API Operations

### List Assets

```bash
GET /hosts

# Response
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

### Create Asset

```bash
POST /hosts
Content-Type: application/json

{
  "name": "New Server",
  "address": "192.168.1.50",
  "type": "server",
  "tags": ["staging"]
}
```

### Update Asset

```bash
PUT /hosts/host-001
Content-Type: application/json

{
  "tags": ["production", "web", "critical"]
}
```

### Delete Asset

```bash
DELETE /hosts/host-001
```

## Best Practices

### Asset Inventory

1. **Keep updated** - Remove decommissioned assets
2. **Use consistent naming** - Follow naming conventions
3. **Tag everything** - Enable easy filtering
4. **Document dependencies** - Map relationships
5. **Review regularly** - Audit asset list

### Security Considerations

1. **Critical assets first** - Prioritize high-value targets
2. **Complete coverage** - Ensure all assets are tracked
3. **Accurate data** - Keep information current
4. **Access control** - Limit who can modify assets

## Related

- [Running Scans](/user-guide/running-scans)
- [Dashboard](/features/dashboard)
- [Generating Reports](/user-guide/generating-reports)
