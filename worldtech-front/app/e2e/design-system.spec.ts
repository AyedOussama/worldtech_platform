import { expect, Page, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { mkdir, writeFile } from 'node:fs/promises';
const evidence = `${process.env['WT_E2E_EVIDENCE_DIR'] ?? '../verification/fe03'}/regression-fe02`;

const browserErrors = new WeakMap<Page, string[]>();
test.beforeEach(({ page }) => {
  const errors: string[] = [];
  browserErrors.set(page, errors);
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()); });
});
test.afterEach(({ page }) => {
  expect(browserErrors.get(page), 'No unhandled or Angular-caught browser errors').toEqual([]);
});

async function expectAccessible(page: Page, name: string) {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']).analyze();
  await mkdir(`${evidence}/accessibility`, { recursive: true });
  await writeFile(`${evidence}/accessibility/${name}.json`, JSON.stringify({
    engine: result.testEngine, url: result.url, timestamp: result.timestamp,
    violations: result.violations, incomplete: result.incomplete, passes: result.passes.map((pass) => pass.id),
  }, null, 2));
  expect(result.violations, JSON.stringify(result.violations, null, 2)).toEqual([]);
}

async function fillIncident(page: Page) {
  const dialog = page.getByRole('dialog', { name: 'Déclarer un incident fictif', exact: true });
  await dialog.getByRole('combobox', { name: 'Site', exact: true }).click();
  await page.getByRole('option', { name: 'Lyon · Part-Dieu' }).click();
  await dialog.getByRole('textbox', { name: 'Objet', exact: true }).fill('Réseau indisponible à l’accueil');
  await dialog.getByRole('textbox', { name: 'Description', exact: true }).fill('Le poste d’accueil ne se connecte plus au réseau depuis ce matin.');
  await dialog.getByRole('textbox', { name: /Rémunération proposée/ }).fill('120,50');
  return dialog;
}

for (const space of ['admin', 'client'] as const) {
  for (const mode of ['light', 'dark'] as const) {
    for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }]) {
      test(`FE02 representative ${space} ${mode} ${viewport.width}`, async ({ page }) => {
        test.setTimeout(60_000);
        const unexpectedRequests: string[] = [];
        page.on('request', (request) => {
          if (request.url().includes('/api/') || !request.url().startsWith('http://localhost:4310')) unexpectedRequests.push(request.url());
        });
        await page.setViewportSize(viewport);
        await page.emulateMedia({ colorScheme: mode, reducedMotion: 'reduce' });
        await page.goto(`/dev/${space}/${space === 'admin' ? 'tickets' : 'incidents'}`);
        await expect(page.getByRole('heading', { name: space === 'admin' ? 'Tickets' : 'Incidents', exact: true })).toBeVisible();
        await page.evaluate(() => document.fonts.ready);
        expect(await page.evaluate(() => document.fonts.check('16px "IBM Plex Sans"', 'Équipement, cœur, 1\u202f250,50 €'))).toBe(true);
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        await expectAccessible(page, `${space}-${mode}-${viewport.width}`);
        await page.screenshot({ path: `${evidence}/screenshots/${space}-${mode}-${viewport.width}.png`, fullPage: true });
        await page.getByRole('button', { name: space === 'admin' ? 'Formulaire de référence' : 'Déclarer un incident fictif', exact: true }).click();
        const dialog = page.getByRole('dialog', { name: 'Déclarer un incident fictif', exact: true });
        await expect(dialog).toBeVisible();
        await expectAccessible(page, `form-${space}-${mode}-${viewport.width}`);
        await page.screenshot({ path: `${evidence}/screenshots/form-${space}-${mode}-${viewport.width}.png`, animations: 'disabled' });
        expect(unexpectedRequests).toEqual([]);
      });
    }
  }
}

