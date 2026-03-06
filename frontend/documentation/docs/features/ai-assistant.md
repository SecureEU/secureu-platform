---
sidebar_position: 4
---

# AI Assistant

The SECUR-EU AI Assistant provides intelligent security analysis and recommendations powered by Ollama and large language models.

## Overview

The AI Assistant helps with:

- Vulnerability analysis and prioritization
- Remediation recommendations
- Security report generation
- Threat intelligence interpretation
- Natural language querying of scan data

## Getting Started

### Prerequisites

1. **Ollama** installed and running
2. **Language model** pulled (e.g., llama3)

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull a model
ollama pull llama3

# Verify it's running
curl http://localhost:11434/api/tags
```

### Configuration

Configure the AI Assistant in your environment:

```bash
# .env
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3
```

## Using the Assistant

### Chat Interface

Navigate to **AI Assistant** in the sidebar to access the chat interface.

### Example Prompts

**Vulnerability Analysis:**
```
Analyze the latest scan results and identify the highest priority issues.
```

**Remediation Guidance:**
```
How do I fix CVE-2024-1234 on my Apache server?
```

**Report Generation:**
```
Generate an executive summary of our current security posture.
```

**Threat Intelligence:**
```
Explain the implications of this STIX indicator for our infrastructure.
```

## API Integration

### Chat Endpoint

```bash
POST /ai/chat
Content-Type: application/json

{
  "message": "What are the most critical vulnerabilities in my latest scan?",
  "context": {
    "scanId": "scan-12345",
    "includeHistory": true
  }
}
```

### Response

```json
{
  "response": "Based on your latest scan (scan-12345), I've identified 3 critical vulnerabilities that require immediate attention:\n\n1. **CVE-2024-1234** - SQL Injection in login form...",
  "sources": [
    {
      "type": "scan",
      "id": "scan-12345",
      "reference": "Finding #1"
    }
  ],
  "suggestions": [
    "Would you like remediation steps for any of these?",
    "Should I generate a detailed report?"
  ]
}
```

### Vulnerability Analysis

```bash
POST /ai/analyze
Content-Type: application/json

{
  "vulnerabilityId": "vuln-001",
  "analysisType": "full"
}
```

### Report Generation

```bash
POST /ai/report
Content-Type: application/json

{
  "type": "executive",
  "scanIds": ["scan-001", "scan-002"],
  "format": "markdown"
}
```

## Capabilities

### Vulnerability Prioritization

The AI analyzes vulnerabilities considering:

| Factor | Weight | Description |
|--------|--------|-------------|
| CVSS Score | High | Base severity rating |
| Exploitability | High | Known exploits available |
| Asset Criticality | Medium | Business impact of affected system |
| Network Exposure | Medium | Internal vs. internet-facing |
| Data Sensitivity | Medium | Type of data at risk |

### Remediation Recommendations

For each vulnerability, the AI provides:

1. **Step-by-step fix instructions**
2. **Alternative workarounds** if patching isn't immediate
3. **Verification steps** to confirm the fix
4. **Related vulnerabilities** that may be addressed together

### Natural Language Queries

Ask questions in plain English:

- "Which servers have critical vulnerabilities?"
- "Show me all SQL injection findings"
- "What changed since last week's scan?"
- "Are there any new CVEs affecting our infrastructure?"

## Supported Models

| Model | Size | Speed | Quality |
|-------|------|-------|---------|
| llama3 | 4.7GB | Medium | High |
| llama3:70b | 40GB | Slow | Very High |
| mistral | 4.1GB | Fast | Good |
| codellama | 3.8GB | Fast | Good (code focus) |

### Changing Models

```bash
# Pull a different model
ollama pull mistral

# Update configuration
OLLAMA_MODEL=mistral
```

## Context Management

### Conversation History

The AI maintains conversation context:

```javascript
// Context is preserved across messages
const conversation = [
  { role: "user", content: "What vulnerabilities were found?" },
  { role: "assistant", content: "I found 5 critical vulnerabilities..." },
  { role: "user", content: "Tell me more about the first one" }
  // AI understands "first one" from context
];
```

### Scan Context

Include scan data for analysis:

```json
{
  "message": "Analyze this scan",
  "context": {
    "scanId": "scan-12345",
    "includeScanData": true,
    "maxFindings": 50
  }
}
```

## Best Practices

### Effective Prompts

**Good:**
```
Analyze the SQL injection vulnerability (ID: vuln-001) and provide
specific remediation steps for our Django application.
```

**Less Effective:**
```
Fix my security issues.
```

### Tips for Better Results

1. **Be specific** - Include IDs, CVEs, or specific details
2. **Provide context** - Mention your tech stack
3. **Ask follow-ups** - Drill down into recommendations
4. **Verify suggestions** - AI provides guidance, human review is essential

## Limitations

- AI recommendations should be verified by security professionals
- Real-time threat intelligence may have delays
- Complex exploitation scenarios require human expertise
- Model responses depend on training data cutoff

## Privacy & Security

- Conversations are processed locally via Ollama
- No data sent to external services
- Scan data stays within your infrastructure
- Conversation history can be cleared

## Related

- [Security Scanning](/features/scans)
- [STIX Integration](/features/stix-integration)
- [Generating Reports](/user-guide/generating-reports)
