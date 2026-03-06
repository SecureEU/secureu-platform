'use client';

import { useTheme } from '@/components/docs/ThemeProvider';

export default function AnomalyDetectionPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Anomaly Detection (AD)</h1>

      <p>
        The Anomaly Detection module uses Apache Spark and machine learning algorithms
        to identify unusual network behavior patterns from traffic data streamed via Kafka.
      </p>

      <h2>Overview</h2>

      <p>
        AD consumes network flow data from DTM's Kafka topics, applies statistical and
        ML-based analysis, and generates alerts when anomalous patterns are detected.
        It integrates with HBase for reputation scoring and historical data storage.
      </p>

      <h2>Capabilities</h2>

      <ul>
        <li><strong>Flow Analysis</strong> — Monitors network flow characteristics (packet sizes, durations, protocols)</li>
        <li><strong>Reputation Scoring</strong> — Maintains IP reputation tables based on observed behavior</li>
        <li><strong>Alert Generation</strong> — Produces structured alerts for anomalous traffic</li>
        <li><strong>Kafka Integration</strong> — Consumes from and produces to multiple Kafka topics</li>
      </ul>

      <h2>Kafka Consumers</h2>

      <p>AD subscribes to the following Kafka partitions:</p>
      <ul>
        <li><code>dtm-package</code> — Network package data from DTM</li>
        <li><code>ad-alert</code> — Alert correlation data</li>
        <li><code>ad-nfstream</code> — NFStream network flow data</li>
        <li><code>ad-hogzilla</code> — Hogzilla IDS data</li>
      </ul>

      <h2>Dashboard</h2>

      <p>
        Anomaly detection results are displayed alongside DTM data in the{' '}
        <strong>DefSec → Data Traffic &amp; Anomaly Detection</strong> dashboard,
        showing detected anomalies, reputation scores, and trend analysis.
      </p>

      <h2>API Endpoints</h2>

      <pre><code>{`# Health check
GET http://localhost:5001/sphinx/ad/actuator/health

# AD REST API base
GET http://localhost:5001/sphinx/ad/`}</code></pre>

      <h2>Configuration</h2>

      <p>Key configuration parameters:</p>
      <ul>
        <li><strong>Database</strong> — PostgreSQL on port 8432, schema <code>sphinx</code> (shared with DTM)</li>
        <li><strong>Kafka broker</strong> — <code>localhost:9092</code></li>
        <li><strong>Spark UI</strong> — Available at <code>http://localhost:4040</code> when running</li>
      </ul>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-yellow-900/20 border border-yellow-700' : 'bg-yellow-50 border border-yellow-200'
      }`}>
        <h3 className="mt-0">Startup Order</h3>
        <p className="mb-0">
          AD must start after DTM is fully ready. The startup script waits for DTM's
          health endpoint before launching AD to avoid Liquibase lock conflicts on the
          shared database schema.
        </p>
      </div>
    </div>
  );
}
