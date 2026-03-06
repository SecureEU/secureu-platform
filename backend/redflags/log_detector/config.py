import os

# ====================== Ollama Configuration ========================= #
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "redflags-ollama")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2:latest")
OLLAMA_TIMEOUT = int(os.getenv("OLLAMA_TIMEOUT", "60"))
OLLAMA_TEMPERATURE = float(os.getenv("OLLAMA_TEMPERATURE", "0.1"))

# ==================== PostgreSQL Configuration ======================== #
POSTGRES_HOST = os.getenv("POSTGRES_HOST", "postgres")
POSTGRES_PORT = os.getenv("POSTGRES_PORT", "5432")
POSTGRES_DB = os.getenv("POSTGRES_DB", "security_inc")
POSTGRES_USER = os.getenv("POSTGRES_USER", "user123")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "password123")

# ==================== Log Preprocessing Configuration ================== #
LOG_DIR = os.getenv("LOG_INPUT_PATH", "/aggregated_logs")
LOG_INPUT_PATH = os.getenv("LOG_INPUT_PATH", "/aggregated_logs")
LOG_FILENAME = os.getenv("LOG_FILENAME", "")
POLL_INTERVAL = int(os.getenv("POLL_INTERVAL", "10"))
RATE_LIMIT_DELAY = float(os.getenv("RATE_LIMIT_DELAY", "1.0"))

# ========================= LLM Prompt ================================= #
SURICATA_ALERT_PROMPT = """You are an expert network security analyst. Analyze this Suricata IDS alert and provide a structured security assessment.

{alert_summary}

SEVERITY LEVELS:
- CRITICAL: Active exploitation, command & control, data exfiltration, privilege escalation
- HIGH: Exploit attempts, malware communication, brute force attacks, known CVE exploitation
- MEDIUM: Suspicious reconnaissance, policy violations, potentially unwanted traffic
- LOW: Informational alerts, DNS queries to known tracking domains, minor policy events
- INFO: Normal network activity flagged by broad rules

INSTRUCTIONS:
1. Assess the real-world threat level of this alert
2. Consider if this is a true positive or likely false positive
3. Provide actionable context about what this alert means
4. Return ONLY valid JSON (no markdown, no code blocks)

OUTPUT FORMAT (valid JSON only):
{{
  "timestamp": "ISO 8601 timestamp from the alert",
  "source_ip": "source IP",
  "dest_ip": "destination IP",
  "protocol": "protocol",
  "event_type": "alert category",
  "severity": "CRITICAL|HIGH|MEDIUM|LOW|INFO",
  "description": "1-2 sentence security assessment with actionable context",
  "signature": "the alert signature name",
  "category": "the alert category",
  "is_anomaly": true/false,
  "confidence": 0.0-1.0
}}

REMEMBER: Return ONLY the JSON object, nothing else."""
