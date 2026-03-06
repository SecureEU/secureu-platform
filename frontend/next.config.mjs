/** @type {import('next').NextConfig} */
const nextConfig = {
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
    ]
  },
};

export default nextConfig;
