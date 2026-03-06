'use client';

import { useTheme } from '@/components/docs/ThemeProvider';

export default function BotnetDetectionPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Botnet Detection (SQS)</h1>

      <p>
        The Botnet Detection module monitors network traffic for Mirai botnet activity,
        DDoS attacks, and malicious HTTP patterns using OpenSearch and Suricata rules.
      </p>

      <h2>Overview</h2>

      <p>
        The SQS (Security Query Service) backend aggregates and queries security events
        stored in OpenSearch indices. It provides a FastAPI-based REST API for the
        dashboard to visualize botnet activity, alert severity, and attack patterns.
      </p>

      <h2>Architecture</h2>

      <ul>
        <li><strong>Suricata</strong> — Generates alerts from network traffic using botnet-specific rules</li>
        <li><strong>Logstash</strong> — Processes and indexes alerts into OpenSearch</li>
        <li><strong>OpenSearch</strong> — Stores and indexes security events for fast querying</li>
        <li><strong>FastAPI Backend</strong> — REST API serving dashboard queries and aggregations</li>
      </ul>

      <h2>OpenSearch Indices</h2>

      <p>The following indices store detection data:</p>
      <ul>
        <li><code>mirai-alerts*</code> — Mirai botnet alert events</li>
        <li><code>mirai-ddos*</code> — DDoS attack detections</li>
        <li><code>mirai-http*</code> — Malicious HTTP traffic from Mirai variants</li>
        <li><code>suricata-*</code> — Raw Suricata IDS alerts</li>
      </ul>

      <h2>Dashboard</h2>

      <p>
        Navigate to <strong>DefSec → Botnet Detection</strong> in the sidebar to view:
      </p>
      <ul>
        <li><strong>Alert Summary</strong> — Total alerts by severity (critical, high, medium)</li>
        <li><strong>DDoS Metrics</strong> — Total bytes, packets, and critical DDoS events</li>
        <li><strong>HTTP Activity</strong> — Malicious HTTP request patterns</li>
        <li><strong>Source/Target Analysis</strong> — Unique attacking and targeted IPs</li>
      </ul>

      <h2>API Endpoints</h2>

      <pre><code>{`# Health check
GET http://localhost:8000/health

# Dashboard summary (last 24h)
GET http://localhost:8000/dashboard/summary

# Alert listing with filters
GET http://localhost:8000/alerts?severity=critical&limit=50

# DDoS event data
GET http://localhost:8000/ddos/events`}</code></pre>

      <h2>Configuration</h2>

      <p>Key configuration parameters:</p>
      <ul>
        <li><strong>OpenSearch</strong> — <code>http://localhost:9200</code> (security plugin disabled)</li>
        <li><strong>FastAPI backend</strong> — Port 8000</li>
        <li><strong>Logstash</strong> — Bridges Kafka topics to OpenSearch indices</li>
      </ul>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-blue-900/20 border border-blue-700' : 'bg-blue-50 border border-blue-200'
      }`}>
        <h3 className="mt-0">Live Data</h3>
        <p className="mb-0">
          The botnet detection engine processes real network traffic from the Suricata
          monitoring pipeline. Data is continuously indexed into OpenSearch and
          available for querying within seconds of detection.
        </p>
      </div>
    </div>
  );
}
