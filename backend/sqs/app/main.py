from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import alerts, dashboard, ddos, http_logs, flows, et_alerts

app = FastAPI(title="SECUR-EU DASHBOARDS", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(alerts.router)
app.include_router(ddos.router)
app.include_router(http_logs.router)
app.include_router(dashboard.router)
app.include_router(flows.router)
app.include_router(et_alerts.router)


@app.get("/health")
def health_check():
    return {"status": "healthy"}
