'use client';
import { useTheme } from '@/components/docs/ThemeProvider';

export default function SSLCheckerPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const h1 = `text-3xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h2 = `text-2xl font-semibold mt-8 mb-3 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const p = `mb-4 leading-relaxed ${isDark ? 'text-gray-300' : 'text-gray-700'}`;
  const code = `${isDark ? 'bg-gray-800 text-green-400' : 'bg-gray-100 text-gray-800'} rounded px-3 py-2 block overflow-x-auto text-sm font-mono my-3 whitespace-pre`;
  const li = `mb-2 ${isDark ? 'text-gray-300' : 'text-gray-700'}`;

  return (
    <div>
      <h1 className={h1}>SSL/TLS Analysis</h1>
      <p className={p}>
        The SSL Checker module validates SSL/TLS certificates and assesses the encryption configuration of your web services. It identifies expired certificates, weak cipher suites, and misconfigured HTTPS deployments.
      </p>

      <h2 className={h2}>Backend Service</h2>
      <p className={p}>The SSL Checker runs as a Python FastAPI backend on <strong>port 5000</strong>.</p>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}><strong>Container:</strong> sslchecker-backend</li>
        <li className={li}><strong>Health check:</strong> <code>http://localhost:5000/</code></li>
      </ul>

      <h2 className={h2}>Features</h2>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}>Certificate validity checking (expiration dates, issuer chain)</li>
        <li className={li}>Cipher suite analysis and grading</li>
        <li className={li}>Protocol version detection (TLS 1.0/1.1/1.2/1.3)</li>
        <li className={li}>Certificate chain validation</li>
        <li className={li}>HSTS header detection</li>
      </ul>

      <h2 className={h2}>Usage</h2>
      <p className={p}>Navigate to <strong>OffSec &rarr; SSL Check</strong> in the dashboard. Enter a domain or IP address to analyze its SSL/TLS configuration. Results include certificate details, expiration dates, cipher suites, and security recommendations.</p>

      <h2 className={h2}>API</h2>
      <pre className={code}>{`# Check SSL certificate for a domain
curl http://localhost:5000/check?domain=example.com`}</pre>
    </div>
  );
}
