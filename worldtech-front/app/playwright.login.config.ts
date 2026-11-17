import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e', testMatch: '**/login-design.spec.ts',
  outputDir: 'test-results/login-v1-artifacts', workers: 1, retries: 0, forbidOnly: true,
  timeout: 60_000, // A cold Angular development compile is not a page-response performance test.
  reporter: [['list'], ['json', { outputFile: 'test-results/login-v1.json' }]],
  use: { ...devices['Desktop Chrome'], baseURL: 'http://localhost:4320', trace: 'off', screenshot: 'only-on-failure', video: 'off' },
  webServer: {
    command: 'npm exec -- ng serve --host localhost --port 4320 --proxy-config proxy.conf.json',
    url: 'http://localhost:4320', reuseExistingServer: false, timeout: 120_000,
  },
});
