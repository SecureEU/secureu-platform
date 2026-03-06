'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useTheme } from './ThemeProvider';
import { Shield, Search, Sun, Moon, Github } from 'lucide-react';

export default function DocsHeader() {
  const [searchQuery, setSearchQuery] = useState('');
  const { theme, toggleTheme } = useTheme();

  const isDark = theme === 'dark';

  return (
    <header className={`h-16 border-b sticky top-0 z-50 ${
      isDark
        ? 'border-gray-700 bg-gray-900'
        : 'border-gray-200 bg-white'
    }`}>
      <div className="h-full px-6 flex items-center justify-between">
        {/* Logo and brand */}
        <div className="flex items-center gap-8">
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
              <Shield className="w-5 h-5 text-white" />
            </div>
            <span className={`font-semibold text-lg ${isDark ? 'text-white' : 'text-gray-900'}`}>
              SECUR-EU
            </span>
          </Link>

          <nav className="hidden md:flex items-center gap-6">
            <Link
              href="/docs"
              className={`font-medium transition-colors ${
                isDark ? 'text-white hover:text-blue-400' : 'text-gray-900 hover:text-blue-600'
              }`}
            >
              Docs
            </Link>
            <Link
              href="/docs/api-reference/overview"
              className={`transition-colors ${
                isDark ? 'text-gray-400 hover:text-white' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              API Reference
            </Link>
            <Link
              href="/docs/features/security-scans"
              className={`transition-colors ${
                isDark ? 'text-gray-400 hover:text-white' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Features
            </Link>
          </nav>
        </div>

        {/* Search and actions */}
        <div className="flex items-center gap-4">
          {/* Search */}
          <div className="hidden sm:block relative">
            <input
              type="text"
              placeholder="Search docs..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className={`w-64 px-4 py-2 pl-10 border rounded-lg text-sm focus:outline-none transition-colors ${
                isDark
                  ? 'bg-gray-800 border-gray-700 text-white placeholder-gray-500 focus:border-blue-500'
                  : 'bg-gray-50 border-gray-300 text-gray-900 placeholder-gray-400 focus:border-blue-500'
              }`}
            />
            <Search className={`absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 ${
              isDark ? 'text-gray-500' : 'text-gray-400'
            }`} />
            <kbd className={`absolute right-3 top-1/2 -translate-y-1/2 hidden lg:inline-flex items-center gap-1 px-2 py-0.5 text-xs rounded ${
              isDark ? 'text-gray-500 bg-gray-700' : 'text-gray-400 bg-gray-200'
            }`}>
              ⌘K
            </kbd>
          </div>

          {/* Theme toggle */}
          <button
            onClick={toggleTheme}
            className={`p-2 rounded-lg transition-colors ${
              isDark
                ? 'text-gray-400 hover:text-white hover:bg-gray-800'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
            }`}
            title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>

          {/* GitHub link */}
          <a
            href="https://github.com/secur-eu"
            target="_blank"
            rel="noopener noreferrer"
            className={`transition-colors ${
              isDark ? 'text-gray-400 hover:text-white' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            <Github className="w-6 h-6" />
          </a>

          {/* Back to app */}
          <Link
            href="/"
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            Go to App
          </Link>
        </div>
      </div>
    </header>
  );
}