test('filters, empty search and pagination round-trip in URL without server calls', async ({ page }) => {
  await page.goto('/dev/admin/tickets');
  await page.getByRole('button', { name: 'Page suivante', exact: true }).click();
  await expect(page).toHaveURL(/page=1/);
  await page.reload();
  await expect(page.getByText('Page 2 sur 2', { exact: true })).toBeVisible();
  await page.getByRole('searchbox', { name: 'Rechercher un ticket fictif' }).fill('reseau');
  await expect(page).toHaveURL(/q=reseau/);
  await expect(page.getByRole('button', { name: /DEMO-2048/ })).toBeVisible();
  await page.getByRole('searchbox').fill('aucun-resultat-synthetique');
  await expect(page.getByRole('heading', { name: 'Aucun résultat', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Réinitialiser les filtres', exact: true }).click();
  await page.getByRole('combobox', { name: 'Site', exact: true }).click();
  await page.getByRole('option', { name: 'Nantes · Île de Nantes' }).click();
  await expect(page.getByRole('status')).toHaveText('3 tickets fictifs');
  await page.reload();
  await expect(page.getByRole('combobox', { name: 'Site', exact: true })).toContainText('Nantes');
  await page.getByRole('button', { name: 'En attente client 2', exact: true }).click();
  await expect(page.getByRole('status')).toHaveText('1 ticket fictif');
});

test('client preview is a separate view, never an authenticated session', async ({ page }) => {
  await page.route('**/api/web/v1/me', (route) => route.fulfill({ json: {
    userId: '00000000-0000-4000-8000-000000000001', status: 'PENDING', roles: ['CLIENT'], emailVerified: true,
  } }));
  await page.goto('/dev/client/incidents');
  await expect(page.getByRole('main')).not.toContainText('Comptoir Azur');
  await expect(page.getByText('Aucun utilisateur connecté')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Incidents', exact: true })).toHaveAttribute('aria-current', 'page');
  for (const url of ['/client', '/admin']) {
    await page.goto(url);
    await expect(page.getByRole('heading', { name: 'Votre accès WorldTech', exact: true })).toBeVisible();
    await expect(page.getByText('Votre compte est en attente de validation.', { exact: false })).toBeVisible();
  }
});

test('keyboard skip, menu, detail dialog and focus return', async ({ page }) => {
  await page.goto('/dev/admin/tickets');
  await expect(page.getByRole('heading', { name: 'Tickets', exact: true })).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Aller au contenu' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('main')).toBeFocused();
  await page.getByRole('button', { name: 'Options de la liste' }).focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('menuitem', { name: 'Réinitialiser les filtres' })).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('button', { name: 'Options de la liste' })).toBeFocused();
  const opener = page.getByRole('button', { name: /DEMO-2048/ });
  await opener.focus(); await page.keyboard.press('Enter');
  await expect(page.getByRole('dialog', { name: 'DEMO-2048 · Aperçu fictif' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'DEMO-2048 · Aperçu fictif', exact: true })).toBeFocused();
  await expect(page.getByRole('heading', { name: 'Chronologie illustrative', exact: true })).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('region', { name: 'Contenu du ticket fictif', exact: true })).toBeFocused();
  await page.keyboard.press('PageDown');
  await expect.poll(() => page.getByRole('region', { name: 'Contenu du ticket fictif' }).evaluate((element) => element.scrollTop)).toBeGreaterThan(0);
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', { name: 'Fermer l’aperçu' })).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('region', { name: 'Contenu du ticket fictif', exact: true })).toBeFocused();
  await page.keyboard.press('Escape'); await expect(opener).toBeFocused();
});

test('mobile navigation traps focus, closes on Escape and restores the trigger', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/dev/client/incidents');
  const opener = page.getByRole('button', { name: 'Ouvrir la navigation' });
  await opener.focus(); await page.keyboard.press('Enter');
  await expect(page.getByRole('navigation', { name: 'Espace client', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'WorldTech', exact: true })).toBeFocused();
  for (let index = 0; index < 8; index++) {
    await page.keyboard.press('Tab');
    expect(await page.evaluate(() => !!document.activeElement?.closest('mat-sidenav'))).toBe(true);
  }
  await page.keyboard.press('Escape');
  await expect(opener).toBeFocused();
  await expect(opener).toHaveAttribute('aria-expanded', 'false');
});

