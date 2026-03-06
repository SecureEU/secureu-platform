from datetime import datetime, timedelta, timezone
from typing import Optional

from fastapi import APIRouter, Query
from app.opensearch_client import get_client

router = APIRouter(prefix="/ddos", tags=["ddos"])


@router.get("")
def get_ddos_events(
    hours: int = Query(24, ge=1, le=720),
    size: int = Query(100, ge=1, le=1000),
    severity_level: Optional[str] = None,
    src_ip: Optional[str] = None,
    dest_ip: Optional[str] = None,
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    must_clauses = [
        {"range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}}
    ]

    if severity_level:
        must_clauses.append({"term": {"severity_level.keyword": severity_level}})
    if src_ip:
        must_clauses.append({"term": {"src_ip.keyword": src_ip}})
    if dest_ip:
        must_clauses.append({"term": {"dest_ip.keyword": dest_ip}})

    query = {
        "size": size,
        "query": {"bool": {"must": must_clauses}},
        "sort": [{"@timestamp": {"order": "desc"}}],
    }

    response = client.search(index="mirai-ddos*", body=query)
    hits = response.get("hits", {}).get("hits", [])

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "events": [hit["_source"] for hit in hits],
    }


@router.get("/stats")
def get_ddos_stats(
    hours: int = Query(24, ge=1, le=720),
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    query = {
        "size": 20,
        "query": {
            "range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}
        },
        "aggs": {
            "by_severity": {"terms": {"field": "severity_level.keyword", "size": 10}},
            "by_signature": {"terms": {"field": "alert_signature.keyword", "size": 10}},
            "by_attack_target": {"terms": {"field": "attack_target.keyword", "size": 10}},
            "top_src_ips": {"terms": {"field": "src_ip.keyword", "size": 10}},
            "top_dest_ips": {"terms": {"field": "dest_ip.keyword", "size": 10}},
            "total_bytes_toserver": {"sum": {"field": "flow.bytes_toserver"}},
            "total_bytes_toclient": {"sum": {"field": "flow.bytes_toclient"}},
            "total_pkts_toserver": {"sum": {"field": "flow.pkts_toserver"}},
            "total_pkts_toclient": {"sum": {"field": "flow.pkts_toclient"}},
            "over_time": {
                "date_histogram": {
                    "field": "@timestamp",
                    "fixed_interval": f"{max(1, hours // 24)}h",
                    "min_doc_count": 0,
                },
                "aggs": {
                    "bytes_toserver": {"sum": {"field": "flow.bytes_toserver"}},
                    "bytes_toclient": {"sum": {"field": "flow.bytes_toclient"}},
                },
            },
        },
    }

    query["sort"] = [{"@timestamp": {"order": "desc"}}]

    response = client.search(index="mirai-ddos*", body=query)
    aggs = response.get("aggregations", {})
    hits = response.get("hits", {}).get("hits", [])

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "events": [hit["_source"] for hit in hits],
        "by_severity": aggs.get("by_severity", {}).get("buckets", []),
        "by_signature": aggs.get("by_signature", {}).get("buckets", []),
        "by_attack_target": aggs.get("by_attack_target", {}).get("buckets", []),
        "top_src_ips": aggs.get("top_src_ips", {}).get("buckets", []),
        "top_dest_ips": aggs.get("top_dest_ips", {}).get("buckets", []),
        "total_bytes_toserver": aggs.get("total_bytes_toserver", {}).get("value", 0),
        "total_bytes_toclient": aggs.get("total_bytes_toclient", {}).get("value", 0),
        "total_pkts_toserver": aggs.get("total_pkts_toserver", {}).get("value", 0),
        "total_pkts_toclient": aggs.get("total_pkts_toclient", {}).get("value", 0),
        "over_time": aggs.get("over_time", {}).get("buckets", []),
    }
