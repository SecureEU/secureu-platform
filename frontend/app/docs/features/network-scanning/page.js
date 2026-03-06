'use client';

import { useTheme } from '@/components/docs/ThemeProvider';
import Link from 'next/link';

export default function NetworkScanningPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Network Scanning</h1>

      <p>
        SECUR-EU provides comprehensive network scanning capabilities powered by Nmap,
        the industry-standard network discovery and security auditing tool.
      </p>

      <h2>Overview</h2>

      <p>
        Network scanning helps you discover hosts, identify open ports, detect services,
        and find potential vulnerabilities in your infrastructure.
      </p>

      <h2>Scan Types</h2>

      <h3>Quick Scan</h3>
      <p>
        Fast scan of the most common 100 ports. Ideal for initial reconnaissance.
      </p>
      <pre><code>nmap -T4 -F target</code></pre>

      <h3>Full Scan</h3>
      <p>
        Comprehensive scan of all 65,535 TCP ports with service detection.
      </p>
      <pre><code>nmap -sV -p- target</code></pre>

      <h3>Vulnerability Scan</h3>
      <p>
        Includes vulnerability detection scripts from the Nmap Scripting Engine (NSE).
      </p>
      <pre><code>nmap -sV --script=vuln target</code></pre>

      <h3>Stealth Scan</h3>
      <p>
        SYN scan that doesn't complete TCP handshakes, reducing detection risk.
      </p>
      <pre><code>nmap -sS target</code></pre>

      <h2>Running a Scan</h2>

      <ol>
        <li>Navigate to <strong>Scans</strong> in the sidebar</li>
        <li>Click <strong>New Scan</strong></li>
        <li>Select <strong>Nmap</strong> as the scan type</li>
        <li>Enter target IP address or CIDR range</li>
        <li>Choose a scan profile</li>
        <li>Click <strong>Start Scan</strong></li>
      </ol>

      <h2>Scan Results</h2>

      <p>After a scan completes, you'll see:</p>

      <ul>
        <li><strong>Host Discovery</strong> - List of live hosts found</li>
        <li><strong>Port Status</strong> - Open, closed, and filtered ports</li>
        <li><strong>Service Detection</strong> - Identified services and versions</li>
        <li><strong>OS Detection</strong> - Operating system fingerprinting</li>
        <li><strong>Vulnerabilities</strong> - CVEs and security issues</li>
      </ul>

      <h2>API Usage</h2>

      <p>Start a network scan via the API:</p>

      <pre><code>{`POST /api/v1/scans/nmap
Content-Type: application/json
Authorization: Bearer <token>

{
  "target": "192.168.1.0/24",
  "profile": "quick",
  "ports": "1-1000"
}`}</code></pre>

      <h2>Best Practices</h2>

      <ul>
        <li>Always get proper authorization before scanning</li>
        <li>Start with quick scans to avoid overwhelming networks</li>
        <li>Schedule scans during off-peak hours</li>
        <li>Use stealth scans for sensitive environments</li>
        <li>Review and act on critical findings promptly</li>
      </ul>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-yellow-900/20 border border-yellow-700' : 'bg-yellow-50 border border-yellow-200'
      }`}>
        <h3 className="mt-0">Legal Notice</h3>
        <p className="mb-0">
          Only scan systems you own or have explicit permission to test.
          Unauthorized scanning may be illegal in your jurisdiction.
        </p>
      </div>
    </div>
  );
}
