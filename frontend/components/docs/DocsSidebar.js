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
    ],
  },
  {
    title: 'Offensive Security',
    items: [
      { title: 'Network Scanning', href: '/docs/features/network-scanning' },
      { title: 'Web Security', href: '/docs/features/web-security' },
      { title: 'Active Exploitation', href: '/docs/features/exploitation' },
      { title: 'SSL/TLS Analysis', href: '/docs/features/ssl-checker' },
      { title: 'Darkweb Monitoring', href: '/docs/features/darkweb' },
    ],
  },
  {
    title: 'Defensive Security',
    items: [
      { title: 'SIEM Dashboard', href: '/docs/features/siem' },
      { title: 'Data Traffic Monitoring', href: '/docs/features/dtm' },
      { title: 'Anomaly Detection', href: '/docs/features/anomaly-detection' },
      { title: 'Botnet Detection', href: '/docs/features/botnet-detection' },
    ],
  },
  {
    title: 'Cyber Threat Intelligence',
    items: [
      { title: 'VSP Score Prediction', href: '/docs/features/vsp' },
      { title: 'Red Flags Analysis', href: '/docs/features/red-flags' },
    ],
  },
  {
    title: 'API Reference',
    items: [
      { title: 'Overview', href: '/docs/api-reference/overview' },
      { title: 'Pentest API', href: '/docs/api-reference/swagger' },
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
