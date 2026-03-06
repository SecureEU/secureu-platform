'use client';

import { useTheme } from '@/components/docs/ThemeProvider';
import Link from 'next/link';

export default function IntroductionPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Introduction to SECUR-EU</h1>

      <p>
        SECUR-EU is a comprehensive security operations platform designed to help security teams
        discover vulnerabilities, perform penetration testing, and monitor their infrastructure
        for threats.
      </p>

      <h2>What is SECUR-EU?</h2>

      <p>
        SECUR-EU (Enhancing Security of European SMEs) provides a unified interface for:
      </p>

      <ul>
        <li><strong>Network Scanning</strong> - Discover hosts, open ports, and services using Nmap</li>
        <li><strong>Web Security Testing</strong> - Automated vulnerability scanning with OWASP ZAP</li>
        <li><strong>Penetration Testing</strong> - Controlled exploitation with Metasploit integration</li>
        <li><strong>AI-Powered Analysis</strong> - Get intelligent insights and recommendations</li>
        <li><strong>Compliance Reporting</strong> - Generate reports for PCI-DSS, HIPAA, and SOC2</li>
      </ul>

      <h2>Key Features</h2>

      <h3>Integrated Security Tools</h3>
      <p>
        SECUR-EU integrates industry-leading security tools into a single platform:
      </p>
      <ul>
        <li><strong>Nmap</strong> - Network discovery and security auditing</li>
        <li><strong>OWASP ZAP</strong> - Web application security scanner</li>
        <li><strong>Metasploit</strong> - Penetration testing framework</li>
        <li><strong>Ollama</strong> - Local AI for security analysis</li>
      </ul>

      <h3>Organization Management</h3>
      <p>
        Collaborate with your team using organizations. Assign roles, share scan results,
        and manage access to security data across your team.
      </p>

      <h3>Real-time Monitoring</h3>
      <p>
        Monitor scan progress in real-time, receive alerts for critical vulnerabilities,
        and track remediation efforts through the dashboard.
      </p>

      <h2>Getting Started</h2>

      <p>
        Ready to secure your infrastructure? Follow our{' '}
        <Link href="/docs/getting-started/quick-start">Quick Start Guide</Link>{' '}
        to get up and running.
      </p>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-gray-900 border border-gray-700' : 'bg-gray-50 border border-gray-200'
      }`}>
        <h3 className="mt-0">Next Steps</h3>
        <ul>
          <li><Link href="/docs/getting-started/quick-start">Quick Start Guide</Link></li>
          <li><Link href="/docs/getting-started/installation">Installation</Link></li>
          <li><Link href="/docs/features/network-scanning">Network Scanning</Link></li>
        </ul>
      </div>
    </div>
  );
}
