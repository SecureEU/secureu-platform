'use client';

import { useTheme } from '@/components/docs/ThemeProvider';

export default function DtmPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Data Traffic Monitoring (DTM)</h1>

      <p>
        SECUR-EU integrates a real-time network traffic monitoring pipeline built on
        Suricata IDS, Logstash, and Apache Kafka for continuous threat detection.
      </p>

      <h2>Overview</h2>

      <p>
        The DTM module captures live network traffic, runs it through Suricata's
        rule-based intrusion detection engine, and streams structured alerts into
        Kafka topics for downstream analysis by the Anomaly Detection module.
      </p>

      <h2>Architecture</h2>

      <p>The monitoring pipeline consists of:</p>
      <ul>
        <li><strong>Suricata</strong> — Network IDS/IPS capturing packets and generating alerts</li>
        <li><strong>Tshark</strong> — Packet capture and protocol dissection</li>
        <li><strong>Logstash</strong> — Log processing and forwarding to Kafka</li>
        <li><strong>Kafka</strong> — Message broker for streaming alert data</li>
        <li><strong>PostgreSQL (Sphinx)</strong> — Persistent storage for traffic metadata and configurations</li>
      </ul>

      <h2>Kafka Topics</h2>

      <p>DTM publishes to the following Kafka topics:</p>
      <ul>
        <li><code>dtm-package</code> — Parsed network package data</li>
        <li><code>ad-alert</code> — Anomaly detection alerts</li>
        <li><code>ad-nfstream</code> — Network flow stream data</li>
        <li><code>ad-hogzilla</code> — Hogzilla IDS integration data</li>
      </ul>

      <h2>Dashboard</h2>

      <p>
        The DTM dashboard provides real-time visibility into network traffic patterns,
        active alerts, and protocol distribution. Navigate to{' '}
        <strong>DefSec → Data Traffic &amp; Anomaly Detection</strong> in the sidebar.
      </p>

      <h2>API Endpoints</h2>

      <pre><code>{`# Health check
GET http://localhost:8087/sphinx/dtm/actuator/health

# DTM REST API base
GET http://localhost:8087/sphinx/dtm/`}</code></pre>

      <h2>Configuration</h2>

      <p>Key configuration parameters:</p>
      <ul>
        <li><strong>Database</strong> — PostgreSQL on port 8432, schema <code>sphinx</code></li>
        <li><strong>Kafka broker</strong> — <code>localhost:9092</code></li>
        <li><strong>Logstash</strong> — Configured to skip local traffic by default (<code>skipLocal=true</code>)</li>
      </ul>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-blue-900/20 border border-blue-700' : 'bg-blue-50 border border-blue-200'
      }`}>
        <h3 className="mt-0">Note</h3>
        <p className="mb-0">
          DTM must start before the Anomaly Detection module, as it owns the
          Liquibase database migrations for the shared <code>sphinx</code> schema.
        </p>
      </div>
    </div>
  );
}
