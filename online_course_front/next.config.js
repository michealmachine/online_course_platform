/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/content-service/:path*',
        destination: 'http://localhost:8083/content-service/:path*'
      },
      {
        source: '/api/media-service/:path*',
        destination: 'http://localhost:8083/media-service/:path*'
      }
    ]
  },
  reactStrictMode: true,
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '8080',
        pathname: '/**',
      },
    ],
    unoptimized: process.env.NODE_ENV === 'development',
  },
  experimental: {
    serverActions: {
      bodySizeLimit: '2mb',
      allowedOrigins: ['localhost:3000'],
    },
  },
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production',
  }
}

module.exports = nextConfig 