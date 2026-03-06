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
      icon: '🚀',
    },
    {
      title: 'Network Scanning',
      description: 'Comprehensive network discovery with Nmap integration',
      href: '/docs/features/network-scanning',
      icon: '🌐',
    },
    {
      title: 'Web Security',
      description: 'Automated web vulnerability scanning with OWASP ZAP',
      href: '/docs/features/web-security',
      icon: '🔍',
    },
    {
      title: 'Exploitation',
      description: 'Controlled penetration testing with Metasploit',
      href: '/docs/features/exploitation',
      icon: '🎯',
    },
    {
      title: 'Data Traffic Monitoring',
      description: 'Real-time network traffic analysis with Suricata IDS',
      href: '/docs/features/dtm',
      icon: '📡',
    },
    {
      title: 'Anomaly Detection',
      description: 'ML-based anomaly detection on network flows via Apache Spark',
      href: '/docs/features/anomaly-detection',
      icon: '🧠',
    },
    {
      title: 'Botnet Detection',
      description: 'Mirai botnet and DDoS detection powered by OpenSearch',
      href: '/docs/features/botnet-detection',
      icon: '🛡️',
    },
    {
      title: 'API Reference',
      description: 'Complete API documentation for developers',
      href: '/docs/api-reference/overview',
      icon: '📚',
    },
  ];

  return (
    <div>
      <h1 className={`text-4xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`}>
        SECUR-EU Documentation
      </h1>
      <p className={`text-xl mb-8 ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
        Learn how to secure your infrastructure with comprehensive vulnerability scanning,
        penetration testing, and compliance reporting.
      </p>

      <div className="grid md:grid-cols-2 gap-6 mt-8">
        {sections.map((section) => (
          <Link
            key={section.href}
            href={section.href}
            className={`block p-6 border rounded-lg transition-all group ${
              isDark
                ? 'bg-gray-900 border-gray-700 hover:border-blue-500 hover:bg-gray-800'
                : 'bg-gray-50 border-gray-200 hover:border-blue-500 hover:bg-gray-100'
            }`}
          >
            <div className="flex items-start gap-4">
              <span className="text-3xl">{section.icon}</span>
              <div>
                <h2 className={`text-xl font-semibold transition-colors group-hover:text-blue-500 ${
                  isDark ? 'text-white' : 'text-gray-900'
                }`}>
                  {section.title}
                </h2>
                <p className={`mt-1 ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
                  {section.description}
                </p>
              </div>
            </div>
          </Link>
        ))}
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
            5-Minute Quick Start →
          </Link>
          <Link href="/docs/api-reference/overview" className="text-blue-500 hover:text-blue-400">
            API Reference →
          </Link>
          <Link href="/docs/features/compliance" className="text-blue-500 hover:text-blue-400">
            Compliance Reports →
          </Link>
        </div>
      </div>
    </div>
  );
}
