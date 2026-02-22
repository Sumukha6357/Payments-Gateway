import type { NextConfig } from "next";

const maybeWithBundleAnalyzer = (
  config: NextConfig
): NextConfig => {
  if (process.env.ANALYZE !== "true") {
    return config;
  }

  try {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const bundleAnalyzer = require("@next/bundle-analyzer");
    return bundleAnalyzer({ enabled: true })(config);
  } catch {
    return config;
  }
};

const nextConfig: NextConfig = {
  reactStrictMode: true,
  typedRoutes: true,
  output: "standalone"
};

export default maybeWithBundleAnalyzer(nextConfig);
