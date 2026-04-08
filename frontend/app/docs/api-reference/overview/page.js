'use client';

import { useTheme } from '@/components/docs/ThemeProvider';

export default function APIOverviewPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const h1 = `text-3xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h2 = `text-2xl font-semibold mt-8 mb-3 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h3 = `text-xl font-semibold mt-6 mb-2 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const p = `mb-4 leading-relaxed ${isDark ? 'text-gray-300' : 'text-gray-700'}`;
  const code = `${isDark ? 'bg-gray-800 text-green-400' : 'bg-gray-100 text-gray-800'} rounded px-3 py-2 block overflow-x-auto text-sm font-mono my-3 whitespace-pre`;
  const thClass = `px-4 py-2 text-left text-sm font-medium ${isDark ? 'text-gray-300 bg-gray-800' : 'text-gray-700 bg-gray-50'}`;
  const tdClass = `px-4 py-2 text-sm ${isDark ? 'text-gray-400 border-gray-700' : 'text-gray-600 border-gray-200'}`;

  return (
    <div>
      <h1 className={h1}>API Reference</h1>
      <p className={p}>
        SECUR-EU exposes multiple backend APIs for scanning, monitoring, and threat intelligence. Each service runs independently and can be accessed directly.
      </p>

      <h2 className={h2}>Backend Services</h2>
      <table className="w-full border-collapse border border-gray-200 rounded-lg overflow-hidden mb-6">
        <thead>
          <tr>
            <th className={thClass}>Service</th>
            <th className={thClass}>Port</th>
            <th className={thClass}>Technology</th>
            <th className={thClass}>Description</th>
          </tr>
        </thead>
        <tbody>
          {[
            ['Pentest Backend', '3001', 'Go + Echo', 'Scans, assets, exploitation, VSP storage'],
            ['SEUXDR Manager', '8443 / 8081', 'Go + Gin', 'SIEM agents, alerts, active response'],
            ['Dark Web', '8001', 'Python FastAPI', 'Tor-based dark web search'],
            ['SSL Checker', '5000', 'Python FastAPI', 'SSL/TLS certificate analysis'],
            ['VSP Predictor', '5002', 'Python FastAPI', 'ML CVSS score prediction'],
            ['Red Flags', '8002', 'Python FastAPI', 'AI log anomaly detection'],
            ['SQS / Botnet', '8000', 'Python FastAPI', 'Botnet detection with OpenSearch'],
            ['DTM', '8087', 'Java Spring Boot', 'Data traffic monitoring'],
            ['Anomaly Detection', '5001', 'Java Spring Boot', 'Network anomaly detection with Spark ML'],
          ].map(([service, port, tech, desc]) => (
            <tr key={service}>
              <td className={`${tdClass} font-medium border`}>{service}</td>
              <td className={`${tdClass} font-mono border`}>{port}</td>
              <td className={`${tdClass} border`}>{tech}</td>
              <td className={`${tdClass} border`}>{desc}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2 className={h2}>Pentest API (port 3001)</h2>
      <p className={p}>The primary API for scan management, asset tracking, exploitation, and VSP predictions.</p>

      <h3 className={h3}>Scans</h3>
      <table className="w-full border-collapse border border-gray-200 rounded-lg overflow-hidden mb-6">
        <thead>
          <tr>
            <th className={thClass}>Method</th>
            <th className={thClass}>Endpoint</th>
            <th className={thClass}>Description</th>
          </tr>
        </thead>
        <tbody>
          {[
            ['GET', '/scans', 'List all scans'],
            ['GET', '/scans/:id', 'Get scan details (parsed nmap + ZAP results)'],
            ['POST', '/scan/create', 'Create a new scan'],
            ['DELETE', '/scans/:id', 'Delete a scan'],
            ['POST', '/nmap/start', 'Start Nmap network scan'],
            ['POST', '/zap/start', 'Start ZAP web application scan'],
            ['POST', '/multi/start', 'Start combined Nmap + ZAP scan'],
          ].map(([method, endpoint, desc]) => (
            <tr key={`${method}-${endpoint}`}>
              <td className={`${tdClass} border`}><code>{method}</code></td>
              <td className={`${tdClass} font-mono border`}>{endpoint}</td>
              <td className={`${tdClass} border`}>{desc}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3 className={h3}>Active Exploitation (Metasploit)</h3>
      <table className="w-full border-collapse border border-gray-200 rounded-lg overflow-hidden mb-6">
        <thead>
          <tr>
            <th className={thClass}>Method</th>
            <th className={thClass}>Endpoint</th>
            <th className={thClass}>Description</th>
          </tr>
        </thead>
        <tbody>
          {[
            ['POST', '/metasploit/create', 'Create exploitation scan (accepts IP or domain)'],
            ['POST', '/metasploit/start', 'Start exploitation scan'],
            ['GET', '/metasploit/results', 'List all exploitation results'],
            ['GET', '/metasploit/results/:id', 'Get exploitation result details'],
            ['DELETE', '/metasploit/:id', 'Delete exploitation scan'],
          ].map(([method, endpoint, desc]) => (
            <tr key={`${method}-${endpoint}`}>
              <td className={`${tdClass} border`}><code>{method}</code></td>
              <td className={`${tdClass} font-mono border`}>{endpoint}</td>
              <td className={`${tdClass} border`}>{desc}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3 className={h3}>VSP Predictions</h3>
      <table className="w-full border-collapse border border-gray-200 rounded-lg overflow-hidden mb-6">
        <thead>
          <tr>
            <th className={thClass}>Method</th>
            <th className={thClass}>Endpoint</th>
            <th className={thClass}>Description</th>
          </tr>
        </thead>
        <tbody>
          {[
            ['POST', '/vsp/predictions', 'Save a CVSS prediction'],
            ['GET', '/vsp/predictions', 'List all saved predictions'],
            ['DELETE', '/vsp/predictions/:id', 'Delete a prediction'],
            ['DELETE', '/vsp/predictions', 'Clear all predictions'],
          ].map(([method, endpoint, desc]) => (
            <tr key={`${method}-${endpoint}`}>
              <td className={`${tdClass} border`}><code>{method}</code></td>
              <td className={`${tdClass} font-mono border`}>{endpoint}</td>
              <td className={`${tdClass} border`}>{desc}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2 className={h2}>SEUXDR Manager API (port 8443)</h2>
      <p className={p}>Accessed via the Next.js proxy at <code>/api/seuxdr?endpoint=...</code></p>
      <table className="w-full border-collapse border border-gray-200 rounded-lg overflow-hidden mb-6">
        <thead>
          <tr>
            <th className={thClass}>Method</th>
            <th className={thClass}>Endpoint</th>
            <th className={thClass}>Description</th>
          </tr>
        </thead>
        <tbody>
          {[
            ['GET', '/api/status', 'Manager health check'],
            ['POST', '/api/orgs', 'List organizations'],
            ['POST', '/api/view/agents', 'List all agents'],
            ['POST', '/api/view/alerts', 'Query SIEM alerts (with org_id, time range)'],
            ['POST', '/api/create/agent', 'Generate agent binary for deployment'],
            ['GET', '/api/download/agent', 'Download generated agent package'],
          ].map(([method, endpoint, desc]) => (
            <tr key={`${method}-${endpoint}`}>
              <td className={`${tdClass} border`}><code>{method}</code></td>
              <td className={`${tdClass} font-mono border`}>{endpoint}</td>
              <td className={`${tdClass} border`}>{desc}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2 className={h2}>Infrastructure</h2>
      <table className="w-full border-collapse border border-gray-200 rounded-lg overflow-hidden mb-6">
        <thead>
          <tr>
            <th className={thClass}>Service</th>
            <th className={thClass}>Port</th>
            <th className={thClass}>Purpose</th>
          </tr>
        </thead>
        <tbody>
          {[
            ['MongoDB', '27017', 'Primary database (pentest scans, VSP predictions, user auth)'],
            ['Mongo Express', '8083', 'MongoDB web admin UI'],
            ['PostgreSQL (Pentest)', '5432', 'Metasploit database'],
            ['PostgreSQL (Sphinx)', '8432', 'DTM and Anomaly Detection database'],
            ['PostgreSQL (Red Flags)', '5433', 'Red Flags anomaly storage'],
            ['OpenSearch (SQS)', '9200', 'Botnet detection and Suricata alerts'],
            ['Kafka', '9092', 'Message broker for DTM pipeline'],
            ['Zookeeper', '2181', 'Kafka coordination'],
          ].map(([service, port, purpose]) => (
            <tr key={service}>
              <td className={`${tdClass} font-medium border`}>{service}</td>
              <td className={`${tdClass} font-mono border`}>{port}</td>
              <td className={`${tdClass} border`}>{purpose}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2 className={h2}>Starting All Services</h2>
      <pre className={code}>{`# Start all backend services
bash secureu-backend/start.sh

# Start the frontend (Next.js)
cd secureu-dashboard && npm run dev

# Stop all backend services
bash secureu-backend/stop.sh`}</pre>
    </div>
  );
}
