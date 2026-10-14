import json
import os

import requests

# The SSL checker used DeepSeek, which meant every answer needed an API key the
# UI never collected and sent scan data to a third party. It now talks to the
# Ollama instance the RedFlags detector already runs, so the model is local and
# no key is involved.
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://redflags-ollama:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2:latest")
OLLAMA_TIMEOUT = int(os.getenv("OLLAMA_TIMEOUT", "180"))
OLLAMA_KEEP_ALIVE = os.getenv("OLLAMA_KEEP_ALIVE", "30m")
OLLAMA_MAX_TOKENS = int(os.getenv("OLLAMA_MAX_TOKENS", "400"))

SYSTEM_PROMPT = "You are a cybersecurity assistant."


def _summarise_host_data(host_data):
    """Condense a scan record into the facts worth reasoning about.

    The whole record used to go into the prompt verbatim. Measured on the test
    VM that was 1650 tokens, and prompt evaluation ran at ~18 tok/s on CPU - 89
    of the 155 seconds a question took were spent reading the prompt before the
    model wrote a single word. Most of that bulk is every cipher of every TLS
    version and the full text of each security header, which the answer never
    needs: whether a header is present, and which protocols and weak ciphers
    were seen, is enough.
    """
    if not isinstance(host_data, dict):
        return json.dumps(host_data, default=str)

    cert = host_data.get("certificate") or {}
    headers = host_data.get("headers") or {}
    protocols = host_data.get("protocols") or {}
    scan = host_data.get("scan") or {}

    protocols_json = protocols.get("protocols_json") or {}
    versions = sorted(protocols_json) if isinstance(protocols_json, dict) else []
    cipher_count = sum(len(v or []) for v in protocols_json.values()) if isinstance(protocols_json, dict) else 0

    # Header checks are booleans in the record; report them by name, not value.
    present = sorted(k for k, v in headers.items() if k != "success" and v)
    missing = sorted(k for k, v in headers.items() if k != "success" and not v)

    summary = {
        "host": scan.get("host") or host_data.get("host"),
        "scanned_at": scan.get("date") or scan.get("scan_date"),
        "certificate": {
            "ssl_version": cert.get("ssl_version"),
            "cipher_suite": cert.get("cipher_suite"),
            "key_type": cert.get("key_type"),
            "key_size": cert.get("key_size"),
            "issuers": cert.get("issuers"),
            "expires": cert.get("expire_date"),
            "expires_in_days": cert.get("expires_in"),
            "has_expired": cert.get("has_expired"),
        },
        "security_headers_present": present,
        "security_headers_missing": missing,
        "tls_versions_offered": versions,
        "cipher_suites_offered": cipher_count,
        "weak_cipher_count": protocols.get("insecure_ciphers"),
        "known_issues": {
            k: protocols.get(k)
            for k in ("heartbleed", "poodle", "beast", "crime", "freak", "logjam")
            if protocols.get(k)
        },
    }
    return json.dumps({k: v for k, v in summary.items() if v not in (None, [], {})}, default=str)


def _ask_ollama(prompt):
    """Send one prompt to Ollama and return {success, response}.

    Errors are returned rather than raised: both callers put this straight into
    a JSON response, and a model that is still pulling should read as a message
    in the chat rather than a 500.
    """
    try:
        reply = requests.post(
            f"{OLLAMA_URL}/api/generate",
            json={
                "model": OLLAMA_MODEL,
                "system": SYSTEM_PROMPT,
                "prompt": prompt,
                "stream": False,
                # Without this the model is evicted after 5 idle minutes and the
                # next question pays the reload again.
                "keep_alive": OLLAMA_KEEP_ALIVE,
                "options": {"num_predict": OLLAMA_MAX_TOKENS},
            },
            timeout=OLLAMA_TIMEOUT,
        )
    except requests.exceptions.ConnectionError:
        return {
            "success": False,
            "response": f"Cannot reach the local AI model at {OLLAMA_URL}. "
                        "Check that the redflags-ollama container is running.",
        }
    except requests.exceptions.Timeout:
        return {
            "success": False,
            "response": f"The local AI model did not answer within {OLLAMA_TIMEOUT}s. "
                        "It may still be loading the model on first use.",
        }
    except Exception as exc:  # noqa: BLE001 - surfaced to the caller as text
        return {"success": False, "response": str(exc)}

    if reply.status_code != 200:
        return {
            "success": False,
            "response": f"The local AI model returned HTTP {reply.status_code}: {reply.text[:200]}",
        }

    return {"success": True, "response": reply.json().get("response", "").strip()}


def get_ai_overview(question, host_data):
    prompt = (
        "Having these server data in json format extracted using various security tools\n"
        + _summarise_host_data(host_data)
        + "\n can you answer this question considering the user does not have"
          " cybersecurity expertise ? Only fields the scan actually collected are"
          " included, so do not comment on missing or null data. Answer only from"
          " the data above. Question:" + question
    )
    return _ask_ollama(prompt)


def analyze_with_ai(headers, cert, cipher, heartbleed):
    prompt = f"""
                    You are a cybersecurity analyst. Given the following server security information (in JSON format), perform a detailed yet simple-to-understand analysis of potential vulnerabilities and misconfigurations. The audience does not have a strong background in cybersecurity, so keep explanations clear, concise, and jargon-free where possible.

                    Focus your analysis on:
                    1. SSL/TLS Configuration – Are the protocols and ciphers strong and modern?
                    2. Certificate Validity – Is the certificate valid and trustworthy?
                    3. HTTP Headers – Are the right security headers present and properly configured?
                    4. Server Software and Known Vulnerabilities – Is any outdated or vulnerable software detected?
                    5. General Security Hygiene – Any obvious risks or poor practices?

                    Please:
                    - Explain what each finding means in plain language.
                    - Highlight any critical risks or attack vectors (e.g., MITM attacks, weak encryption, missing headers).
                    - Suggest clear, prioritized, and actionable steps to fix or mitigate each issue.

                    Here are the data collected from the server in json format:
                    - Certificate Info:
                    {json.dumps(cert, default=str, indent=2)}

                    - HTTP Headers:
                    {json.dumps(headers, default=str, indent=2)}

                    - TLS/SSL Cipher & Protocol Info:
                    {json.dumps(cipher, default=str, indent=2)}

                    - Heartbleed Test Result:
                    {json.dumps(heartbleed, default=str, indent=2)}

                    Provide only the objective security report based on the given data and do not include conversational phrases.
                    """
    return _ask_ollama(prompt)
