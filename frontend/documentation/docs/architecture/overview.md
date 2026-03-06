---
sidebar_position: 1
---

# Architecture Overview

SECUR-EU follows a modern microservices-inspired architecture with clear separation of concerns.

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Client Layer                                │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │   Web Browser   │  │   API Clients   │  │   CLI Tools             │  │
│  └────────┬────────┘  └────────┬────────┘  └───────────┬─────────────┘  │
│           │                    │                       │                 │
└───────────┼────────────────────┼───────────────────────┼─────────────────┘
            │                    │                       │
            ▼                    ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Frontend (Next.js)                            │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  React Components │ State Management │ API Client │ UI Components│   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
└────────────────────────────────────┼─────────────────────────────────────┘
                                     │ HTTP/REST
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Backend (Go + Echo)                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  API Routes │ Business Logic │ Scanner Integration │ AI Service  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                │              │              │              │            │
└────────────────┼──────────────┼──────────────┼──────────────┼────────────┘
                 │              │              │              │
        ┌────────┘      ┌───────┘      ┌───────┘      ┌───────┘
        ▼               ▼              ▼              ▼
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────────┐
│    MongoDB    │ │  Docker   │ │ Metasploit│ │    Ollama     │
│   Database    │ │Containers │ │ Framework │ │   AI Models   │
└───────────────┘ └───────────┘ └───────────┘ └───────────────┘
```

## Component Overview

### Frontend

**Technology:** Next.js 15, React 19, Tailwind CSS 4

- Single-page application
- Server-side rendering capable
- Real-time updates via polling
- Responsive design

### Backend

**Technology:** Go 1.21+, Echo Framework

- RESTful API
- Container orchestration
- Scanner integration
- AI service proxy

### Database

**Technology:** MongoDB 6.0

- Document storage
- Scan results
- Asset inventory
- Configuration

### Container Runtime

**Technology:** Docker

- Scanner containers (Nmap, ZAP, Nuclei)
- Isolated execution
- Resource management

### AI Service

**Technology:** Ollama

- Local LLM inference
- Privacy-preserving
- Multiple model support

## Data Flow

### Scan Execution Flow

```
┌──────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ User │───►│Frontend │───►│ Backend │───►│ Docker  │───►│ Scanner │
└──────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
                                │                             │
                                │         Results             │
                                ◄─────────────────────────────┘
                                │
                                ▼
                          ┌─────────┐
                          │ MongoDB │
                          └─────────┘
```

### Request Flow

1. User initiates action in frontend
2. Frontend sends API request
3. Backend validates and processes
4. External service called if needed
5. Response stored in MongoDB
6. Result returned to frontend

## Key Design Decisions

### Containerized Scanners

**Why:** Isolation, consistency, resource control

- Each scan runs in dedicated container
- Prevents tool conflicts
- Easy to update scanner versions
- Resource limits enforced

### Local AI Processing

**Why:** Privacy, no external dependencies

- Scan data stays local
- No cloud API costs
- Works offline
- Model flexibility

### Document Database

**Why:** Flexible schema, natural fit for scan data

- Scan results vary by scanner
- Easy to add new fields
- Good query performance
- Native JSON support

## Scalability Considerations

### Current Limitations

- Single backend instance
- Sequential container monitoring
- No horizontal scaling
- Memory-bound AI operations

### Future Improvements

- Worker pool for scans
- Message queue for jobs
- Horizontal scaling
- Distributed AI inference

## Security Model

### Network Segmentation

```
┌─────────────────────────────────────────┐
│            Trusted Network              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  │
│  │Frontend │  │ Backend │  │ Database│  │
│  └─────────┘  └─────────┘  └─────────┘  │
└─────────────────────────────────────────┘
                    │
                    ▼ Scans
┌─────────────────────────────────────────┐
│          Target Network (Scoped)        │
└─────────────────────────────────────────┘
```

### Access Control

- Frontend: User-facing
- Backend: API access
- Database: Internal only
- Scanners: Controlled execution

## Technology Stack Summary

| Layer | Technology | Purpose |
|-------|------------|---------|
| Frontend | Next.js 15 | UI framework |
| Styling | Tailwind CSS 4 | CSS utilities |
| Backend | Go + Echo | API server |
| Database | MongoDB | Data storage |
| Containers | Docker | Scan isolation |
| AI | Ollama | Local inference |
| Scanners | Nmap, ZAP, Nuclei | Security tools |

## Related

- [Backend Architecture](/architecture/backend)
- [Frontend Architecture](/architecture/frontend)
- [Database Design](/architecture/database)
