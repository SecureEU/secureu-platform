'use client';

import { useTheme } from '@/components/docs/ThemeProvider';
import Link from 'next/link';

export default function APIOverviewPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>API Reference</h1>

      <p>
        The SECUR-EU API provides programmatic access to all platform features.
        Build integrations, automate scans, and retrieve vulnerability data.
      </p>

      <h2>Base URL</h2>

      <pre><code>http://localhost:3001/api/v1</code></pre>

      <h2>Authentication</h2>

      <p>
        All API requests require authentication using JWT tokens. Include the token
        in the Authorization header:
      </p>

      <pre><code>{`Authorization: Bearer <your_access_token>`}</code></pre>

      <p>
        See the <Link href="/docs/api-reference/authentication">Authentication</Link> guide
        for details on obtaining tokens.
      </p>

      <h2>Available Endpoints</h2>

      <h3>Authentication</h3>
      <table>
        <thead>
          <tr>
            <th>Method</th>
            <th>Endpoint</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><code>POST</code></td>
            <td>/auth/register</td>
            <td>Create new account</td>
          </tr>
          <tr>
            <td><code>POST</code></td>
            <td>/auth/login</td>
            <td>Login and get tokens</td>
          </tr>
          <tr>
            <td><code>POST</code></td>
            <td>/auth/refresh</td>
            <td>Refresh access token</td>
          </tr>
          <tr>
            <td><code>POST</code></td>
            <td>/auth/logout</td>
            <td>Invalidate tokens</td>
          </tr>
        </tbody>
      </table>

      <h3>Scans</h3>
      <table>
        <thead>
          <tr>
            <th>Method</th>
            <th>Endpoint</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><code>GET</code></td>
            <td>/scans</td>
            <td>List all scans</td>
          </tr>
          <tr>
            <td><code>POST</code></td>
            <td>/scans/nmap</td>
            <td>Start Nmap scan</td>
          </tr>
          <tr>
            <td><code>POST</code></td>
            <td>/scans/zap</td>
            <td>Start ZAP scan</td>
          </tr>
          <tr>
            <td><code>GET</code></td>
            <td>/scans/:id</td>
            <td>Get scan details</td>
          </tr>
          <tr>
            <td><code>DELETE</code></td>
            <td>/scans/:id</td>
            <td>Delete scan</td>
          </tr>
        </tbody>
      </table>

      <h3>Hosts</h3>
      <table>
        <thead>
          <tr>
            <th>Method</th>
            <th>Endpoint</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><code>GET</code></td>
            <td>/hosts</td>
            <td>List all hosts</td>
          </tr>
          <tr>
            <td><code>POST</code></td>
            <td>/hosts</td>
            <td>Add new host</td>
          </tr>
          <tr>
            <td><code>GET</code></td>
            <td>/hosts/:id</td>
            <td>Get host details</td>
          </tr>
          <tr>
            <td><code>PUT</code></td>
            <td>/hosts/:id</td>
            <td>Update host</td>
          </tr>
          <tr>
            <td><code>DELETE</code></td>
            <td>/hosts/:id</td>
            <td>Delete host</td>
          </tr>
        </tbody>
      </table>

      <h2>Response Format</h2>

      <p>All responses are JSON formatted:</p>

      <pre><code>{`{
  "success": true,
  "data": { ... },
  "message": "Operation successful"
}`}</code></pre>

      <h2>Error Handling</h2>

      <p>Errors return appropriate HTTP status codes with details:</p>

      <pre><code>{`{
  "success": false,
  "error": "Invalid credentials",
  "code": "AUTH_INVALID_CREDENTIALS"
}`}</code></pre>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-gray-900 border border-gray-700' : 'bg-gray-50 border border-gray-200'
      }`}>
        <h3 className="mt-0">Interactive API Docs</h3>
        <p className="mb-0">
          Try the API interactively with our{' '}
          <Link href="/docs/api-reference/swagger">Swagger UI</Link>.
        </p>
      </div>
    </div>
  );
}
