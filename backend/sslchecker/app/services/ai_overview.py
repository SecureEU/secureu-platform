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
    """Condense a scan row into the facts worth reasoning about.

    The whole row used to go into the prompt verbatim. Measured on the test VM
    that was 1650 tokens, and prompt evaluation runs at ~18 tok/s on CPU, so 89
    of the 155 seconds a question took were spent reading the prompt before the
    model wrote a word. The bulk is header_values and protocols - every cipher
    of every TLS version, and the full text of each security header - which the
    answer never needs. Which protocols, which headers, and the weak-cipher and
    known-issue flags are enough.

    get_scan() returns a flat sqlite row; a couple of its columns hold JSON as
    text. Anything unexpected falls back to dumping the row as before, so a
    schema change degrades to the old behaviour rather than an empty prompt.
    """
    try:
        row = dict(host_data)
    except (TypeError, ValueError):
        return json.dumps(host_data, default=str)

    if "host" not in row and "ssl_version" not in row:
        return json.dumps(host_data, default=str)

    def as_json(value):
        if isinstance(value, str):
            try:
                return json.loads(value)
            except (ValueError, TypeError):
                return {}
        return value if isinstance(value, (dict, list)) else {}

    headers = as_json(row.get("header_values"))
    protocols = as_json(row.get("protocols"))

    versions, cipher_count = [], 0
    if isinstance(protocols, dict):
        versions = sorted(k for k, v in protocols.items() if v)
        cipher_count = sum(len(v) for v in protocols.values() if isinstance(v, list))

    present, missing = [], []
    if isinstance(headers, dict):
        for name, value in headers.items():
            (present if value else missing).append(name)

    summary = {
        "host": row.get("host"),
        "scanned_at": row.get("scan_date"),
        "server": row.get("server_type"),
        "negotiated": {
            "ssl_version": row.get("ssl_version"),
            "cipher": row.get("cipher"),
        },
        "certificate": {
            "issuer": row.get("issuer"),
            "subject": row.get("subject"),
            "key_type": row.get("key_type"),
            "key_size": row.get("key_size"),
            "valid_until": row.get("valid_until"),
            "expires_in_days": row.get("expires_in"),
            "has_expired": bool(row.get("has_expired")),
        },
        "tls_versions_offered": versions,
        "cipher_suites_offered": cipher_count,
        "weak_cipher_count": row.get("weak_ciphers"),
        "legacy_protocols_enabled": sorted(
            name for name, flag in (
                ("SSLv3", row.get("sslv3_supported")),
                ("TLSv1.0", row.get("tls1_supported")),
                ("TLSv1.1", row.get("tls1_1_supported")),
            ) if flag
        ),
        "heartbleed_vulnerable": bool(row.get("heartbleed")),
        "security_headers_present": sorted(present),
        "security_headers_missing": sorted(missing),
    }

    def prune(value):
        if isinstance(value, dict):
            cleaned = {k: prune(v) for k, v in value.items()}
            return {k: v for k, v in cleaned.items() if v not in (None, "", [], {})}
        return value

    return json.dumps(prune(summary), default=str)


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


def _ask_prompt(question, host_data):
    return (
        "Having these server data in json format extracted using various security tools\n"
        + _summarise_host_data(host_data)
        + "\n can you answer this question considering the user does not have"
          " cybersecurity expertise ? Only fields the scan actually collected are"
          " included, so do not comment on missing or null data. Answer only from"
          " the data above. Question:" + question
    )


def get_ai_overview(question, host_data):
    return _ask_ollama(_ask_prompt(question, host_data))


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


def _stream_ollama(prompt):
    """Yield the answer as the model writes it.

    Ollama emits one JSON object per line when stream is true. The caller turns
    each yielded piece into a line of its own, so a browser can render tokens as
    they arrive instead of staring at a spinner for a minute. Errors are yielded
    as text rather than raised: the HTTP status is already sent by then.
    """
    try:
        reply = requests.post(
            f"{OLLAMA_URL}/api/generate",
            json={
                "model": OLLAMA_MODEL,
                "system": SYSTEM_PROMPT,
                "prompt": prompt,
                "stream": True,
                "keep_alive": OLLAMA_KEEP_ALIVE,
                "options": {"num_predict": OLLAMA_MAX_TOKENS},
            },
            timeout=OLLAMA_TIMEOUT,
            stream=True,
        )
    except requests.exceptions.ConnectionError:
        yield False, f"Cannot reach the local AI model at {OLLAMA_URL}. Check that the redflags-ollama container is running."
        return
    except requests.exceptions.Timeout:
        yield False, f"The local AI model did not answer within {OLLAMA_TIMEOUT}s."
        return
    except Exception as exc:  # noqa: BLE001 - surfaced to the caller as text
        yield False, str(exc)
        return

    if reply.status_code != 200:
        yield False, f"The local AI model returned HTTP {reply.status_code}: {reply.text[:200]}"
        return

    try:
        for line in reply.iter_lines(decode_unicode=True):
            if not line:
                continue
            try:
                chunk = json.loads(line)
            except ValueError:
                continue
            piece = chunk.get("response") or ""
            if piece:
                yield True, piece
            if chunk.get("done"):
                return
    except requests.exceptions.RequestException as exc:
        yield False, f"The answer was cut short: {exc}"


def stream_ai_overview(question, host_data):
    return _stream_ollama(_ask_prompt(question, host_data))
