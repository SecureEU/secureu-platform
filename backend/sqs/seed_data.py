"""Seed OpenSearch with realistic Mirai botnet detection data."""
import random
import json
from datetime import datetime, timedelta, timezone
from opensearchpy import OpenSearch, helpers

import os

client = OpenSearch(
    hosts=[{"host": os.environ.get("OPENSEARCH_HOST", "opensearch"), "port": 9200}],
    use_ssl=False,
    verify_certs=False,
)

NOW = datetime.now(timezone.utc)

# Realistic IPs
ATTACKER_IPS = [
    "185.220.101.34", "45.148.10.92", "194.26.29.15", "89.248.165.52",
    "162.142.125.217", "71.6.135.131", "198.235.24.159", "118.25.6.39",
    "36.110.228.254", "103.203.57.20", "212.70.149.34", "91.240.118.172",
]
TARGET_IPS = [
    "10.0.1.50", "10.0.1.51", "10.0.1.100", "10.0.2.10",
    "10.0.2.20", "10.0.3.5", "192.168.1.10", "192.168.1.50",
]
PROTOCOLS = ["TCP", "UDP", "ICMP"]

MIRAI_SIGNATURES = [
    "ET MALWARE Mirai Variant CnC Beacon",
    "ET TROJAN Mirai Bot Scan",
    "ET EXPLOIT Telnet Brute Force Attempt",
    "ET MALWARE Mirai Bot Credential Stuffing",
    "ET SCAN SSH Brute Force Attempt",
    "ET TROJAN Mirai Variant Download Request",
    "ET MALWARE IoT Bot Propagation",
    "ET EXPLOIT Default IoT Credential Login",
]
MIRAI_STAGES = ["scan", "exploit", "c2_beacon", "propagation", "ddos_attack", "credential_stuffing"]
CATEGORIES = ["Trojan", "Malware", "Exploit", "Scan", "Botnet Activity"]

ET_SIGNATURES = [
    "ET SCAN Potential SSH Scan",
    "ET POLICY Outbound SSH Traffic",
    "ET SCAN Nmap SYN Scan",
    "ET POLICY DNS Query to .onion Domain",
    "ET TROJAN Known Bot CnC Traffic",
    "ET EXPLOIT CVE-2023-1234 Attempt",
    "ET INFO Suspicious DNS TXT Query",
    "ET SCAN Aggressive Port Scan",
]
ET_CATEGORIES = [
    "A Network Trojan was detected",
    "Potentially Bad Traffic",
    "Attempted Information Leak",
    "Misc Attack",
    "Not Suspicious Traffic",
]
APP_PROTOS = ["ssh", "http", "dns", "tls", "failed", "ntp", "telnet"]
FLOW_STATES = ["new", "established", "closed", "syn_sent"]
HTTP_METHODS = ["GET", "POST", "HEAD", "PUT", "OPTIONS"]
HTTP_HOSTNAMES = [
    "api.malware-c2.example.com", "update.botnet.example.net",
    "scan.exploit.example.org", "telnet.iot.example.com",
    "raw.payload.example.net",
]
USER_AGENTS = [
    "Mozilla/5.0", "curl/7.68.0", "Wget/1.21", "python-requests/2.28",
    "Go-http-client/1.1", "Mirai-Bot/1.0", "",
]
DDOS_SIGNATURES = [
    "ET DOS UDP Flood Detected",
    "ET DOS SYN Flood Detected",
    "ET DOS HTTP Flood Detected",
    "ET DOS Amplification Attack Detected",
    "ET DOS Volumetric Attack Detected",
]
ATTACK_TARGETS = ["web_server", "dns_server", "iot_gateway", "database", "api_server"]


def rand_ts(hours_back=168):
    """Random timestamp within the last N hours."""
    offset = random.uniform(0, hours_back * 3600)
    return (NOW - timedelta(seconds=offset)).isoformat()


def gen_mirai_alerts(count=500):
    for _ in range(count):
        severity = random.choices([1, 2, 3], weights=[15, 35, 50])[0]
        yield {
            "_index": "mirai-alerts-2026.03",
            "_source": {
                "@timestamp": rand_ts(),
                "event_type": "alert",
                "src_ip": random.choice(ATTACKER_IPS),
                "dest_ip": random.choice(TARGET_IPS),
                "src_port": random.randint(1024, 65535),
                "dest_port": random.choice([23, 22, 80, 443, 2323, 8080, 8443, 5555]),
                "proto": random.choice(PROTOCOLS),
                "alert_severity": severity,
                "alert_signature": random.choice(MIRAI_SIGNATURES),
                "alert_category": random.choice(CATEGORIES),
                "mirai_stage": random.choice(MIRAI_STAGES),
            },
        }


