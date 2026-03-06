'use client';

import { useTheme } from '@/components/docs/ThemeProvider';

export default function SwaggerPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div>
      <h1>Swagger UI</h1>

      <p>
        Interactive API documentation powered by Swagger/OpenAPI.
      </p>

      <div className={`mt-6 rounded-lg overflow-hidden border ${
        isDark ? 'border-gray-700' : 'border-gray-200'
      }`}>
        <iframe
          src="http://localhost:3001/docs"
          className="w-full h-[800px]"
          title="Swagger UI"
        />
      </div>

      <div className={`mt-6 p-4 rounded-lg ${
        isDark ? 'bg-gray-900 border border-gray-700' : 'bg-gray-50 border border-gray-200'
      }`}>
        <p className="mb-0">
          Can't see the Swagger UI? Make sure the backend server is running on port 3001.
          You can also access it directly at{' '}
          <a href="http://localhost:3001/docs" target="_blank" rel="noopener noreferrer">
            http://localhost:3001/docs
          </a>
        </p>
      </div>
    </div>
  );
}
