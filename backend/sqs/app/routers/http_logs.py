from datetime import datetime, timedelta, timezone
from typing import Optional

from fastapi import APIRouter, Query
from app.opensearch_client import get_client

router = APIRouter(prefix="/http", tags=["http"])


@router.get("")
def get_http_logs(
    hours: int = Query(24, ge=1, le=720),
    size: int = Query(100, ge=1, le=1000),
    method: Optional[str] = None,
    src_ip: Optional[str] = None,
    hostname: Optional[str] = None,
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    must_clauses = [
        {"range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}}
    ]

    if method:
        must_clauses.append({"term": {"http_method.keyword": method}})
    if src_ip:
        must_clauses.append({"term": {"src_ip.keyword": src_ip}})
    if hostname:
        must_clauses.append({"term": {"http_hostname.keyword": hostname}})

    query = {
        "size": size,
        "query": {"bool": {"must": must_clauses}},
        "sort": [{"@timestamp": {"order": "desc"}}],
    }

    response = client.search(index="mirai-http*", body=query)
    hits = response.get("hits", {}).get("hits", [])

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "logs": [hit["_source"] for hit in hits],
    }


@router.get("/stats")
def get_http_stats(
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
            "by_method": {"terms": {"field": "http_method.keyword", "size": 10}},
            "by_hostname": {"terms": {"field": "http_hostname.keyword", "size": 10}},
            "by_user_agent": {"terms": {"field": "http_user_agent.keyword", "size": 10}},
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

    response = client.search(index="mirai-http*", body=query)
    aggs = response.get("aggregations", {})

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "by_method": aggs.get("by_method", {}).get("buckets", []),
        "by_hostname": aggs.get("by_hostname", {}).get("buckets", []),
        "by_user_agent": aggs.get("by_user_agent", {}).get("buckets", []),
        "top_src_ips": aggs.get("top_src_ips", {}).get("buckets", []),
        "top_dest_ips": aggs.get("top_dest_ips", {}).get("buckets", []),
        "over_time": aggs.get("over_time", {}).get("buckets", []),
    }
