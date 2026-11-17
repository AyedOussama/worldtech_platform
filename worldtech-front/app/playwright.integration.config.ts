import { defineConfig, devices } from '@playwright/test';

// The real fixture displays one-time TOTP/recovery secrets. Disable Playwright's
// automatic failure DOM snapshot as well as screenshots/traces/videos below.
process.env['PLAYWRIGHT_NO_COPY_PROMPT'] = '1';

export default defineConfig({
  testDir: './e2e', testMatch: '**/identity.integration.spec.ts', outputDir: 'test-results/spring-artifacts',
  globalSetup: './scripts/integration/global-setup.mjs', workers: 1, retries: 0, forbidOnly: true,
  reporter: [['list'], ['json', { outputFile: 'test-results/spring.json' }]],
  use: { ...devices['Desktop Chrome'], baseURL: 'http://localhost:4311', trace: 'off', screenshot: 'off', video: 'off' },
  webServer: { command: 'npm exec -- ng serve --host localhost --port 4311 --proxy-config proxy.integration.conf.json', url: 'http://localhost:4311', reuseExistingServer: false, timeout: 120_000 },
});