test('form summary links, exact decimals, network/refusal/stale simulations and double action', async ({ page }) => {
  const requests: string[] = [];
  page.on('request', (request) => { if (request.url().includes('/api/')) requests.push(request.url()); });
  await page.goto('/dev/client/incidents');
  await page.getByRole('button', { name: 'Déclarer un incident fictif', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Déclarer un incident fictif', exact: true });
  await dialog.getByRole('button', { name: 'Tester la saisie' }).click();
  await expect(dialog.getByRole('region', { name: 'Champs à corriger' })).toBeFocused();
  await dialog.getByRole('link', { name: 'Choisissez un site', exact: true }).click();
  await expect(dialog.getByRole('combobox', { name: 'Site', exact: true })).toBeFocused();
  await fillIncident(page);
  for (const [scenario, message] of [['network', /réseau indisponible/], ['forbidden', /accès refusé/], ['stale', /version périmée/], ['local', /Aucun incident créé/]] as const) {
    await dialog.getByLabel('Résultat simulé').selectOption(scenario);
    await dialog.getByRole('button', { name: 'Tester la saisie' }).evaluate((button: HTMLButtonElement) => { button.click(); button.click(); });
    await expect(dialog.getByRole('button', { name: 'Vérification…' })).toBeDisabled();
    await expect(dialog.getByRole('status')).toContainText(message);
    await expect(dialog.getByRole('textbox', { name: 'Objet', exact: true })).toHaveValue('Réseau indisponible à l’accueil');
    await expect(dialog.getByRole('textbox', { name: /Rémunération proposée/ })).toHaveValue('120,50');
  }
  expect(requests).toEqual([]);
});

test('dirty form confirmation keeps data on Escape, returns focus, then allows explicit discard', async ({ page }) => {
  await page.goto('/dev/client/incidents');
  const opener = page.getByRole('button', { name: 'Déclarer un incident fictif', exact: true });
  await opener.click();
  const dialog = await fillIncident(page);
  await dialog.getByRole('button', { name: 'Annuler', exact: true }).click();
  const confirmation = page.getByRole('dialog', { name: 'Abandonner la saisie fictive ?', exact: true });
  await expect(confirmation.getByRole('button', { name: 'Conserver la saisie' })).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(confirmation).toHaveCount(0);
  await expect(dialog.getByRole('button', { name: 'Annuler', exact: true })).toBeFocused();
  await expect(dialog.getByRole('textbox', { name: 'Objet', exact: true })).toHaveValue('Réseau indisponible à l’accueil');
  await dialog.getByRole('button', { name: 'Annuler', exact: true }).click();
  await confirmation.getByRole('button', { name: 'Abandonner la saisie', exact: true }).click();
  await expect(dialog).toHaveCount(0); await expect(opener).toBeFocused();
});

test('list state fixtures are explicit and preserve filters, never automatic API fallback', async ({ page }) => {
  await page.goto('/dev/admin/tickets?q=reseau');
  await page.getByText('Tester les états de l’interface', { exact: true }).click();
  for (const [scenario, heading] of [['network', 'Réseau indisponible — simulation'], ['forbidden', 'Accès refusé — simulation'], ['stale', 'Version périmée — simulation'], ['empty', 'Aucun incident dans cet exemple']] as const) {
    await page.getByLabel('État de la liste').selectOption(scenario);
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
    await expect(page.getByRole('searchbox')).toHaveValue('reseau');
    await expect(page.getByRole('button', { name: /DEMO-2048/ })).toHaveCount(0);
  }
  await page.getByLabel('État de la liste').selectOption('loading');
  await expect(page.getByLabel('Chargement simulé')).toHaveAttribute('aria-busy', 'true');
  await page.getByLabel('État de la liste').selectOption('ready');
  await expect(page.getByRole('button', { name: /DEMO-2048/ })).toBeVisible();
});

test('system theme updates an already open overlay and native controls', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'light' });
  await page.goto('/dev/admin/tickets');
  await page.getByRole('button', { name: /DEMO-2048/ }).click();
  const dialog = page.getByRole('dialog');
  const light = await dialog.evaluate((element) => getComputedStyle(element.querySelector('mat-dialog-content') ?? element).color);
  await page.emulateMedia({ colorScheme: 'dark' });
  await expect(page.locator('html')).toHaveClass(/wt-dark/);
  await expect.poll(() => dialog.evaluate((element) => getComputedStyle(element.querySelector('mat-dialog-content') ?? element).color)).not.toBe(light);
  await expectAccessible(page, 'detail-overlay-dark');
});

