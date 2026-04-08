'use client';

import Link from 'next/link';
import { useTheme } from '@/components/docs/ThemeProvider';

export default function DocsPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  const sections = [
    {
      title: 'Getting Started',
      description: 'Learn how to set up and start using SECUR-EU',
      href: '/docs/getting-started/introduction',
      category: 'General',
    },
    {
      title: 'Network Scanning',
      description: 'Network discovery and service detection with Nmap',
      href: '/docs/features/network-scanning',
      category: 'Offensive Security',
    },
    {
      title: 'Web Application Security',
      description: 'OWASP ZAP vulnerability scanning for web applications',
      href: '/docs/features/web-security',
      category: 'Offensive Security',
    },
    {
      title: 'Active Exploitation',
      description: 'Penetration testing with Metasploit framework',
      href: '/docs/features/exploitation',
      category: 'Offensive Security',
    },
    {
      title: 'SSL/TLS Analysis',
      description: 'Certificate validation and encryption assessment',
      href: '/docs/features/ssl-checker',
      category: 'Offensive Security',
    },
    {
      title: 'Darkweb Monitoring',
      description: 'Search dark web for leaked credentials and data breaches',
      href: '/docs/features/darkweb',
      category: 'Offensive Security',
    },
    {
      title: 'SIEM Dashboard',
      description: 'Host-based intrusion detection with SEUXDR agents and Wazuh',
      href: '/docs/features/siem',
      category: 'Defensive Security',
    },
    {
      title: 'Data Traffic Monitoring',
      description: 'Real-time network traffic analysis with Suricata IDS',
      href: '/docs/features/dtm',
      category: 'Defensive Security',
    },
    {
      title: 'Anomaly Detection',
      description: 'ML-based anomaly detection on network flows via Apache Spark',
      href: '/docs/features/anomaly-detection',
      category: 'Defensive Security',
    },
    {
      title: 'Botnet Detection',
      description: 'Mirai botnet and DDoS detection powered by OpenSearch',
      href: '/docs/features/botnet-detection',
      category: 'Defensive Security',
    },
    {
      title: 'VSP Score Prediction',
      description: 'ML-powered CVSS vulnerability score prediction from descriptions',
      href: '/docs/features/vsp',
      category: 'Cyber Threat Intelligence',
    },
    {
      title: 'Red Flags Analysis',
      description: 'AI-powered log anomaly detection with LLM analysis',
      href: '/docs/features/red-flags',
      category: 'Cyber Threat Intelligence',
    },
    {
      title: 'API Reference',
      description: 'Complete API documentation for all backend services',
      href: '/docs/api-reference/overview',
      category: 'Developer',
    },
  ];

  return (
    <div>
      <h1 className={`text-4xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`}>
        SECUR-EU Documentation
      </h1>
      <p className={`text-xl mb-8 ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
        Complete documentation for the SECUR-EU security operations platform covering offensive testing,
        defensive monitoring, and cyber threat intelligence.
      </p>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-5 mt-8">
        {sections.map((section) => {
          const categoryColors = {
            'General': 'bg-blue-100 text-blue-700',
            'Offensive Security': 'bg-red-100 text-red-700',
            'Defensive Security': 'bg-green-100 text-green-700',
            'Cyber Threat Intelligence': 'bg-purple-100 text-purple-700',
            'Developer': 'bg-gray-100 text-gray-700',
          };
          return (
            <Link
              key={section.href}
              href={section.href}
              className={`block p-5 border rounded-lg transition-all group ${
                isDark
                  ? 'bg-gray-900 border-gray-700 hover:border-blue-500 hover:bg-gray-800'
                  : 'bg-gray-50 border-gray-200 hover:border-blue-500 hover:bg-gray-100'
              }`}
            >
              <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full mb-2 ${categoryColors[section.category] || ''}`}>
                {section.category}
              </span>
              <h2 className={`text-lg font-semibold transition-colors group-hover:text-blue-500 ${
                isDark ? 'text-white' : 'text-gray-900'
              }`}>
                {section.title}
              </h2>
              <p className={`mt-1 text-sm ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
                {section.description}
              </p>
            </Link>
          );
        })}
      </div>

      <div className={`mt-12 p-6 rounded-lg ${
        isDark
          ? 'bg-blue-900/20 border border-blue-700'
          : 'bg-blue-50 border border-blue-200'
      }`}>
        <h2 className={`text-xl font-semibold mb-2 ${isDark ? 'text-white' : 'text-gray-900'}`}>
          Quick Links
        </h2>
        <div className="flex flex-wrap gap-4">
          <Link href="/docs/getting-started/quick-start" className="text-blue-500 hover:text-blue-400">
            Quick Start Guide →
          </Link>
          <Link href="/docs/features/siem" className="text-blue-500 hover:text-blue-400">
            SIEM Setup →
          </Link>
          <Link href="/docs/api-reference/overview" className="text-blue-500 hover:text-blue-400">
            API Reference →
          </Link>
        </div>
      </div>
    </div>
  );
}
