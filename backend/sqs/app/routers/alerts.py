from datetime import datetime, timedelta, timezone
from typing import Optional

from fastapi import APIRouter, Query
from app.opensearch_client import get_client

router = APIRouter(prefix="/alerts", tags=["alerts"])


@router.get("")
def get_alerts(
    hours: int = Query(24, ge=1, le=720),
    size: int = Query(100, ge=1, le=1000),
    severity: Optional[int] = None,
    mirai_stage: Optional[str] = None,
    src_ip: Optional[str] = None,
    dest_ip: Optional[str] = None,
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    must_clauses = [
        {"range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}}
    ]

    if severity is not None:
        must_clauses.append({"term": {"alert_severity": severity}})
    if mirai_stage:
        must_clauses.append({"term": {"mirai_stage.keyword": mirai_stage}})
    if src_ip:
        must_clauses.append({"term": {"src_ip.keyword": src_ip}})
    if dest_ip:
        must_clauses.append({"term": {"dest_ip.keyword": dest_ip}})

    query = {
        "size": size,
        "query": {"bool": {"must": must_clauses}},
        "sort": [{"@timestamp": {"order": "desc"}}],
    }

    response = client.search(index="mirai-alerts*", body=query)
    hits = response.get("hits", {}).get("hits", [])

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "alerts": [hit["_source"] for hit in hits],
    }


@router.get("/stats")
def get_alert_stats(
    hours: int = Query(24, ge=1, le=720),
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    query = {
        "size": 0,
        "query": {
            "range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}
        },
        "aggs": {
            "by_severity": {"terms": {"field": "alert_severity", "size": 10}},
            "by_mirai_stage": {"terms": {"field": "mirai_stage.keyword", "size": 10}},
            "by_signature": {"terms": {"field": "alert_signature.keyword", "size": 10}},
            "by_category": {"terms": {"field": "alert_category.keyword", "size": 10}},
            "by_protocol": {"terms": {"field": "proto.keyword", "size": 10}},
            "by_dest_port": {"terms": {"field": "dest_port", "size": 15}},
            "top_src_ips": {"terms": {"field": "src_ip.keyword", "size": 10}},
            "top_dest_ips": {"terms": {"field": "dest_ip.keyword", "size": 10}},
            "over_time": {
                "date_histogram": {
                    "field": "@timestamp",
                    "fixed_interval": f"{max(1, hours // 24)}h",
                    "min_doc_count": 0,
                }
            },
        },
    }

    response = client.search(index="mirai-alerts*", body=query)
    aggs = response.get("aggregations", {})

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "by_severity": aggs.get("by_severity", {}).get("buckets", []),
        "by_mirai_stage": aggs.get("by_mirai_stage", {}).get("buckets", []),
        "by_signature": aggs.get("by_signature", {}).get("buckets", []),
        "by_category": aggs.get("by_category", {}).get("buckets", []),
        "by_protocol": aggs.get("by_protocol", {}).get("buckets", []),
        "by_dest_port": aggs.get("by_dest_port", {}).get("buckets", []),
        "top_src_ips": aggs.get("top_src_ips", {}).get("buckets", []),
        "top_dest_ips": aggs.get("top_dest_ips", {}).get("buckets", []),
        "over_time": aggs.get("over_time", {}).get("buckets", []),
    }
