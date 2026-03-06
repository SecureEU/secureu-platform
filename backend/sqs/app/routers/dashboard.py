from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Query
from app.opensearch_client import get_client

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


@router.get("/summary")
def get_dashboard_summary(
    hours: int = Query(24, ge=1, le=720),
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    time_filter = {
        "range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}
    }

    alerts_query = {
        "size": 0,
        "query": time_filter,
        "aggs": {
            "critical": {"filter": {"term": {"alert_severity": 1}}},
            "high": {"filter": {"term": {"alert_severity": 2}}},
            "medium": {"filter": {"term": {"alert_severity": 3}}},
            "unique_src_ips": {"cardinality": {"field": "src_ip.keyword"}},
            "unique_dest_ips": {"cardinality": {"field": "dest_ip.keyword"}},
        },
    }

    ddos_query = {
        "size": 0,
        "query": time_filter,
        "aggs": {
            "critical": {"filter": {"term": {"severity_level.keyword": "critical"}}},
            "total_bytes": {"sum": {"field": "flow.bytes_toserver"}},
            "total_packets": {"sum": {"field": "flow.pkts_toserver"}},
        },
    }

    http_query = {
        "size": 0,
        "query": time_filter,
    }

    alerts_resp = client.search(index="mirai-alerts*", body=alerts_query)
    ddos_resp = client.search(index="mirai-ddos*", body=ddos_query)
    http_resp = client.search(index="mirai-http*", body=http_query)

    alerts_aggs = alerts_resp.get("aggregations", {})
    ddos_aggs = ddos_resp.get("aggregations", {})

    return {
        "alerts": {
            "total": alerts_resp.get("hits", {}).get("total", {}).get("value", 0),
            "critical": alerts_aggs.get("critical", {}).get("doc_count", 0),
            "high": alerts_aggs.get("high", {}).get("doc_count", 0),
            "medium": alerts_aggs.get("medium", {}).get("doc_count", 0),
            "unique_sources": alerts_aggs.get("unique_src_ips", {}).get("value", 0),
            "unique_targets": alerts_aggs.get("unique_dest_ips", {}).get("value", 0),
        },
        "ddos": {
            "total": ddos_resp.get("hits", {}).get("total", {}).get("value", 0),
            "critical": ddos_aggs.get("critical", {}).get("doc_count", 0),
            "total_bytes": ddos_aggs.get("total_bytes", {}).get("value", 0),
            "total_packets": ddos_aggs.get("total_packets", {}).get("value", 0),
        },
        "http": {
            "total": http_resp.get("hits", {}).get("total", {}).get("value", 0),
        },
        "time_range": {"start": start_time.isoformat(), "end": now.isoformat(), "hours": hours},
    }


@router.get("/timeline")
def get_timeline(
    hours: int = Query(24, ge=1, le=720),
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    interval = f"{max(1, hours // 24)}h"

    time_filter = {
        "range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}
    }

    base_query = {
        "size": 0,
        "query": time_filter,
        "aggs": {
            "over_time": {
                "date_histogram": {
                    "field": "@timestamp",
                    "fixed_interval": interval,
                    "min_doc_count": 0,
                    "extended_bounds": {
                        "min": start_time.isoformat(),
                        "max": now.isoformat(),
                    },
                }
            }
        },
    }

    alerts_resp = client.search(index="mirai-alerts*", body=base_query)
    ddos_resp = client.search(index="mirai-ddos*", body=base_query)
    http_resp = client.search(index="mirai-http*", body=base_query)

    return {
        "alerts": alerts_resp.get("aggregations", {}).get("over_time", {}).get("buckets", []),
        "ddos": ddos_resp.get("aggregations", {}).get("over_time", {}).get("buckets", []),
        "http": http_resp.get("aggregations", {}).get("over_time", {}).get("buckets", []),
        "interval": interval,
    }


@router.get("/recent-alerts")
def get_recent_alerts(
    size: int = Query(20, ge=1, le=100),
):
    client = get_client()

    query = {
        "size": size,
        "query": {"match_all": {}},
        "sort": [{"@timestamp": {"order": "desc"}}],
        "_source": [
            "@timestamp",
            "alert_signature",
            "alert_severity",
            "alert_category",
            "src_ip",
            "dest_ip",
            "mirai_stage",
            "proto",
        ],
    }

    response = client.search(index="mirai-alerts*", body=query)
    hits = response.get("hits", {}).get("hits", [])

    return {"alerts": [hit["_source"] for hit in hits]}


@router.get("/top-attackers")
def get_top_attackers(
    hours: int = Query(24, ge=1, le=720),
    size: int = Query(10, ge=1, le=50),
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
            "top_sources": {
                "terms": {"field": "src_ip.keyword", "size": size},
                "aggs": {
                    "signatures": {"terms": {"field": "alert_signature.keyword", "size": 5}},
                    "targets": {"cardinality": {"field": "dest_ip.keyword"}},
                },
            }
        },
    }

    response = client.search(index="mirai-alerts*", body=query)
    buckets = response.get("aggregations", {}).get("top_sources", {}).get("buckets", [])

    return {
        "attackers": [
            {
                "ip": b["key"],
                "count": b["doc_count"],
                "unique_targets": b["targets"]["value"],
                "top_signatures": [s["key"] for s in b["signatures"]["buckets"]],
            }
            for b in buckets
        ]
    }
