'use client';
import { useTheme } from '@/components/docs/ThemeProvider';

export default function RedFlagsPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const h1 = `text-3xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h2 = `text-2xl font-semibold mt-8 mb-3 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const p = `mb-4 leading-relaxed ${isDark ? 'text-gray-300' : 'text-gray-700'}`;
  const code = `${isDark ? 'bg-gray-800 text-green-400' : 'bg-gray-100 text-gray-800'} rounded px-3 py-2 block overflow-x-auto text-sm font-mono my-3 whitespace-pre`;
  const li = `mb-2 ${isDark ? 'text-gray-300' : 'text-gray-700'}`;

  return (
    <div>
      <h1 className={h1}>Red Flags Analysis</h1>
      <p className={p}>
        Red Flags is an AI-powered log anomaly detection system. It uses Ollama LLM models to analyze log files, identify suspicious patterns, and flag potential security incidents that rule-based systems might miss.
      </p>

      <h2 className={h2}>Architecture</h2>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}><strong>API Backend:</strong> Python FastAPI on port 8002 (container: redflags-api)</li>
        <li className={li}><strong>Detector:</strong> Background Python service (container: redflags-detector) that continuously analyzes logs</li>
        <li className={li}><strong>LLM Engine:</strong> Ollama (container: redflags-ollama) running local language models</li>
        <li className={li}><strong>Database:</strong> PostgreSQL (container: redflags-postgres, port 5433)</li>
        <li className={li}><strong>Log Shipping:</strong> Filebeat (container: redflags-filebeat) collects and forwards logs</li>
      </ul>

      <h2 className={h2}>How It Works</h2>
      <ol className="list-decimal pl-6 mb-4">
        <li className={li}>Filebeat collects logs from configured sources</li>
        <li className={li}>The detector service processes logs in batches</li>
        <li className={li}>Each batch is analyzed by the Ollama LLM for anomalies</li>
        <li className={li}>Detected anomalies are stored with severity scores and explanations</li>
        <li className={li}>Results are displayed in the Red Flags dashboard with filtering and search</li>
      </ol>

      <h2 className={h2}>Features</h2>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}>AI-driven anomaly detection beyond static rule matching</li>
        <li className={li}>Natural language explanations of detected issues</li>
        <li className={li}>Severity scoring for prioritization</li>
        <li className={li}>Historical analysis and trend tracking</li>
        <li className={li}>Runs entirely on-premise &mdash; no cloud API calls</li>
      </ul>

      <h2 className={h2}>API</h2>
      <pre className={code}>{`# Health check
GET http://localhost:8002/health

# Get detected anomalies
GET http://localhost:8002/anomalies

# Analyze a specific log entry
POST http://localhost:8002/analyze
Body: { "log": "log entry text" }`}</pre>

      <h2 className={h2}>Usage</h2>
      <p className={p}>Navigate to <strong>CTI &rarr; Red Flags</strong> in the dashboard.</p>
    </div>
  );
}
