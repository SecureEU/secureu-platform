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

SYSTEM_PROMPT = "You are a cybersecurity assistant."


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
        + json.dumps(host_data, default=str)
        + "\n can you answer this question considering the user does not have"
          " cybersecurity expertise ? If many data appear as None it means the tools"
          " could not retrieve information due to issues like SSLv3 or expired"
          " certificate. Question:" + question
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