def gen_ddos_events(count=200):
    for _ in range(count):
        yield {
            "_index": "mirai-ddos-2026.03",
            "_source": {
                "@timestamp": rand_ts(),
                "event_type": "ddos",
                "src_ip": random.choice(ATTACKER_IPS),
                "dest_ip": random.choice(TARGET_IPS[:4]),
                "src_port": random.randint(1024, 65535),
                "dest_port": random.choice([80, 443, 53, 8080]),
                "proto": random.choice(["TCP", "UDP"]),
                "severity_level": random.choice(["critical", "high", "medium", "low"]),
                "alert_signature": random.choice(DDOS_SIGNATURES),
                "attack_target": random.choice(ATTACK_TARGETS),
                "flow": {
                    "bytes_toserver": random.randint(50000, 50000000),
                    "bytes_toclient": random.randint(1000, 500000),
                    "pkts_toserver": random.randint(100, 100000),
                    "pkts_toclient": random.randint(10, 10000),
                },
            },
        }


def gen_http_logs(count=300):
    for _ in range(count):
        yield {
            "_index": "mirai-http-2026.03",
            "_source": {
                "@timestamp": rand_ts(),
                "event_type": "http",
                "src_ip": random.choice(ATTACKER_IPS),
                "dest_ip": random.choice(TARGET_IPS),
                "src_port": random.randint(1024, 65535),
                "dest_port": random.choice([80, 443, 8080, 8443]),
                "http_method": random.choices(HTTP_METHODS, weights=[50, 25, 10, 10, 5])[0],
                "http_hostname": random.choice(HTTP_HOSTNAMES),
                "http_url": random.choice(["/", "/api/v1/cmd", "/update", "/login", "/shell", "/cgi-bin/exec"]),
                "http_user_agent": random.choice(USER_AGENTS),
                "http_status": random.choice([200, 301, 403, 404, 500]),
            },
        }


def gen_suricata_flows(count=400):
    for _ in range(count):
        yield {
            "_index": "suricata-2026.03.04",
            "_source": {
                "@timestamp": rand_ts(),
                "event_type": "flow",
                "src_ip": random.choice(ATTACKER_IPS + TARGET_IPS),
                "dest_ip": random.choice(TARGET_IPS + ATTACKER_IPS),
                "src_port": random.randint(1024, 65535),
                "dest_port": random.choice([22, 23, 53, 80, 443, 8080, 123, 2323]),
                "proto": random.choice(PROTOCOLS),
                "app_proto": random.choice(APP_PROTOS),
                "flow": {
                    "bytes_toserver": random.randint(100, 5000000),
                    "bytes_toclient": random.randint(100, 3000000),
                    "pkts_toserver": random.randint(1, 50000),
                    "pkts_toclient": random.randint(1, 30000),
                    "state": random.choice(FLOW_STATES),
                },
            },
        }


def gen_suricata_et_alerts(count=250):
    for _ in range(count):
        yield {
            "_index": "suricata-2026.03.04",
            "_source": {
                "@timestamp": rand_ts(),
                "event_type": "alert",
                "src_ip": random.choice(ATTACKER_IPS),
                "dest_ip": random.choice(TARGET_IPS),
                "src_port": random.randint(1024, 65535),
                "dest_port": random.choice([22, 23, 53, 80, 443]),
                "proto": random.choice(PROTOCOLS),
                "alert": {
                    "signature": random.choice(ET_SIGNATURES),
                    "severity": random.choices([1, 2, 3], weights=[20, 40, 40])[0],
                    "category": random.choice(ET_CATEGORIES),
                },
            },
        }


def main():
    print("Seeding OpenSearch with sample data...")

    generators = [
        ("mirai-alerts", gen_mirai_alerts, 500),
        ("mirai-ddos", gen_ddos_events, 200),
        ("mirai-http", gen_http_logs, 300),
        ("suricata flows", gen_suricata_flows, 400),
        ("suricata ET alerts", gen_suricata_et_alerts, 250),
    ]

    total = 0
    for name, gen_fn, count in generators:
        print(f"  Indexing {count} {name} docs...")
        success, errors = helpers.bulk(client, gen_fn(count), raise_on_error=False)
        if errors:
            print(f"    WARNING: {len(errors)} errors")
        total += success
        print(f"    Indexed {success} docs")

    # Refresh so data is immediately searchable
    client.indices.refresh(index="mirai-*,suricata-*")
    print(f"\nDone! Total docs indexed: {total}")

    # Verify
    for pattern in ["mirai-alerts*", "mirai-ddos*", "mirai-http*", "suricata-*"]:
        resp = client.count(index=pattern)
        print(f"  {pattern}: {resp['count']} docs")


if __name__ == "__main__":
    main()
