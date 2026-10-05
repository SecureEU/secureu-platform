import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 30000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  retries: 0,
  use: {
    // Point at a deployed instance with PLAYWRIGHT_BASE_URL=https://host.
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    trace: 'off',
    video: 'off',
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
  ],
});
