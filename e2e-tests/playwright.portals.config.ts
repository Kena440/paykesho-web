import { defineConfig, devices } from '@playwright/test';

// Portal integration suite (e2e-tests/portals). Expects the portals to be
// running behind one origin: the Worker, or scripts/portal-router.mjs locally/CI.
export default defineConfig({
  testDir: './portals',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['json', { outputFile: 'portals-results.json' }], ['html', { open: 'never', outputFolder: 'portals-report' }]],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:8787',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    navigationTimeout: 60_000,
    actionTimeout: 20_000, // a click that cannot happen fails with a reason instead of a bare test timeout
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
