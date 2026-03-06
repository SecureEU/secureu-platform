'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState } from 'react';
import { useTheme } from './ThemeProvider';
import { ChevronRight } from 'lucide-react';

const docsNavigation = [
  {
    title: 'Getting Started',
    items: [
      { title: 'Introduction', href: '/docs/getting-started/introduction' },
      { title: 'Quick Start', href: '/docs/getting-started/quick-start' },
      { title: 'Installation', href: '/docs/getting-started/installation' },
      { title: 'Authentication', href: '/docs/getting-started/authentication' },
    ],
  },
  {
    title: 'Features',
    items: [
      { title: 'Network Scanning', href: '/docs/features/network-scanning' },
      { title: 'Web Security', href: '/docs/features/web-security' },
      { title: 'Exploitation', href: '/docs/features/exploitation' },
      { title: 'Data Traffic Monitoring', href: '/docs/features/dtm' },
      { title: 'Anomaly Detection', href: '/docs/features/anomaly-detection' },
      { title: 'Botnet Detection', href: '/docs/features/botnet-detection' },
      { title: 'Compliance', href: '/docs/features/compliance' },
    ],
  },
  {
    title: 'User Guide',
    items: [
      { title: 'Dashboard', href: '/docs/user-guide/dashboard' },
      { title: 'Managing Scans', href: '/docs/user-guide/managing-scans' },
      { title: 'Understanding Results', href: '/docs/user-guide/understanding-results' },
      { title: 'Organizations', href: '/docs/user-guide/organizations' },
      { title: 'Asset Management', href: '/docs/user-guide/asset-management' },
    ],
  },
  {
    title: 'API Reference',
    items: [
      { title: 'Overview', href: '/docs/api-reference/overview' },
      { title: 'Swagger UI', href: '/docs/api-reference/swagger' },
      { title: 'Authentication', href: '/docs/api-reference/authentication' },
      { title: 'Errors', href: '/docs/api-reference/errors' },
      { title: 'Rate Limits', href: '/docs/api-reference/rate-limits' },
    ],
  },
  {
    title: 'API Endpoints',
    items: [
      { title: 'Scans', href: '/docs/api-reference/endpoints/scans' },
      { title: 'Hosts', href: '/docs/api-reference/endpoints/hosts' },
      { title: 'Vulnerabilities', href: '/docs/api-reference/endpoints/vulnerabilities' },
      { title: 'Organizations', href: '/docs/api-reference/endpoints/organizations' },
      { title: 'Users', href: '/docs/api-reference/endpoints/users' },
    ],
  },
  {
    title: 'Integrations',
    items: [
      { title: 'Nmap', href: '/docs/integrations/nmap' },
      { title: 'OWASP ZAP', href: '/docs/integrations/zap' },
      { title: 'Metasploit', href: '/docs/integrations/metasploit' },
    ],
  },
];

export default function DocsSidebar() {
  const pathname = usePathname();
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  const [expandedSections, setExpandedSections] = useState(
    docsNavigation.map((_, i) => i < 2)
  );

  const toggleSection = (index) => {
    setExpandedSections(prev => {
      const newState = [...prev];
      newState[index] = !newState[index];
      return newState;
    });
  };

  return (
    <nav className={`w-64 flex-shrink-0 border-r overflow-y-auto h-[calc(100vh-64px)] sticky top-16 ${
      isDark
        ? 'border-gray-700 bg-gray-900'
        : 'border-gray-200 bg-gray-50'
    }`}>
      <div className="p-4">
        <Link
          href="/docs"
          className={`text-lg font-bold transition-colors ${
            isDark ? 'text-white hover:text-blue-400' : 'text-gray-900 hover:text-blue-600'
          }`}
        >
          Documentation
        </Link>
      </div>
      <div className="px-2 pb-8">
        {docsNavigation.map((section, sectionIndex) => (
          <div key={section.title} className="mb-2">
            <button
              onClick={() => toggleSection(sectionIndex)}
              className={`w-full flex items-center justify-between px-3 py-2 text-sm font-semibold rounded-md transition-colors ${
                isDark
                  ? 'text-gray-300 hover:text-white hover:bg-gray-800'
                  : 'text-gray-700 hover:text-gray-900 hover:bg-gray-200'
              }`}
            >
              {section.title}
              <ChevronRight
                className={`w-4 h-4 transition-transform ${expandedSections[sectionIndex] ? 'rotate-90' : ''}`}
              />
            </button>
            {expandedSections[sectionIndex] && (
              <div className="ml-2 mt-1 space-y-1">
                {section.items.map((item) => {
                  const isActive = pathname === item.href;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`block px-3 py-1.5 text-sm rounded-md transition-colors ${
                        isActive
                          ? 'bg-blue-600 text-white'
                          : isDark
                            ? 'text-gray-400 hover:text-white hover:bg-gray-800'
                            : 'text-gray-600 hover:text-gray-900 hover:bg-gray-200'
                      }`}
                    >
                      {item.title}
                    </Link>
                  );
                })}
              </div>
            )}
          </div>
        ))}
      </div>
    </nav>
  );
}
