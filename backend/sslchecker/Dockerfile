FROM python:3.13-slim

WORKDIR /app

# Install nmap (required for SSL/TLS cipher enumeration)
RUN apt-get update && apt-get install -y --no-install-recommends nmap && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

# Served by gunicorn, not Flask's development server. The old CMD ran
# app.run(debug=True), which serves single-threaded and exposes the Werkzeug
# debugger - a browser-reachable Python console.
#
# One worker, several threads: requests are I/O bound (nmap, the SSL handshake,
# the model) and a single writer process keeps SQLite out of cross-process lock
# contention. --preload runs create_app once in the master so the schema is
# initialised before any worker forks.
#
# The timeout must outlast an AI answer; OLLAMA_TIMEOUT defaults to 180s.
CMD ["gunicorn", "--bind", "0.0.0.0:5000", \
     "--workers", "1", "--threads", "8", "--worker-class", "gthread", \
     "--timeout", "300", "--graceful-timeout", "30", "--preload", \
     "--access-logfile", "-", "--error-logfile", "-", \
     "server:app"]
