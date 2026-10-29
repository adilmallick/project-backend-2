import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  transpilePackages: ["@swiggy/types", "@swiggy/config"],
  async rewrites() {
    return [
      // Optional local proxies to bypass CORS when running locally in browser
      {
        source: "/api/proxy/user/:path*",
        destination: "http://localhost:8081/:path*",
      },
      {
        source: "/api/proxy/catalog/:path*",
        destination: "http://localhost:8082/:path*",
      },
      {
        source: "/api/proxy/order/:path*",
        destination: "http://localhost:8083/:path*",
      },
      {
        source: "/api/proxy/dispatch/:path*",
        destination: "http://localhost:8084/:path*",
      },
    ];
  },
};

export default nextConfig;