for (const mode of ['light', 'dark'] as const) {
  test(`real controls, selected/focus/error states and overlays: ${mode}`, async ({ page }) => {
    test.setTimeout(60_000);
    await page.emulateMedia({ colorScheme: mode, reducedMotion: 'reduce' });
    await page.goto('/dev/admin/tickets');
    await expect(page.getByRole('heading', { name: 'Tickets', exact: true })).toBeVisible();
    await page.keyboard.press('Tab');
    await page.getByRole('combobox', { name: 'Thème', exact: true }).focus();
    await expect(page.getByRole('combobox', { name: 'Thème', exact: true })).toBeFocused();
    expect(await page.getByRole('combobox', { name: 'Thème', exact: true }).evaluate((element) => element.matches(':focus-visible'))).toBe(true);
    const contrasts = await page.evaluate(() => {
      function rgb(value: string): number[] {
        const match = /^rgba?\((.+)\)$/.exec(value);
        if (!match?.[1]) throw new Error(`Unsupported computed color: ${value}`);
        return match[1].split(/[, /]+/).map(Number);
      }
      function background(element: Element): string {
        const value = getComputedStyle(element).backgroundColor;
        const channels = rgb(value);
        if (channels.length < 4 || channels[3] === 1) return value;
        return element.parentElement ? background(element.parentElement) : 'rgb(255, 255, 255)';
      }
      function luminance(value: string): number {
        const channels = rgb(value).slice(0, 3).map((channel) => {
          const normal = channel / 255;
          return normal <= .04045 ? normal / 12.92 : ((normal + .055) / 1.055) ** 2.4;
        });
        return (channels[0] ?? 0) * .2126 + (channels[1] ?? 0) * .7152 + (channels[2] ?? 0) * .0722;
      }
      return [
        { selector: '.state-filter.selected', property: 'color', min: 4.5 },
        { selector: '.ticket-link', property: 'color', min: 4.5 },
        { selector: '.status-label[data-tone="warning"]', property: 'color', min: 4.5 },
        { selector: '.status-label[data-tone="info"]', property: 'color', min: 4.5 },
        { selector: '.theme-picker select', property: 'borderColor', min: 3 },
        { selector: '.theme-picker select', property: 'outlineColor', min: 3 },
      ].map(({ selector, property, min }) => {
        const element = document.querySelector(selector);
        if (!element) throw new Error(`Missing contrast target: ${selector}`);
        const foreground = getComputedStyle(element).getPropertyValue(property.replace(/[A-Z]/g, (letter) => '-' + letter.toLowerCase()));
        const bg = background(element);
        const a = luminance(foreground); const b = luminance(bg);
        return { selector, property, foreground, background: bg, ratio: (Math.max(a, b) + .05) / (Math.min(a, b) + .05), min,
          outlineStyle: getComputedStyle(element).outlineStyle, outlineWidth: getComputedStyle(element).outlineWidth };
      });
    });
    await mkdir(`${evidence}/accessibility`, { recursive: true });
    await writeFile(`${evidence}/accessibility/contrasts-${mode}.json`, JSON.stringify(contrasts, null, 2));
    for (const pair of contrasts) {
      expect(pair.ratio, `${pair.selector} ${pair.property}`).toBeGreaterThanOrEqual(pair.min);
      if (pair.property === 'outlineColor') {
        expect(pair.outlineStyle).toBe('solid');
        expect(Number.parseFloat(pair.outlineWidth)).toBeGreaterThanOrEqual(3);
      }
    }
    await page.getByRole('button', { name: 'Options de la liste' }).click();
    await expectAccessible(page, `menu-${mode}`);
    await page.keyboard.press('Escape');
    await page.getByRole('combobox', { name: 'Site', exact: true }).click();
    await expectAccessible(page, `select-${mode}`);
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: 'Formulaire de référence' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: 'Tester la saisie' }).click();
    await expect(dialog.getByRole('region', { name: 'Champs à corriger' })).toBeFocused();
    await expectAccessible(page, `invalid-form-${mode}`);
    await page.screenshot({ path: `${evidence}/screenshots/invalid-form-${mode}.png`, animations: 'disabled' });
  });
}

test('text zoom 200 percent and 320px reflow retain usable controls', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.setViewportSize({ width: 720, height: 900 });
  await page.goto('/dev/client/incidents');
  await page.evaluate(() => { document.documentElement.style.fontSize = '200%'; });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole('button', { name: 'Déclarer un incident fictif', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  expect(await page.getByRole('dialog').evaluate((element) => element.scrollWidth <= element.clientWidth)).toBe(true);
  await page.screenshot({ path: `${evidence}/screenshots/text-zoom-200.png`, animations: 'disabled' });
  await page.keyboard.press('Escape');
  await page.evaluate(() => { document.documentElement.style.fontSize = ''; });
  await page.setViewportSize({ width: 320, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await expect(page.getByRole('searchbox')).toBeVisible();
  await page.screenshot({ path: `${evidence}/screenshots/reflow-320.png`, fullPage: true });
});
