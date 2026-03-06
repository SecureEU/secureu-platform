from fastapi import FastAPI, HTTPException, Query, Header, Depends, Request
from fastapi.responses import JSONResponse
from typing import Optional, List
from datetime import datetime, timedelta
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from fastapi.middleware.cors import CORSMiddleware
import os

from database import db
from drain_analysis import drain_analyzer
from raw_log_reader import raw_log_reader

app = FastAPI(
    title = "Red Flags API",
    description = "REST API for querying security incidents detected.",
    version = "1.0.0",
    docs_url = "/docs",
    redoc_url = "/redoc"
)

origins = [
    "http://localhost",
    "http://127.0.0.1",
    "http://localhost:3000",
    "http://localhost:7274",
    "http://192.168.6.123:7274",
    "https://redflags.iee.ihu.gr",
    "https://api.redflags.iee.ihu.gr",
    "https://ventricular-ariah-burly.ngrok-free.dev",
]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,   
    allow_credentials=True,
    allow_methods=["*"],     
    allow_headers=["*"],
)

# Limit request rate
limiter = Limiter(key_func = get_remote_address)
app.state.Limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# API key authentication
#API_KEY = os.getenv("API_KEY", None)

#def verify_api_key(x_api_key):
#    if API_KEY and x_api_key != API_KEY:
#        raise HTTPException(status_code = 401, detail = "Invalid or missing API key")
#    return True

@app.get("/")
@limiter.limit("100/minute")
async def root(request):
    return {
        "service": "Red Flags API",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs",
        "endpoints": {
            "incidents": "/incidents",
            "statistics": "/statistics"
        }
    }

@app.get("/raw-logs/recent")
@limiter.limit("100/minute")
async def get_recent_raw_logs(
    request: Request,
    n: int = Query(100, ge=1, le=1000, description="Number of recent logs to return (max 1000)"),
    log_type: Optional[str] = Query(None, description="Filter by log type (e.g. system logs, web logs)"),
    source_host: Optional[str] = Query(None, description="Filter by source host")
):
    try:
        result = raw_log_reader.get_recent_logs(
            n = n,
            log_type = log_type,
            source_host = source_host
        )

        result["filters"] = {
            "n": n,
            "log_type": log_type,
            "source_host": source_host
        }

        return result

    except Exception as e:
        raise HTTPException(
            status_code = 500,
            detail = f"Failed to read raw logs: {str(e)}"
        )
    
@app.get("/raw-logs/stats")
@limiter.limit("100/minute")
async def get_raw_log_stats(request: Request):
    try:
        stats = raw_log_reader.get_statistics()
        return stats
    except Exception as e:
        raise HTTPException(
            status_code = 500,
            detail = f"Failed to get log statistics: {str(e)}"
        )


@app.get("/incidents")
@limiter.limit("100/minute")
async def list_incidents(
    request: Request,
    limit: int = Query(20, ge=1, le=100, description="Number of results (max 100)"),
    offset: int = Query(0, ge=0, description = "Skip N results (pagination)"),
    severity: Optional[str] = Query(None, description = "Filter by severity (CRITICAL, HIGH, MEDIUM, LOW, INFO)"),
    log_type: Optional[str] = Query(None, description="Filter by log type (system, web, application)"),
    source_host: Optional[str] = Query(None, description="Filter by source host"),
    hours: Optional[int] = Query(None, description="Last N hours (e.g., 24)")
    ):
    try:
        # Date range if hours is provided
        date_from = None
        if hours:
            date_from = datetime.now() - timedelta(hours=hours)

        incidents = db.get_incidents(
            limit = limit,
            offset = offset,
            severity = severity,
            log_type = log_type,
            source_host = source_host,
            date_from = date_from
        )

        return {
            "total_returned": len(incidents),
            "limit": limit,
            "offset": offset,
            "filters": {
                "severity": severity,
                "log_type": log_type,
                "source_host": source_host,
                "hours": hours
            },
            "incidents": incidents
        }
    except Exception as e:
        raise HTTPException(status_code = 500, detail = f"Database error: {str(e)}")
    
@app.get("/incidents/{incident_id}")
@limiter.limit("100/minute")
async def get_incident(
    request: Request,
    incident_id: int
):
    try:
        incident = db.get_single_incident(incident_id)

        if not incident:
            raise HTTPException(status_code = 404, detail = "Incident not found")

        return incident

    except HTTPException:
        raise
    except Exception as e:  # ← Missing except block!
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
        
@app.get("/statistics")
@limiter.limit("100/minute")
async def get_statistics(
    request: Request,
    hours: int = Query(24, ge=0, le=8760, description = "Time range in hours (0 for all-time, max 8760 = 1 year)")
):
    try:
        stats = db.get_stats(
            hours=hours
        )
        return stats
    except Exception as e:
        raise HTTPException(status_code = 500, detail = f"Database error: {str(e)}")
    

@app.get("/search/ip/{ip_address}")
@limiter.limit("100/minute")
async def search_by_ip(
    request: Request,
    ip_address: str
):
    try:
        incidents = db.search_by_ip(ip_address)

        return {
            "ip_address": ip_address,
            "total_found": len(incidents),
            "incidents": incidents
        }
    except Exception as e:
        raise HTTPException(status_code = 500, detail = f"Database error: {str(e)}")
    
@app.get("/recent")
@limiter.limit("100/minute")
async def get_recent_incidents(
    request: Request,
    hours: int = Query(24, ge=1, le=168, description = "Number of hours to look back (max 168 = 1 week)")
):
    try:
        incidents = db.get_recent(hours = hours)

        return {
            "hours": hours,
            "total_found": len(incidents),
            "incidents": incidents
        }
    except Exception as e:
        raise HTTPException(status_code = 500, detail = f"Database error: {str(e)}")
    
@app.post("/analyze/templates")
@limiter.limit("100/minute")
async def extract_attack_patterns(
    request: Request,
    hours: int = Query(4, ge=1, le=168, description = "Hours to analyze"),
    log_type: Optional[str] = Query(None, description="Filter by log type (system/web)"),
    severity: Optional[str] = Query(None, description="Filter by severity (CRITICAL/HIGH/MEDIUM/LOW/INFO)"),
    anomaly_only: bool = Query(False, description="Extract only anomalous logs, if selected")
):
    """
    Extract templates for security incidents using the Drain3 parser

    **Purpose**: Identify attack patterns over time
    **Use case**: Dashboard button "Analyze Attack Patterns"

    **Example requests**:
    - POST /analyze/templates?hours=4
    - POST /analyze/templates?hours=24&severity=HIGH
    - POST /analyze/templates?hours=4&log_type=system
    """
    try:
        logs = db.get_incidents_by_timerange(
            hours = hours,
            log_type = log_type,
            severity = severity,
            anomaly_only = anomaly_only
        )
        if not logs:
            return {
                "message": "No security incidents found in specified time range",
                "total_logs_processed": 0,
                "total_templates": 0,
                "time_range_hours": hours,
                "filters": {
                    "log_type": log_type,
                    "severity": severity
                },
                "templates": []
            }
        results = drain_analyzer.extract_templates(logs)

        results["time_range_hours"] = hours
        results["filters"] = {
            "log_type": log_type,
            "severity": severity
        }
        results["anomaly_only"] = anomaly_only

        return results
    
    except Exception as e:
        raise HTTPException(
            status_code = 500,
            detail = f"Template extraction failed: {str(e)}"
        )
    
@app.on_event("shutdown")
async def shutdown_event():
    db.close()

# Run with: uvicorn main:app --host 0.0.0.0 --port 8000








