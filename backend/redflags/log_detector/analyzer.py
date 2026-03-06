import json
import time
import requests
import glob
import os
from postgre_store import PostgresStorage
import config


def wait_for_ollama():
    print("Waiting for Ollama...")
    for attempt in range(60):
        try:
            HOST = os.getenv("OLLAMA_HOST", "redflags-ollama")
            PORT = os.getenv("OLLAMA_PORT", "11434")
            response = requests.get(f"http://{HOST}:{PORT}/api/tags", timeout=5)
            if response.status_code == 200:
                models = response.json().get('models', [])
                if any(config.OLLAMA_MODEL in m['name'] for m in models):
                    print(f"Ollama ready with {config.OLLAMA_MODEL}")
                    return True
        except:
            pass
        print(f"  Attempt {attempt + 1}/60...")
        time.sleep(2)
    print("Ollama timeout")
    return False


def analyze_alert_with_ollama(alert_data):
    """Send a Suricata alert to Ollama for detailed security analysis."""
    alert = alert_data.get("alert", {})
    src_ip = alert_data.get("src_ip", "unknown")
    dest_ip = alert_data.get("dest_ip", "unknown")
    src_port = alert_data.get("src_port", "")
    dest_port = alert_data.get("dest_port", "")
    proto = alert_data.get("proto", "unknown")
    timestamp = alert_data.get("timestamp", "")

    # Build a concise summary for the LLM
    alert_summary = (
        f"Suricata Alert:\n"
        f"  Timestamp: {timestamp}\n"
        f"  Signature: {alert.get('signature', 'N/A')}\n"
        f"  Category: {alert.get('category', 'N/A')}\n"
        f"  Severity: {alert.get('severity', 'N/A')}\n"
        f"  Action: {alert.get('action', 'N/A')}\n"
        f"  Source: {src_ip}:{src_port}\n"
        f"  Destination: {dest_ip}:{dest_port}\n"
        f"  Protocol: {proto}\n"
    )

    prompt = config.SURICATA_ALERT_PROMPT.format(alert_summary=alert_summary)

    try:
        HOST = os.getenv("OLLAMA_HOST", "redflags-ollama")
        PORT = os.getenv("OLLAMA_PORT", "11434")
        response = requests.post(
            f"http://{HOST}:{PORT}/api/generate",
            json={
                "model": config.OLLAMA_MODEL,
                "prompt": prompt,
                "stream": False,
                "temperature": config.OLLAMA_TEMPERATURE,
            },
            timeout=config.OLLAMA_TIMEOUT,
        )

        if response.status_code != 200:
            print(f"  Ollama HTTP {response.status_code}")
            return None

        result = response.json()
        llm_output = result.get("response", "").strip()

        # Clean markdown code blocks if present
        if "```json" in llm_output:
            llm_output = llm_output.split("```json")[1].split("```")[0].strip()
        elif "```" in llm_output:
            llm_output = llm_output.split("```")[1].split("```")[0].strip()

        return json.loads(llm_output)
    except json.JSONDecodeError as e:
        print(f"  Invalid JSON from LLM: {e}")
        return None
    except Exception as e:
        print(f"  Ollama error: {e}")
        return None


def find_latest_log_file():
    pattern = os.path.join(config.LOG_DIR, "aggregated*.ndjson")
    files = glob.glob(pattern)
    if not files:
        return None
    return max(files, key=os.path.getctime)


def tail_file(db, log_file):
    if not os.path.exists(log_file):
        print(f"File not found: {log_file}")
        return

    print(f"Tailing: {os.path.basename(log_file)}")
    print("Filtering for Suricata alert events only, analyzing with Ollama...")
    processed = 0
    skipped = 0

    with open(log_file, "r") as f:
        f.seek(0, os.SEEK_END)
        while True:
            line = f.readline()

            if line:
                try:
                    log_entry = json.loads(line)
                    message = log_entry.get("message", "")

                    if not message:
                        continue

                    # Parse the inner message (Suricata JSON embedded in filebeat)
                    try:
                        inner = json.loads(message)
                    except (json.JSONDecodeError, TypeError):
                        skipped += 1
                        continue

                    # Only process alert events
                    if inner.get("event_type") != "alert":
                        skipped += 1
                        continue

                    analysis = analyze_alert_with_ollama(inner)
                    if analysis:
                        log_entry["_parsed_alert"] = inner

                        incident_id = db.store_anomaly(log_entry, analysis)
                        if incident_id:
                            processed += 1
                            severity = analysis.get("severity", "UNKNOWN")
                            desc = analysis.get("description", "")[:80]
                            print(f"  [{severity}] #{incident_id}: {desc}")

                    time.sleep(config.RATE_LIMIT_DELAY)

                except json.JSONDecodeError:
                    pass
                except Exception as e:
                    print(f"Error processing log: {e}")
            else:
                time.sleep(1)


def main():
    print("Red Flags Detector (Ollama LLM, alerts only)")

    if not wait_for_ollama():
        return

    print("Waiting for PostgreSQL...")
    time.sleep(5)

    try:
        db = PostgresStorage()
    except Exception as e:
        print(f"Failed to initialize DB: {e}")
        return

    try:
        log_file = find_latest_log_file()

        if not log_file:
            print(f"No log files found in {config.LOG_DIR}")
            print("Waiting for logs...")
            while not log_file:
                time.sleep(config.POLL_INTERVAL)
                log_file = find_latest_log_file()

        print(f"Found log file: {os.path.basename(log_file)}")
        tail_file(db, log_file)
    except KeyboardInterrupt:
        print("\nStopping analysis...")
    except Exception as e:
        print(f"Fatal error: {e}")
    finally:
        db.close()
        print("Shutdown complete")


if __name__ == "__main__":
    main()
