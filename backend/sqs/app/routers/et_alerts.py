from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Query

from app.opensearch_client import get_client

router = APIRouter(prefix="/et-alerts", tags=["et-alerts"])


@router.get("/stats")
def get_et_alert_stats(
    hours: int = Query(24, ge=1, le=720),
):
    client = get_client()
    now = datetime.now(timezone.utc)
    start_time = now - timedelta(hours=hours)

    query = {
        "size": 0,
        "query": {
            "bool": {
                "must": [
                    {"range": {"@timestamp": {"gte": start_time.isoformat(), "lte": now.isoformat()}}},
                    {"term": {"event_type.keyword": "alert"}},
                    {"wildcard": {"alert.signature.keyword": "ET*"}}
                ]
            }
        },
        "aggs": {
            "by_severity": {"terms": {"field": "alert.severity", "size": 10}},
            "by_category": {"terms": {"field": "alert.category.keyword", "size": 10}},
            "by_signature": {"terms": {"field": "alert.signature.keyword", "size": 15}},
            "top_src_ips": {"terms": {"field": "src_ip.keyword", "size": 10}},
            "top_dest_ips": {"terms": {"field": "dest_ip.keyword", "size": 10}},
            "by_proto": {"terms": {"field": "proto.keyword", "size": 10}},
            "by_dest_port": {"terms": {"field": "dest_port", "size": 15}},
            "over_time": {
                "date_histogram": {
                    "field": "@timestamp",
                    "fixed_interval": f"{max(1, hours // 24)}h",
                    "min_doc_count": 0,
                }
            },
        },
    }

    response = client.search(index="suricata-*", body=query)
    aggs = response.get("aggregations", {})

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "by_severity": aggs.get("by_severity", {}).get("buckets", []),
        "by_category": aggs.get("by_category", {}).get("buckets", []),
        "by_signature": aggs.get("by_signature", {}).get("buckets", []),
        "top_src_ips": aggs.get("top_src_ips", {}).get("buckets", []),
        "top_dest_ips": aggs.get("top_dest_ips", {}).get("buckets", []),
        "by_proto": aggs.get("by_proto", {}).get("buckets", []),
        "by_dest_port": aggs.get("by_dest_port", {}).get("buckets", []),
        "over_time": aggs.get("over_time", {}).get("buckets", []),
    }


@router.get("/recent")
def get_recent_et_alerts(
    size: int = Query(20, ge=1, le=100),
):
    client = get_client()

    query = {
        "size": size,
        "query": {
            "bool": {
                "must": [
                    {"term": {"event_type.keyword": "alert"}},
                    {"wildcard": {"alert.signature.keyword": "ET*"}}
                ]
            }
        },
        "sort": [{"@timestamp": {"order": "desc"}}],
        "_source": [
            "@timestamp",
            "alert.signature",
            "alert.severity",
            "alert.category",
            "src_ip",
            "dest_ip",
            "src_port",
            "dest_port",
            "proto",
        ],
    }

    response = client.search(index="suricata-*", body=query)
    hits = response.get("hits", {}).get("hits", [])

    return {"alerts": [hit["_source"] for hit in hits]}
