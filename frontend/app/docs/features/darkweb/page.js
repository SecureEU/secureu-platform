'use client';
import { useTheme } from '@/components/docs/ThemeProvider';

export default function DarkwebPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const h1 = `text-3xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h2 = `text-2xl font-semibold mt-8 mb-3 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const p = `mb-4 leading-relaxed ${isDark ? 'text-gray-300' : 'text-gray-700'}`;
  const code = `${isDark ? 'bg-gray-800 text-green-400' : 'bg-gray-100 text-gray-800'} rounded px-3 py-2 block overflow-x-auto text-sm font-mono my-3 whitespace-pre`;
  const li = `mb-2 ${isDark ? 'text-gray-300' : 'text-gray-700'}`;

  return (
    <div>
      <h1 className={h1}>Darkweb Monitoring</h1>
      <p className={p}>
        The Darkweb Monitoring module searches dark web search engines via Tor for leaked credentials, data breaches, and mentions of your organization. It supports 24 onion-based search engines and aggregates results.
      </p>

      <h2 className={h2}>Architecture</h2>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}><strong>Backend:</strong> Python FastAPI on port 8001 (container: dark-web-backend)</li>
        <li className={li}><strong>Tor proxy:</strong> SOCKS5 on port 9050 inside the container</li>
        <li className={li}><strong>Search engines:</strong> 24 configured in <code>engines.json</code> including Ahmia, Torch, Haystack, and others</li>
      </ul>

      <h2 className={h2}>How It Works</h2>
      <ol className="list-decimal pl-6 mb-4">
        <li className={li}>Enter a search keyword (company name, domain, email, etc.)</li>
        <li className={li}>Select search mode: exact matching or broad search</li>
        <li className={li}>The backend routes requests through Tor to onion search engines</li>
        <li className={li}>Results are aggregated, deduplicated, and displayed with source URLs</li>
      </ol>

      <h2 className={h2}>Engine Health</h2>
      <p className={p}>
        Before each search, the system checks which engines are online. Onion services are inherently unreliable &mdash; expect some engines to be down at any time. The search proceeds with whichever engines respond within the 15-second timeout.
      </p>
      <p className={p}>
        If all selected engines are down, you will see the error: <em>&quot;All engines are down&quot;</em>. This is typically a transient Tor connectivity issue &mdash; retry after a few minutes.
      </p>

      <h2 className={h2}>Usage</h2>
      <p className={p}>Navigate to <strong>OffSec &rarr; Darkweb &rarr; Monitor</strong>. Enter your search term and click Scan.</p>

      <h2 className={h2}>API</h2>
      <pre className={code}>{`# Search dark web for a keyword
GET http://localhost:8001/search?keyword=example.com&limit=3

# Parameters:
#   keyword     - Search term (required)
#   engines     - Comma-separated engine names (optional, defaults to all)
#   exclude     - Engines to exclude (optional)
#   limit       - Max pages per engine (default: 3)
#   mp_units    - Multiprocessing units (default: CPU cores - 1)`}</pre>
    </div>
  );
}
