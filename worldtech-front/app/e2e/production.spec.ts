import { expect, test } from '@playwright/test';

test('built application opens without browser errors', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('/');
  await expect(page).toHaveURL(/\/connexion$/);
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
  await expect(page.getByLabel('Thème')).toHaveCount(0);
  expect(errors).toEqual([]);
});

for (const path of ['/dev', '/dev?demoEnabled=true', '/dev/components', '/dev/admin/tickets', '/dev/client/incidents']) {
  test(`development catalogue is absent from production: ${path}`, async ({ page }) => {
    await page.goto(path);
    await expect(page.getByRole('heading', { name: 'Page introuvable' })).toBeVisible();
    await expect(page.locator('[data-development-only]')).toHaveCount(0);
  });
}

test('runtime config cannot activate a development mode', async ({ page }) => {
  await page.route('**/runtime-config.json', (route) => route.fulfill({ json: {
    appName: 'WorldTech', apiBaseUrl: '/api/web/v1', buildVersion: 'test',
    googleLoginEnabled: false, telemetryEnabled: false, demoEnabled: true,
  } }));
  await page.goto('/dev');
  await expect(page.getByRole('heading', { name: 'Configuration indisponible' })).toBeVisible();
});

test('private routes stay closed in the optimized build', async ({ page }) => {
  for (const path of ['/admin', '/client/sites']) {
    await page.goto(path);
    await expect(page.getByRole('heading', { name: 'Session indisponible' })).toBeVisible();
  }
});

test('static server never turns missing assets or API failures into SPA success', async ({ request }) => {
  expect((await request.get('/missing-script.js')).status()).toBe(404);
  const response = await request.get('/api/web/v1/me');
  expect(response.status()).toBe(502);
  expect(response.headers()['content-type']).toContain('application/problem+json');
  expect((await request.get('/runtime-config.json')).headers()['cache-control']).toBe('no-store');
});

test('built login assets are images with correct MIME types and decode in the optimized page', async ({ page, request }) => {
  await page.route('**/api/web/v1/me', (route) => route.fulfill({ status: 401, json: { status: 401, code: 'AUTHENTICATION_REQUIRED' } }));
  await page.goto('/connexion');
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
  for (const [path, type] of [
    ['/assets/worldtech/login/branding/worldtech_logo_sombre.png', 'image/png'],
    ['/assets/worldtech/login/photos/photo_technicien_datacenter.webp', 'image/webp'],
    ['/assets/worldtech/login/graphics/carte_mondiale_transparente.webp', 'image/webp'],
    ['/assets/worldtech/login/icons/ic_google_g.svg', 'image/svg+xml'],
  ] as const) {
    const response = await request.get(path); expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toBe(type);
  }
  await expect.poll(() => page.evaluate(() => [...document.images].every((image) => image.complete && image.naturalWidth > 0))).toBe(true);
});
