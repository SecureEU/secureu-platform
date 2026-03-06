'use client';

import { useTheme } from '@/components/docs/ThemeProvider';

export default function WebSecurityPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Web Security Testing</h1>

      <p>
        SECUR-EU integrates OWASP ZAP (Zed Attack Proxy) for comprehensive web application
        security testing and vulnerability scanning.
      </p>

      <h2>Overview</h2>

      <p>
        Web security testing helps identify vulnerabilities in your web applications before
        attackers can exploit them. ZAP tests for the OWASP Top 10 vulnerabilities and more.
      </p>

      <h2>Scan Types</h2>

      <h3>Spider Scan</h3>
      <p>
        Crawls your web application to discover all pages, forms, and endpoints.
      </p>

      <h3>Active Scan</h3>
      <p>
        Actively probes discovered endpoints for vulnerabilities like XSS, SQL injection,
        and more.
      </p>

      <h3>Passive Scan</h3>
      <p>
        Analyzes responses during crawling without sending attack payloads.
      </p>

      <h2>Vulnerabilities Detected</h2>

      <ul>
        <li><strong>SQL Injection</strong> - Database manipulation attacks</li>
        <li><strong>Cross-Site Scripting (XSS)</strong> - Script injection vulnerabilities</li>
        <li><strong>Cross-Site Request Forgery (CSRF)</strong> - Unauthorized actions</li>
        <li><strong>Insecure Direct Object References</strong> - Access control flaws</li>
        <li><strong>Security Misconfiguration</strong> - Server and app misconfigurations</li>
        <li><strong>Sensitive Data Exposure</strong> - Unprotected sensitive information</li>
        <li><strong>Broken Authentication</strong> - Session and auth weaknesses</li>
      </ul>

      <h2>Running a Web Scan</h2>

      <ol>
        <li>Navigate to <strong>Scans</strong></li>
        <li>Click <strong>New Scan</strong></li>
        <li>Select <strong>ZAP</strong> as the scan type</li>
        <li>Enter your web application URL</li>
        <li>Choose scan options (Spider, Active, Passive)</li>
        <li>Click <strong>Start Scan</strong></li>
      </ol>

      <h2>API Usage</h2>

      <pre><code>{`POST /api/v1/scans/zap
Content-Type: application/json
Authorization: Bearer <token>

{
  "target": "https://example.com",
  "spider": true,
  "active": true,
  "passive": true
}`}</code></pre>

      <h2>Risk Levels</h2>

      <table>
        <thead>
          <tr>
            <th>Level</th>
            <th>Description</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><span className="text-red-500 font-bold">High</span></td>
            <td>Critical vulnerabilities</td>
            <td>Immediate remediation required</td>
          </tr>
          <tr>
            <td><span className="text-orange-500 font-bold">Medium</span></td>
            <td>Significant vulnerabilities</td>
            <td>Plan remediation soon</td>
          </tr>
          <tr>
            <td><span className="text-yellow-500 font-bold">Low</span></td>
            <td>Minor vulnerabilities</td>
            <td>Consider fixing</td>
          </tr>
          <tr>
            <td><span className="text-blue-500 font-bold">Info</span></td>
            <td>Informational findings</td>
            <td>Review and assess</td>
          </tr>
        </tbody>
      </table>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-blue-900/20 border border-blue-700' : 'bg-blue-50 border border-blue-200'
      }`}>
        <h3 className="mt-0">Pro Tip</h3>
        <p className="mb-0">
          Run passive scans first to understand your application structure, then
          follow up with active scans during maintenance windows to avoid disruption.
        </p>
      </div>
    </div>
  );
}
