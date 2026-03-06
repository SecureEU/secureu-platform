'use client';

import { useTheme } from '@/components/docs/ThemeProvider';
import Link from 'next/link';

export default function QuickStartPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Quick Start Guide</h1>

      <p>
        Get up and running with SECUR-EU in just 5 minutes. This guide will walk you through
        creating your first security scan.
      </p>

      <h2>Step 1: Create an Account</h2>

      <p>
        Sign up for a free account at{' '}
        <Link href="/register">the registration page</Link>. You'll need:
      </p>
      <ul>
        <li>A valid email address</li>
        <li>A strong password (8+ characters)</li>
      </ul>

      <h2>Step 2: Add Your First Host</h2>

      <p>
        After logging in, navigate to the <strong>Assets</strong> page and click <strong>Add Host</strong>.
        Enter the IP address or hostname of the system you want to scan.
      </p>

      <pre><code>{`{
  "hostname": "192.168.1.1",
  "description": "Main web server"
}`}</code></pre>

      <h2>Step 3: Run a Network Scan</h2>

      <p>
        Go to the <strong>Scans</strong> page and create a new Nmap scan:
      </p>

      <ol>
        <li>Click <strong>New Scan</strong></li>
        <li>Select <strong>Nmap</strong> as the scan type</li>
        <li>Choose your target host</li>
        <li>Select a scan profile (Quick Scan recommended for first scan)</li>
        <li>Click <strong>Start Scan</strong></li>
      </ol>

      <h2>Step 4: Review Results</h2>

      <p>
        Once the scan completes, you'll see:
      </p>
      <ul>
        <li>Open ports and services</li>
        <li>Operating system detection</li>
        <li>Potential vulnerabilities</li>
        <li>Risk assessment scores</li>
      </ul>

      <h2>Step 5: Get AI Insights</h2>

      <p>
        Use the <strong>AI Assistant</strong> to get recommendations on your scan results.
        Ask questions like:
      </p>
      <ul>
        <li>"What are the biggest risks in my scan?"</li>
        <li>"How do I remediate CVE-2024-XXXX?"</li>
        <li>"Is my web server properly secured?"</li>
      </ul>

      <div className={`mt-8 p-6 rounded-lg ${
        isDark ? 'bg-green-900/20 border border-green-700' : 'bg-green-50 border border-green-200'
      }`}>
        <h3 className="mt-0">Congratulations!</h3>
        <p className="mb-0">
          You've completed your first security scan with SECUR-EU. Continue exploring
          the platform with our{' '}
          <Link href="/docs/user-guide/dashboard">Dashboard Guide</Link>.
        </p>
      </div>
    </div>
  );
}
