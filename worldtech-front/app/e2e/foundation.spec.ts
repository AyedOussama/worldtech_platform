import { expect, test } from '@playwright/test';

const config = { appName: 'WorldTech', apiBaseUrl: '/api/web/v1', googleLoginEnabled: false,
  telemetryEnabled: false, buildVersion: 'browser-test' };
const evidence = process.env['WT_E2E_EVIDENCE_DIR'] ?? '../verification/fe03';

// FE03: these foundation checks deliberately establish an anonymous server response.
test.beforeEach(async ({ page }) => {
  await page.route('**/api/web/v1/me', (route) => route.fulfill({ status: 401, json: { status: 401, code: 'AUTHENTICATION_REQUIRED' } }));
});

test('unknown route and keyboard return remain usable', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('/adresse-inconnue');
  await expect(page.getByRole('heading', { name: 'Page introuvable' })).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Aller au contenu' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.locator('main')).toBeFocused();
  await expect(page).toHaveURL(/\/adresse-inconnue$/);
  await page.getByRole('link', { name: 'Retour à la connexion' }).focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
  expect(errors).toEqual([]);
});

for (const path of ['/client', '/admin', '/admin/tickets?token=synthetic-marker']) {
  test(`private deep link stays closed: ${path}`, async ({ page }) => {
    const requests: string[] = [];
    page.on('request', (request) => { if (request.url().includes('/api/')) requests.push(new URL(request.url()).pathname); });
    await page.addInitScript(() => localStorage.setItem('wt-session', '{"role":"ADMIN","authenticated":true}'));
    await page.goto(path);
    await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
    expect(page.url()).not.toContain('synthetic-marker');
    expect(requests).toEqual(['/api/web/v1/me']);
    await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toHaveCount(0);
  });
}

test('invalid or unavailable startup config cannot bootstrap private content', async ({ page }) => {
  const requests: string[] = [];
  page.on('request', (request) => { if (request.url().includes('/api/')) requests.push(request.url()); });
  await page.route('**/runtime-config.json', (route) => route.fulfill({ json: { ...config, apiBaseUrl: 'https://outside.example' } }));
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Configuration indisponible' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Réessayer' })).toBeVisible();
  expect(requests).toEqual([]);
  await page.route('**/runtime-config.json', (route) => route.abort());
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Configuration indisponible' })).toBeVisible();
});

test('development field and Material button handle errors without API calls', async ({ page }) => {
  const requests: string[] = [];
  page.on('request', (request) => { if (request.url().includes('/api/')) requests.push(request.url()); });
  await page.goto('/dev');
  await expect(page.getByRole('heading', { name: 'Composants de développement' })).toBeVisible();
  // Browser constraint validation and Angular validation must both stay usable.
  await page.getByLabel('Libellé').focus();
  await page.getByLabel('Libellé').press('Tab');
  await expect(page.getByText('Renseignez un libellé')).toBeVisible();
  await page.getByLabel('Libellé').fill('Exemple local');
  await page.getByRole('button', { name: 'Vérifier la saisie' }).click();
  await expect(page.getByRole('status')).toHaveText('La saisie est valide. Aucun envoi réseau.');
  expect(requests).toEqual([]);
});

test('system theme follows changes and explicit choice survives reload', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'dark' });
  await page.goto('/dev');
  await expect(page.locator('html')).toHaveClass(/wt-dark/);
  await page.getByLabel('Thème').selectOption('light');
  await page.reload();
  await expect(page.getByLabel('Thème')).toHaveValue('light');
  await expect(page.locator('html')).not.toHaveClass(/wt-dark/);
  await page.getByLabel('Thème').selectOption('system');
  await expect(page.locator('html')).toHaveClass(/wt-dark/);
  await page.emulateMedia({ colorScheme: 'light' });
  await expect(page.locator('html')).not.toHaveClass(/wt-dark/);
});

test('theme controls work even when storage is unavailable', async ({ page }) => {
  await page.addInitScript(() => Object.defineProperty(window, 'localStorage', {
    get() { throw new Error('Storage blocked for this test'); },
  }));
  await page.goto('/dev');
  await expect(page.getByRole('heading', { name: 'Composants de développement', exact: true })).toBeVisible();
  await page.getByLabel('Thème').selectOption('dark');
  await expect(page.locator('html')).toHaveClass(/wt-dark/);
});

for (const mode of ['light', 'dark'] as const) {
  for (const viewport of [{ width: 1440, height: 900 }, { width: 390, height: 844 }]) {
    test(`representative foundation: ${mode} ${viewport.width}`, async ({ page }) => {
      await page.setViewportSize(viewport);
      await page.emulateMedia({ colorScheme: mode, reducedMotion: 'reduce' });
      await page.goto('/dev');
      await page.getByLabel('Thème').selectOption(mode);
      await expect(page.getByRole('heading', { name: 'Composants de développement' })).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await page.getByLabel('Thème').focus();
      await expect(page.getByLabel('Thème')).toBeFocused();
      await page.screenshot({ path: `${evidence}/regression-fe01/foundation-${mode}-${viewport.width}.png`, fullPage: true });
    });
  }
}
