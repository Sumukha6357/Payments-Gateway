import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "tests/e2e",
  timeout: 120000,
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:3000"
  },
  webServer: {
    command: "npm run dev -- --hostname 127.0.0.1",
    port: 3000,
    reuseExistingServer: true,
    timeout: 240000
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }]
});
