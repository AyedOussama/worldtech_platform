import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  outputDir: 'test-results/development-artifacts',
  testMatch: ['**/foundation.spec.ts', '**/design-system.spec.ts', '**/identity.spec.ts'],
  fullyParallel: false,
  workers: 1,
  retries: 0,
  forbidOnly: true,
  reporter: [['list'], ['json', { outputFile: 'test-results/development.json' }]],
  use: {
    ...devices['Desktop Chrome'],
    baseURL: 'http://localhost:4310',
    trace: 'off',
    screenshot: 'only-on-failure',
  },
  webServer: {
    command: 'npm exec -- ng serve --host localhost --port 4310 --proxy-config proxy.conf.json',
    url: 'http://localhost:4310',
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
