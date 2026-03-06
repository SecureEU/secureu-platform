'use client';

import { useTheme } from './ThemeProvider';

export default function DocsContent({ children }) {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div className={`min-h-screen ${isDark ? 'bg-gray-950' : 'bg-white'}`}>
      <div className="flex min-h-[calc(100vh-64px)]">
        {children}
      </div>
    </div>
  );
}
