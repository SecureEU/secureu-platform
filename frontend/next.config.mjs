/** @type {import('next').NextConfig} */
const nextConfig = {
  // Cosmetic ESLint errors (e.g. unescaped curly quotes in docs pages) should
  // not block production builds. Lint runs in dev and CI separately.
  eslint: {
    ignoreDuringBuilds: true,
  },
  async rewrites() {
    return [
      {
        source: '/sphinx/dtm/:path*',
        destination: `${process.env.DTM_API_URL || 'http://localhost:8087'}/sphinx/dtm/:path*`,
      },
      {
        source: '/sphinx/ad/:path*',
        destination: `${process.env.AD_API_URL || 'http://localhost:5001'}/sphinx/ad/:path*`,
      },
      {
        source: '/proxy/darkweb/:path*',
        destination: 'http://localhost:8001/:path*',
      },
      {
        source: '/proxy/ssl/:path*',
        destination: 'http://localhost:5000/:path*',
      },
      {
        source: '/proxy/pentest/:path*',
        destination: 'http://localhost:3001/:path*',
      },
      {
        source: '/proxy/vsp/:path*',
        destination: 'http://localhost:5002/:path*',
      },
      {
        source: '/proxy/sqs/:path*',
        destination: 'http://localhost:8000/:path*',
      },
    ]
  },
};

export default nextConfig;
