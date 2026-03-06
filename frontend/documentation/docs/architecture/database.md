---
sidebar_position: 4
---

# Database Design

SECUR-EU uses MongoDB for flexible document storage of security scan data.

## Why MongoDB?

- **Flexible Schema**: Scan results vary by scanner type
- **Document Model**: Natural fit for nested security data
- **Query Performance**: Efficient for read-heavy workloads
- **Scalability**: Horizontal scaling when needed

## Collections

### scans

Stores all scan records and results.

```javascript
{
  "_id": ObjectId("..."),
  "id": "scan-001",
  "type": "nmap",              // nmap, zap, nuclei
  "target": "192.168.1.0/24",
  "status": "completed",       // queued, running, completed, failed
  "containerId": "abc123",
  "config": {
    "scanType": "standard",
    "portRange": "1-65535",
    "options": {
      "serviceDetection": true,
      "osDetection": false
    }
  },
  "results": {
    "hosts": [...],
    "vulnerabilities": [...],
    "rawOutput": "..."
  },
  "summary": {
    "totalHosts": 15,
    "totalVulnerabilities": 27,
    "critical": 3,
    "high": 8,
    "medium": 12,
    "low": 4
  },
  "startedAt": ISODate("2024-01-15T10:00:00Z"),
  "completedAt": ISODate("2024-01-15T10:15:00Z"),
  "createdAt": ISODate("2024-01-15T09:59:00Z"),
  "updatedAt": ISODate("2024-01-15T10:15:00Z")
}
```

### hosts

Asset inventory and host information.

```javascript
{
  "_id": ObjectId("..."),
  "id": "host-001",
  "name": "Web Server",
  "address": "192.168.1.10",
  "hostname": "web01.example.com",
  "type": "server",
  "status": "online",
  "tags": ["production", "web", "critical"],
  "properties": {
    "os": "Ubuntu 22.04",
    "openPorts": [22, 80, 443],
    "services": [
      { "port": 80, "service": "Apache/2.4.41" },
      { "port": 443, "service": "Apache/2.4.41" }
    ]
  },
  "vulnerabilities": [
    { "id": "vuln-001", "severity": "high" },
    { "id": "vuln-002", "severity": "medium" }
  ],
  "lastScan": ISODate("2024-01-15T10:00:00Z"),
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-15T10:15:00Z")
}
```

### vulnerabilities

Detailed vulnerability records.

```javascript
{
  "_id": ObjectId("..."),
  "id": "vuln-001",
  "scanId": "scan-001",
  "hostId": "host-001",
  "title": "SQL Injection",
  "severity": "high",
  "cvss": 8.6,
  "cve": "CVE-2024-1234",
  "cwe": "CWE-89",
  "description": "SQL injection vulnerability in login form",
  "evidence": {
    "url": "https://example.com/login",
    "parameter": "username",
    "payload": "' OR '1'='1",
    "response": "..."
  },
  "remediation": "Use parameterized queries",
  "references": [
    "https://owasp.org/www-community/attacks/SQL_Injection",
    "https://nvd.nist.gov/vuln/detail/CVE-2024-1234"
  ],
  "status": "open",          // open, remediated, accepted, false_positive
  "detectedAt": ISODate("2024-01-15T10:00:00Z"),
  "createdAt": ISODate("2024-01-15T10:00:00Z"),
  "updatedAt": ISODate("2024-01-15T10:00:00Z")
}
```

### exploitation

Exploitation test records.

```javascript
{
  "_id": ObjectId("..."),
  "id": "exp-001",
  "module": "exploit/multi/http/apache_mod_cgi_bash_env_exec",
  "target": {
    "host": "192.168.1.100",
    "port": 80,
    "service": "Apache/2.4.41"
  },
  "options": {
    "RHOSTS": "192.168.1.100",
    "RPORT": 80,
    "TARGETURI": "/cgi-bin/test.cgi"
  },
  "payload": "linux/x86/meterpreter/reverse_tcp",
  "payloadOptions": {
    "LHOST": "192.168.1.50",
    "LPORT": 4444
  },
  "status": "success",       // running, success, failed
  "sessions": [
    {
      "id": 1,
      "type": "meterpreter",
      "info": "www-data @ target"
    }
  ],
  "evidence": {
    "logs": ["session_commands.log"],
    "screenshots": []
  },
  "startedAt": ISODate("2024-01-15T10:30:00Z"),
  "completedAt": ISODate("2024-01-15T10:35:00Z")
}
```

