import { expect, Page, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { mkdir, readdir, writeFile } from 'node:fs/promises';
import { identityFixture, loginFixture } from './identity-fixture';

const evidence = '../verification/login-v1';
const errors = new WeakMap<Page, string[]>();
test.beforeEach(({ page }) => {
  const captured: string[] = []; errors.set(page, captured);
  page.on('pageerror', (error) => captured.push(error.message));
  page.on('console', (message) => { if (message.type() === 'error' && /(^ERROR\b|NG\d{4})/.test(message.text())) captured.push(message.text()); });
});
test.afterEach(({ page }) => { expect(errors.get(page)).toEqual([]); });

async function expectViewportFit(page: Page) {
  const dimensions = await page.evaluate(() => {
    const card = document.querySelector('.login-card');
    if (!card) throw new Error('Login card missing');
    return {
      horizontal: document.documentElement.scrollWidth - innerWidth,
      vertical: document.documentElement.scrollHeight - innerHeight,
      cardOverflow: card.scrollHeight - card.clientHeight,
      pageScroll: scrollY,
    };
  });
  expect(dimensions.horizontal, 'No horizontal page scroll').toBeLessThanOrEqual(1);
  expect(dimensions.vertical, 'No vertical page scroll').toBeLessThanOrEqual(1);
  expect(dimensions.cardOverflow, 'No hidden internal form scrolling at normal text size').toBeLessThanOrEqual(1);
  expect(dimensions.pageScroll).toBe(0);
  for (const selector of ['#login-email', '#login-password', 'button[type="submit"]', '.forgot', '.access a']) {
    await expect(page.locator(selector)).toBeInViewport({ ratio: 1 });
  }
}

const sizes = [
  { width: 1672, height: 941 }, { width: 1440, height: 900 }, { width: 1280, height: 800 },
  { width: 1366, height: 768 }, { width: 1280, height: 720 }, { width: 1024, height: 768 },
  { width: 390, height: 844 }, { width: 360, height: 800 }, { width: 320, height: 568 },
];
for (const mode of ['light', 'dark'] as const) {
  for (const size of sizes) {
    test(`WorldTech login V1 ${mode} ${size.width}x${size.height} (HTTP double)`, async ({ page }) => {
      await identityFixture(page);
      await page.setViewportSize(size); await page.emulateMedia({ colorScheme: mode, reducedMotion: 'reduce' });
      await page.goto('/connexion');
      await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
      await page.evaluate(() => document.fonts.ready);
      await expect.poll(() => page.evaluate(() => [...document.images].every((image) => image.complete && image.naturalWidth > 0))).toBe(true);
      await expectViewportFit(page);
      await expect(page.getByLabel('Thème')).toHaveCount(0);
      const panel = await page.locator('.login-panel').boundingBox();
      const card = await page.locator('.login-card').boundingBox();
      if (!panel || !card) throw new Error('Login geometry unavailable');
      expect(card.x).toBeGreaterThanOrEqual(panel.x); expect(card.x + card.width).toBeLessThanOrEqual(size.width);
      if (size.width >= 1280) expect(panel.x / size.width).toBeCloseTo(.61, 2);
      if (size.width >= 768) {
        await expect(page.getByRole('list', { name: 'Domaines d’intervention' }).getByRole('listitem')).toHaveCount(4);
        await expect(page.getByText('Couverture internationale', { exact: true })).toBeVisible();
        const overlap = await page.evaluate(() => {
          const text = [...document.querySelectorAll('.editorial .line, .editorial p')].map((element) => element.getBoundingClientRect());
          return !text.length || [...document.querySelectorAll('.domains li')].some((item) => {
            const box = item.getBoundingClientRect();
            return text.some((line) => box.left < line.right && box.right > line.left && box.top < line.bottom && box.bottom > line.top);
          });
        });
        expect(overlap, 'Domain badges must not cover editorial text').toBe(false);
      } else {
        await expect(page.locator('wt-login-visual')).toBeHidden();
        expect(card.y).toBeLessThan(100);
      }
      const name = `login-${mode}-${size.width}x${size.height}`;
      await page.screenshot({ path: `${evidence}/screenshots/${name}.png`, fullPage: true, animations: 'disabled' });
      const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']).analyze();
      await mkdir(`${evidence}/accessibility`, { recursive: true });
      await writeFile(`${evidence}/accessibility/${name}.json`, JSON.stringify({ violations: result.violations, incomplete: result.incomplete }, null, 2));
      expect(result.violations).toEqual([]);
    });
  }
}

test('all deployed login assets load as real local images; no screenshot or remote font is embedded', async ({ page, request }) => {
  await identityFixture(page); const remote: string[] = [];
  page.on('request', (req) => { if (!['localhost', '127.0.0.1'].includes(new URL(req.url()).hostname)) remote.push(req.url()); });
  await page.goto('/connexion');
  const assets: string[] = [];
  for (const category of ['branding', 'photos', 'graphics', 'icons']) {
    for (const name of await readdir(`public/assets/worldtech/login/${category}`)) {
      const path = `/assets/worldtech/login/${category}/${name}`;
      const response = await request.get(path); expect(response.status(), path).toBe(200);
      expect(response.headers()['content-type']).toMatch(/^image\//); assets.push(path);
    }
  }
  await page.evaluate(async (paths) => { for (const path of paths) { const image = new Image(); image.src = path; await image.decode(); } }, assets);
  expect(assets.length).toBe(21); expect(remote).toEqual([]);
  const images = await page.locator('img').evaluateAll((elements) => elements.map((image) => image.getAttribute('src') ?? ''));
  expect(images.some((url) => /preview|reference_montage|base64|demo-login/.test(url))).toBe(false);
});

test('keyboard errors, exact password, safe recovery and disabled Google preserve real identity boundaries', async ({ page }) => {
  const fixture = await identityFixture(page);
  await page.goto('/connexion');
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Aller au contenu', exact: true })).toBeFocused();
  await page.keyboard.press('Enter'); await expect(page.locator('main')).toBeFocused();
  await expect(page).toHaveURL(/\/connexion$/);
  await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
  await expect(page.getByRole('region', { name: 'Champs à corriger' })).toBeFocused();
  await page.getByRole('link', { name: 'Renseignez une adresse email valide', exact: true }).click();
  const email = page.getByRole('textbox', { name: 'E-mail professionnel', exact: true });
  await expect(email).toBeFocused(); await expect(email).toHaveAttribute('aria-describedby', 'login-email-error');
  await email.fill('fixture@example.test');
  await page.getByLabel('Mot de passe', { exact: true }).fill(' Exact synthetic passphrase ');
  await page.getByRole('button', { name: 'Afficher le mot de passe', exact: true }).click();
  await expect(page.getByRole('textbox', { name: 'Mot de passe', exact: true })).toHaveValue(' Exact synthetic passphrase ');
  await expect(page.getByRole('button', { name: 'Continuer avec Google', exact: true })).toBeDisabled();
  expect(fixture.calls.some((call) => call.path.includes('/google/'))).toBe(false);
  await expect(page.getByRole('link', { name: 'Créer un compte client' })).toBeVisible();
  const login = page.waitForRequest((req) => req.url().endsWith('/auth/login'));
  await page.getByLabel('Mot de passe', { exact: true }).press('Enter');
  expect((await login).postDataJSON()).toEqual({ email: 'fixture@example.test', password: ' Exact synthetic passphrase ' });
  await expect(page.getByRole('heading', { name: 'Espace client', exact: true })).toBeVisible();
});

test('recovery links remain the existing routes; system theme and 320px text reflow stay usable', async ({ page }) => {
  await identityFixture(page); await page.setViewportSize({ width: 320, height: 720 });
  await page.emulateMedia({ colorScheme: 'light', reducedMotion: 'reduce' }); await page.goto('/connexion');
  await expect(page.getByLabel('Thème')).toHaveCount(0);
  await page.emulateMedia({ colorScheme: 'dark' });
  await expect(page.locator('html')).toHaveClass(/wt-dark/);
  await page.evaluate(() => { document.documentElement.style.fontSize = '200%'; });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await expect(page.getByRole('button', { name: 'Se connecter', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Se connecter', exact: true }).scrollIntoViewIfNeeded();
  await expect(page.getByRole('button', { name: 'Se connecter', exact: true })).toBeInViewport({ ratio: 1 });
  expect(await page.evaluate(() => scrollY)).toBe(0);
  await page.screenshot({ path: `${evidence}/screenshots/login-text-200-320.png`, fullPage: true });
  await page.getByRole('link', { name: 'Mot de passe oublié ?', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Mot de passe oublié', exact: true })).toBeVisible();
  await page.goto('/connexion'); await page.getByRole('link', { name: 'Créer un compte client' }).click();
  await expect(page.getByRole('heading', { name: 'Confirmer votre adresse email', exact: true })).toBeVisible();
});

for (const size of [
  { width: 1672, height: 941 }, { width: 1280, height: 720 }, { width: 1024, height: 768 },
  { width: 390, height: 844 }, { width: 360, height: 640 }, { width: 320, height: 568 },
]) {
  test(`static scene and no scroll through typing, delayed session, validation and server refusal ${size.width}x${size.height}`, async ({ page }) => {
    await identityFixture(page);
    let resolveSession: () => void = () => undefined;
    let resolveLogin: () => void = () => undefined;
    const sessionResponse = new Promise<void>((resolve) => { resolveSession = resolve; });
    const loginResponse = new Promise<void>((resolve) => { resolveLogin = resolve; });
    await page.route('**/api/web/v1/me', async (route) => {
      await sessionResponse;
      await route.fulfill({ status: 503, json: { status: 503, code: 'SERVICE_UNAVAILABLE' } });
    });
    await page.route('**/api/web/v1/auth/login', async (route) => {
      await loginResponse;
      await route.fulfill({ status: 403, json: { status: 403, code: 'ACCESS_DENIED' } });
    });
    await page.setViewportSize(size); await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.goto('/connexion'); await expect(page.locator('#login-title')).toBeVisible();
    await page.evaluate(async () => {
      await document.fonts.ready;
      await Promise.all([...document.images].map((image) => image.decode()));
      await Promise.all(['photo_salle_serveurs.png', 'photo_technicien_datacenter.webp'].map(async (name) => {
        const image = new Image(); image.src = `/assets/worldtech/login/photos/${name}`; await image.decode();
      }));
    });
    const scene = page.locator('.scene');
    const initialBox = size.width >= 768 ? await scene.boundingBox() : null;
    const initialPixels = size.width >= 768 ? await scene.screenshot({ animations: 'disabled' }) : null;
    const geometry = async () => {
      await expectViewportFit(page);
      if (initialPixels) {
        expect(await scene.boundingBox()).toEqual(initialBox);
        expect(Buffer.compare(await scene.screenshot({ animations: 'disabled' }), initialPixels), 'Left scene must remain pixel-identical').toBe(0);
      }
    };
    await geometry(); resolveSession();
    await expect(page.getByRole('status')).toContainText('Le service de session est indisponible');
    await geometry();
    await page.locator('#login-email').fill('invalide'); await page.locator('#login-password').focus();
    await expect(page.getByRole('region', { name: 'Champs à corriger' })).toBeVisible();
    await geometry();
    await page.locator('button[type="submit"]').click();
    await expect(page.getByRole('region', { name: 'Champs à corriger' })).toBeFocused();
    await geometry();
    await page.locator('#login-email').fill('fixture@example.test');
    await page.locator('#login-password').fill('Synthetic passphrase only');
    await page.getByRole('button', { name: 'Afficher le mot de passe', exact: true }).click();
    await page.getByRole('button', { name: 'Masquer le mot de passe', exact: true }).click();
    await expect(page.getByRole('region', { name: 'Champs à corriger' })).toHaveCount(0);
    await geometry();
    await page.locator('button[type="submit"]').click();
    await expect(page.getByRole('button', { name: 'Connexion…', exact: true })).toBeDisabled();
    await geometry(); resolveLogin();
    await expect(page.getByRole('alert')).toContainText('Cette action est refusée');
    await geometry();
    await page.screenshot({ path: `${evidence}/screenshots/login-refusal-${size.width}x${size.height}.png`, fullPage: true });
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']).analyze();
    await writeFile(`${evidence}/accessibility/login-refusal-${size.width}x${size.height}.json`, JSON.stringify({ violations: result.violations, incomplete: result.incomplete }, null, 2));
    expect(result.violations).toEqual([]);
  });
}

test('theme controls are absent only on login and keep working on other public pages', async ({ page }) => {
  await identityFixture(page); await page.goto('/connexion');
  await expect(page.locator('#login-title')).toBeVisible();
  await expect(page.getByLabel('Thème')).toHaveCount(0);
  await page.getByRole('link', { name: 'Mot de passe oublié ?', exact: true }).click();
  await expect(page.getByLabel('Thème')).toBeVisible();
  await page.getByLabel('Thème').selectOption('dark');
  await expect(page.locator('html')).toHaveClass(/wt-dark/);
  await page.goto('/connexion');
  await expect(page.locator('#login-title')).toBeVisible();
  await expect(page.getByLabel('Thème')).toHaveCount(0);
  await expect(page.locator('html')).toHaveClass(/wt-dark/);
});

test('refused credentials preserve the password and cannot open a private UI', async ({ page }) => {
  const fixture = await identityFixture(page, { loginFailure: 401 }); await loginFixture(page);
  await expect(page.getByRole('alert')).toContainText('ne permettent pas de confirmer votre accès');
  await expect(page.getByLabel('Mot de passe', { exact: true })).toHaveValue('Synthetic passphrase only');
  expect(fixture.calls.filter((call) => call.path.endsWith('/auth/login')).length).toBe(1);
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toHaveCount(0);
});

test('pending login is single-flight; lost response never grants access or automatically retries', async ({ page }) => {
  await identityFixture(page);
  let release: () => void = () => undefined; let calls = 0;
  const pending = new Promise<void>((resolve) => { release = resolve; });
  await page.route('**/api/web/v1/auth/login', async (route) => { calls++; await pending; await route.abort('failed'); });
  await loginFixture(page);
  await expect(page.getByRole('button', { name: 'Connexion…', exact: true })).toBeDisabled();
  await page.locator('form').dispatchEvent('submit'); release();
  await expect(page.getByRole('alert')).toContainText('Le résultat de cette opération n’est pas confirmé');
  expect(calls).toBe(1);
  await expect(page.getByRole('heading', { name: 'Espace client', exact: true })).toHaveCount(0);
});
