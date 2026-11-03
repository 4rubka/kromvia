import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: { unoptimized: true },
  agentRules: false,
};

export default nextConfig;

import("@opennextjs/cloudflare").then((m) => m.initOpenNextCloudflareForDev());