### compliance

Compliance assessment records.

```javascript
{
  "_id": ObjectId("..."),
  "id": "cra-001",
  "framework": "cra",
  "targetId": "product-001",
  "status": "completed",
  "score": 72,
  "results": {
    "pass": 18,
    "fail": 5,
    "warning": 3,
    "notApplicable": 2
  },
  "requirements": [
    {
      "id": "CRA-VM-001",
      "title": "Vulnerability Identification",
      "status": "pass",
      "evidence": ["..."],
      "recommendations": []
    }
  ],
  "completedAt": ISODate("2024-01-15T10:30:00Z")
}
```

### stix_objects

STIX threat intelligence objects.

```javascript
{
  "_id": ObjectId("..."),
  "stixId": "indicator--8e2e2d2b-17d4-4cbf-938f-98ee46b3cd3f",
  "type": "indicator",
  "name": "Malicious IP",
  "description": "Known C2 server",
  "pattern": "[ipv4-addr:value = '198.51.100.1']",
  "patternType": "stix",
  "validFrom": ISODate("2024-01-01T00:00:00Z"),
  "validUntil": ISODate("2024-12-31T23:59:59Z"),
  "createdBy": "identity--...",
  "labels": ["malicious-activity", "c2"],
  "importedAt": ISODate("2024-01-15T00:00:00Z")
}
```

## Indexes

### Recommended Indexes

```javascript
// scans collection
db.scans.createIndex({ "status": 1 })
db.scans.createIndex({ "type": 1 })
db.scans.createIndex({ "startedAt": -1 })
db.scans.createIndex({ "target": 1 })

// hosts collection
db.hosts.createIndex({ "address": 1 }, { unique: true })
db.hosts.createIndex({ "tags": 1 })
db.hosts.createIndex({ "status": 1 })

// vulnerabilities collection
db.vulnerabilities.createIndex({ "scanId": 1 })
db.vulnerabilities.createIndex({ "hostId": 1 })
db.vulnerabilities.createIndex({ "severity": 1 })
db.vulnerabilities.createIndex({ "cve": 1 })
db.vulnerabilities.createIndex({ "status": 1 })

// stix_objects collection
db.stix_objects.createIndex({ "stixId": 1 }, { unique: true })
db.stix_objects.createIndex({ "type": 1 })
db.stix_objects.createIndex({ "pattern": "text" })
```

## Common Queries

### Get Recent Scans

```javascript
db.scans.find({})
  .sort({ startedAt: -1 })
  .limit(10)
```

### Get Vulnerabilities by Severity

```javascript
db.vulnerabilities.find({
  severity: { $in: ["critical", "high"] },
  status: "open"
})
```

### Aggregate Dashboard Stats

```javascript
db.vulnerabilities.aggregate([
  { $match: { status: "open" } },
  { $group: {
    _id: "$severity",
    count: { $sum: 1 }
  }}
])
```

### Search STIX Indicators

```javascript
db.stix_objects.find({
  type: "indicator",
  pattern: { $regex: "192.168" }
})
```

## Data Retention

### Archival Strategy

```javascript
// Archive scans older than 90 days
db.scans.find({
  completedAt: { $lt: new Date(Date.now() - 90*24*60*60*1000) }
}).forEach(doc => {
  db.scans_archive.insertOne(doc);
  db.scans.deleteOne({ _id: doc._id });
});
```

### Cleanup Old Data

```javascript
// Remove failed scans older than 30 days
db.scans.deleteMany({
  status: "failed",
  startedAt: { $lt: new Date(Date.now() - 30*24*60*60*1000) }
});
```

## Backup and Recovery

### Backup

```bash
# Full backup
mongodump --uri="mongodb://localhost:27017/secureu" --out=/backup/$(date +%Y%m%d)

# Specific collection
mongodump --uri="mongodb://localhost:27017/secureu" --collection=scans --out=/backup/scans
```

### Restore

```bash
# Full restore
mongorestore --uri="mongodb://localhost:27017/secureu" /backup/20240115

# Specific collection
mongorestore --uri="mongodb://localhost:27017/secureu" --collection=scans /backup/scans/secureu/scans.bson
```

## Related

- [Architecture Overview](/architecture/overview)
- [Backend Architecture](/architecture/backend)
- [API Endpoints](/api/endpoints)
