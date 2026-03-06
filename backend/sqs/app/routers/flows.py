from datetime import datetime, timedelta, timezone
from typing import Optional

from fastapi import APIRouter, Query

from app.opensearch_client import get_client

router = APIRouter(prefix="/flows", tags=["flows"])


@router.get("/stats")
def get_flow_stats(
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
                    {"term": {"event_type.keyword": "flow"}}
                ]
            }
        },
        "aggs": {
            "by_proto": {"terms": {"field": "proto.keyword", "size": 10}},
            "by_app_proto": {"terms": {"field": "app_proto.keyword", "size": 10}},
            "by_dest_port": {"terms": {"field": "dest_port", "size": 15}},
            "top_src_ips": {
                "terms": {"field": "src_ip.keyword", "size": 10},
                "aggs": {
                    "bytes": {"sum": {"field": "flow.bytes_toserver"}}
                }
            },
            "top_dest_ips": {
                "terms": {"field": "dest_ip.keyword", "size": 10},
                "aggs": {
                    "bytes": {"sum": {"field": "flow.bytes_toclient"}}
                }
            },
            "total_bytes_toserver": {"sum": {"field": "flow.bytes_toserver"}},
            "total_bytes_toclient": {"sum": {"field": "flow.bytes_toclient"}},
            "total_pkts_toserver": {"sum": {"field": "flow.pkts_toserver"}},
            "total_pkts_toclient": {"sum": {"field": "flow.pkts_toclient"}},
            "by_state": {"terms": {"field": "flow.state.keyword", "size": 10}},
            "over_time": {
                "date_histogram": {
                    "field": "@timestamp",
                    "fixed_interval": f"{max(1, hours // 24)}h",
                    "min_doc_count": 0,
                },
                "aggs": {
                    "bytes_toserver": {"sum": {"field": "flow.bytes_toserver"}},
                    "bytes_toclient": {"sum": {"field": "flow.bytes_toclient"}},
                }
            },
        },
    }

    response = client.search(index="suricata-*", body=query)
    aggs = response.get("aggregations", {})

    # Process top IPs to include bytes
    top_src = []
    for bucket in aggs.get("top_src_ips", {}).get("buckets", []):
        top_src.append({
            "key": bucket["key"],
            "doc_count": bucket["doc_count"],
            "bytes": bucket["bytes"]["value"]
        })

    top_dest = []
    for bucket in aggs.get("top_dest_ips", {}).get("buckets", []):
        top_dest.append({
            "key": bucket["key"],
            "doc_count": bucket["doc_count"],
            "bytes": bucket["bytes"]["value"]
        })

    return {
        "total": response.get("hits", {}).get("total", {}).get("value", 0),
        "by_proto": aggs.get("by_proto", {}).get("buckets", []),
        "by_app_proto": aggs.get("by_app_proto", {}).get("buckets", []),
        "by_dest_port": aggs.get("by_dest_port", {}).get("buckets", []),
        "by_state": aggs.get("by_state", {}).get("buckets", []),
        "top_src_ips": top_src,
        "top_dest_ips": top_dest,
        "total_bytes_toserver": aggs.get("total_bytes_toserver", {}).get("value", 0),
        "total_bytes_toclient": aggs.get("total_bytes_toclient", {}).get("value", 0),
        "total_pkts_toserver": aggs.get("total_pkts_toserver", {}).get("value", 0),
        "total_pkts_toclient": aggs.get("total_pkts_toclient", {}).get("value", 0),
        "over_time": aggs.get("over_time", {}).get("buckets", []),
    }
