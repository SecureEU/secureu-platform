---
slug: /
sidebar_position: 1
---

# SECUR-EU Documentation

Welcome to the **SECUR-EU** (SME Security Platform) platform documentation. This comprehensive guide will help you understand and effectively use all features of the security operations platform.

## What is SECUR-EU?

SECUR-EU is an advanced security operations platform that provides:

- **Vulnerability Scanning** - Automated network and web application security scanning
- **Exploitation Testing** - Controlled offensive security testing with Metasploit integration
- **AI-Powered Analysis** - Intelligent threat analysis and recommendations
- **Compliance Management** - Regulatory compliance checking and reporting
- **STIX Integration** - Threat intelligence sharing and analysis

## Platform Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        SECUR-EU Platform                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │Dashboard │  │  Scans   │  │Exploits  │  │  AI Assistant    │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────────┬─────────┘ │
│       │             │             │                  │           │
│       └─────────────┴─────────────┴──────────────────┘           │
│                              │                                    │
│                    ┌─────────▼─────────┐                         │
│                    │   Backend API      │                         │
│                    │   (Go + Echo)      │                         │
│                    └─────────┬─────────┘                         │
│                              │                                    │
│       ┌──────────────────────┼──────────────────────┐            │
│       │                      │                      │            │
│  ┌────▼────┐           ┌─────▼─────┐          ┌────▼────┐       │
│  │ MongoDB │           │  Docker   │          │ Ollama  │       │
│  │   DB    │           │ Containers│          │   AI    │       │
│  └─────────┘           └───────────┘          └─────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

## Key Features

### 🔍 Security Scanning

Run comprehensive security scans using industry-standard tools:

- **Nmap** - Network discovery and security auditing
- **OWASP ZAP** - Web application vulnerability scanner
- **Nuclei** - Fast vulnerability scanner with templates

### 🎯 Exploitation Testing

Safely test exploits in controlled environments:

- **Metasploit Integration** - Full Metasploit Framework support
- **Controlled Testing** - Isolated container-based testing
- **Detailed Reporting** - Comprehensive exploitation reports

### 🤖 AI Assistant

Get intelligent security insights:

- **Vulnerability Analysis** - AI-powered vulnerability assessment
- **Remediation Guidance** - Automated fix recommendations
- **Threat Intelligence** - Smart threat correlation

### ✅ Compliance

Ensure regulatory compliance:

- **CRA Compliance** - Cyber Resilience Act checking
- **Automated Reports** - Generate compliance documentation
- **Gap Analysis** - Identify compliance gaps

## Quick Navigation

| Section | Description |
|---------|-------------|
| [Getting Started](/getting-started/installation) | Installation and setup guide |
| [Features](/features/dashboard) | Detailed feature documentation |
| [User Guide](/user-guide/running-scans) | Step-by-step tutorials |
| [API Reference](/api/overview) | Backend API documentation |
| [Architecture](/architecture/overview) | System architecture details |

## System Requirements

- **Node.js** 18.0 or higher
- **Go** 1.21 or higher
- **Docker** with Docker Compose
- **MongoDB** 6.0 or higher
- **4GB RAM** minimum (8GB recommended)

## Getting Help

If you need assistance:

1. Check the [FAQ](/faq) for common questions
2. Review the [Troubleshooting](/troubleshooting) guide
3. Open an issue on [GitHub](https://github.com/secur-eu)

---

Ready to get started? Head to the [Installation Guide](/getting-started/installation).
