import { expect, Page, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { mkdir, writeFile } from 'node:fs/promises';
import { identityFixture, loginFixture } from './identity-fixture';
const evidence = process.env['WT_E2E_EVIDENCE_DIR'] ?? '../verification/fe03';

const browserErrors = new WeakMap<Page, string[]>();
test.beforeEach(({ page }) => {
  const errors: string[] = []; browserErrors.set(page, errors);
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => { if (message.type() === 'error' && /(^ERROR\b|NG\d{4})/.test(message.text())) errors.push(message.text()); });
});
test.afterEach(({ page }) => { expect(browserErrors.get(page)).toEqual([]); });

async function captureAndAudit(page: Page, name: string) {
  await page.evaluate(() => document.fonts.ready);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: `${evidence}/screenshots/${name}.png`, fullPage: true });
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']).analyze();
  await mkdir(`${evidence}/accessibility`, { recursive: true });
  await writeFile(`${evidence}/accessibility/${name}.json`, JSON.stringify({ violations: result.violations, incomplete: result.incomplete }, null, 2));
  expect(result.violations).toEqual([]);
}

for (const mode of ['light', 'dark'] as const) {
  for (const width of [1440, 390]) {
    test(`identity design ${mode} ${width} (simulated)`, async ({ page }) => {
      await identityFixture(page);
      await page.setViewportSize({ width, height: 900 }); await page.emulateMedia({ colorScheme: mode, reducedMotion: 'reduce' });
      await page.goto('/connexion');
      await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
      await page.evaluate(() => document.fonts.ready);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await page.screenshot({ path: `${evidence}/screenshots/login-${mode}-${width}.png`, fullPage: true });
      await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
      await expect(page.getByRole('region', { name: 'Champs à corriger' })).toBeFocused();
      const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']).analyze();
      await mkdir(`${evidence}/accessibility`, { recursive: true });
      await writeFile(`${evidence}/accessibility/login-${mode}-${width}.json`, JSON.stringify({ violations: result.violations, incomplete: result.incomplete }, null, 2));
      expect(result.violations).toEqual([]);
      await expect(page.getByRole('button', { name: 'Continuer avec Google', exact: true })).toBeDisabled();
      await expect(page.getByText('Connexion Google non configurée', { exact: true })).toBeVisible();
    });

    test(`identity secondary screens ${mode} ${width} (simulated)`, async ({ page }) => {
      test.setTimeout(60_000);
      await identityFixture(page, { admin: true });
      await page.setViewportSize({ width, height: 900 }); await page.emulateMedia({ colorScheme: mode, reducedMotion: 'reduce' });
      await page.goto('/auth/mot-de-passe-oublie');
      await expect(page.getByRole('heading', { name: 'Mot de passe oublié', exact: true })).toBeVisible();
      await captureAndAudit(page, `recovery-${mode}-${width}`);
      await page.goto('/auth/password-reset#token=' + 'a'.repeat(43));
      await expect(page.getByRole('heading', { name: 'Nouveau mot de passe', exact: true })).toBeVisible();
      await captureAndAudit(page, `reset-${mode}-${width}`);
      await loginFixture(page);
      await expect(page.getByRole('heading', { name: 'Vérifier votre accès', exact: true })).toBeVisible();
      await captureAndAudit(page, `mfa-${mode}-${width}`);
      await page.getByRole('button', { name: 'Configurer mon application', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Configurer votre application', exact: true })).toBeVisible();
      await expect(page.getByLabel('Clé de configuration TOTP')).toHaveCount(0);
      await captureAndAudit(page, `enrollment-${mode}-${width}`);
      await page.getByRole('textbox', { name: 'Code à six chiffres', exact: true }).fill('123456');
      await page.getByRole('button', { name: 'Confirmer la configuration', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Conservez vos codes de récupération' })).toBeVisible();
      await captureAndAudit(page, `recovery-codes-${mode}-${width}`);
      await page.getByLabel('J’ai conservé ces codes hors ligne', { exact: true }).check();
      await page.getByRole('button', { name: 'Continuer', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
      await captureAndAudit(page, `admin-access-${mode}-${width}`);
    });
  }
}
test('login preserves safe return, requires /me and never accepts a local fake admin (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page);
  await page.addInitScript(() => localStorage.setItem('wt-session', '{"role":"ADMIN"}'));
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
  await page.getByRole('textbox', { name: 'E-mail professionnel', exact: true }).fill('fixture@example.test');
  await page.getByLabel('Mot de passe', { exact: true }).fill('Synthetic passphrase only');
  await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Espace client', exact: true })).toBeVisible();
  expect(fixture.calls.filter((call) => call.path.endsWith('/me')).length).toBe(2);
  await page.goto('/admin'); await expect(page.getByRole('heading', { name: 'Accès refusé', exact: true })).toBeVisible();
});
test('MFA is server-proven, reload cannot guess a challenge from generic 403 (simulated)', async ({ page }) => {
  await identityFixture(page, { admin: true }); await loginFixture(page);
  await expect(page.getByRole('heading', { name: 'Vérifier votre accès', exact: true })).toBeVisible();
  await page.reload(); await expect(page.getByText('Ce parcours ne peut pas être repris', { exact: false })).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Code à six chiffres' })).toHaveCount(0);
});
test('MFA enrollment acknowledgement and logout (simulated)', async ({ page }) => {
  await identityFixture(page, { admin: true }); await loginFixture(page);
  await page.getByRole('button', { name: 'Configurer mon application', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Configurer votre application', exact: true })).toBeVisible();
  await page.getByRole('textbox', { name: 'Code à six chiffres', exact: true }).fill('123456');
  await page.getByRole('button', { name: 'Confirmer la configuration', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Conservez vos codes de récupération' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Continuer', exact: true })).toBeDisabled();
  await page.getByLabel('J’ai conservé ces codes hors ligne', { exact: true }).check();
  await page.getByRole('button', { name: 'Continuer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Se déconnecter', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
});
test('pending account remains closed; failed logout is never presented as success (simulated)', async ({ page }) => {
  await identityFixture(page, { pending: true, logoutFailure: true }); await loginFixture(page);
  await expect(page.getByRole('heading', { name: 'Votre accès WorldTech', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Se déconnecter', exact: true }).click();
  await expect(page.getByText('La déconnexion n’a pas été confirmée.', { exact: false })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Réessayer la déconnexion', exact: true })).toBeVisible();
});
test('recovery uses generic 202 without account enumeration (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page); await page.goto('/auth/mot-de-passe-oublie');
  await page.getByRole('textbox', { name: 'Adresse email', exact: true }).fill('absent@example.test');
  await page.getByRole('button', { name: 'Demander le lien', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Si cette adresse correspond à un compte éligible');
  expect(fixture.calls.filter((call) => call.path.endsWith('/password-reset/requests')).length).toBe(1);
});
test('reset scrubs fragment before runtime fetch, never confirms by GET, loses secrets on reload (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page); const token = 'a'.repeat(43);
  let scrubbedBeforeConfig = false;
  page.on('request', (request) => { if (request.url().endsWith('/runtime-config.json')) scrubbedBeforeConfig = !page.url().includes('#'); });
  await page.goto('/auth/password-reset#token=' + token);
  await expect(page.getByRole('heading', { name: 'Nouveau mot de passe', exact: true })).toBeVisible();
  expect(page.url().includes(token)).toBe(false); expect(scrubbedBeforeConfig).toBe(true);
  expect(fixture.calls.filter((call) => call.path.endsWith('/confirm')).length).toBe(0);
  await page.reload(); await expect(page.getByText('Ce lien est absent', { exact: false })).toBeVisible();
});
test('keyboard, password manager and safe refusal feedback (simulated)', async ({ page }) => {
  await identityFixture(page, { loginFailure: 401 }); await page.goto('/connexion');
  const email = page.getByRole('textbox', { name: 'E-mail professionnel', exact: true });
  await expect(email).toHaveAttribute('autocomplete', 'username');
  await email.fill('fixture@example.test');
  await page.getByLabel('Mot de passe', { exact: true }).fill('Synthetic passphrase only');
  await page.getByRole('button', { name: 'Afficher le mot de passe', exact: true }).click();
  await expect(page.getByRole('textbox', { name: 'Mot de passe', exact: true })).toHaveAttribute('autocomplete', 'current-password');
  await page.getByRole('button', { name: 'Se connecter', exact: true }).focus(); await page.keyboard.press('Enter');
  await expect(page.getByRole('alert')).toContainText('ne permettent pas de confirmer votre accès');
  await expect(email).toHaveValue('fixture@example.test');
  await expect(page.getByText('Le service de session est indisponible.', { exact: false })).toHaveCount(0);
});

test('429 countdown blocks duplicate login, never automatically replays it (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page, { loginFailure: 429 }); await loginFixture(page);
  await expect(page.getByRole('alert')).toContainText('Trop de tentatives');
  await expect(page.getByText('Nouvel essai dans', { exact: false })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Se connecter', exact: true })).toBeEnabled();
  expect(fixture.calls.filter((call) => call.path.endsWith('/auth/login')).length).toBe(1);
  await expect(page.getByLabel('Mot de passe', { exact: true })).toHaveValue('Synthetic passphrase only');
});

test('reset conflict preserves both passwords and the action for an explicit retry (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page, { resetFailure: 409 });
  await page.goto('/auth/password-reset#token=' + 'a'.repeat(43));
  await page.getByLabel('Nouveau mot de passe', { exact: true }).fill('Synthetic reset passphrase');
  await page.getByLabel('Confirmer le mot de passe', { exact: true }).fill('Synthetic reset passphrase');
  await page.getByRole('button', { name: 'Mettre à jour le mot de passe', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('n’est plus disponible');
  await expect(page.getByLabel('Nouveau mot de passe', { exact: true })).toHaveValue('Synthetic reset passphrase');
  await expect(page.getByLabel('Confirmer le mot de passe', { exact: true })).toHaveValue('Synthetic reset passphrase');
  expect(fixture.calls.filter((call) => call.path.endsWith('/password-reset/confirm')).length).toBe(1);
});

test('pending reset is single-flight and a lost response cannot show success (simulated)', async ({ page }) => {
  await identityFixture(page);
  let release: () => void = () => undefined; let calls = 0;
  const pending = new Promise<void>((resolve) => { release = resolve; });
  await page.route('**/api/web/v1/auth/password-reset/confirm', async (route) => {
    calls++; await pending; await route.abort('failed');
  });
  await page.goto('/auth/password-reset#token=' + 'a'.repeat(43));
  await page.getByLabel('Nouveau mot de passe', { exact: true }).fill('Synthetic reset passphrase');
  await page.getByLabel('Confirmer le mot de passe', { exact: true }).fill('Synthetic reset passphrase');
  await page.getByRole('button', { name: 'Mettre à jour le mot de passe', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Confirmation…', exact: true })).toBeDisabled();
  await page.locator('form').dispatchEvent('submit');
  release();
  await expect(page.getByRole('alert')).toContainText('Le résultat de cette opération n’est pas confirmé');
  expect(calls).toBe(1); await expect(page.getByRole('status')).toHaveCount(0);
});

test('email verification is never consumed by GET and resend remains generic (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page);
  await page.goto('/auth/email-verification#token=' + 'a'.repeat(43));
  await expect(page.getByRole('heading', { name: 'Confirmation de votre adresse email', exact: true })).toBeVisible();
  expect(fixture.calls.filter((call) => call.path.endsWith('/confirm')).length).toBe(0);
  await page.getByRole('button', { name: 'Confirmer mon adresse', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Votre adresse email est confirmée');
  await page.reload();
  await page.getByRole('textbox', { name: 'Adresse email', exact: true }).fill('absent@example.test');
  await page.getByRole('button', { name: 'Demander le lien', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Si cette adresse correspond à un compte éligible');
});

test('recovery-code login, password reauthentication and replacement require a new login (simulated)', async ({ page }) => {
  await identityFixture(page, { admin: true }); await loginFixture(page);
  await page.getByRole('button', { name: 'Utiliser un code de récupération', exact: true }).click();
  await page.getByLabel('Code de récupération', { exact: true }).fill('a'.repeat(43));
  await page.getByRole('button', { name: 'Vérifier le code', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Sécurité', exact: true }).click();
  await page.getByLabel('Mot de passe actuel', { exact: true }).fill('Synthetic passphrase only');
  await page.getByRole('button', { name: 'Vérifier mon mot de passe', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Votre mot de passe a été vérifié');
  await page.getByRole('button', { name: 'Remplacer le facteur', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Configurer votre application', exact: true })).toBeVisible();
  await page.getByRole('textbox', { name: 'Code à six chiffres', exact: true }).fill('123456');
  await page.getByRole('button', { name: 'Confirmer la configuration', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Conservez vos codes de récupération' })).toBeVisible();
  await page.getByLabel('J’ai conservé ces codes hors ligne', { exact: true }).check();
  await page.getByRole('button', { name: 'Continuer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
});

test('MFA refusal remains explicit, without private access or retry (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page, { admin: true, mfaFailure: 403 }); await loginFixture(page);
  await page.getByRole('textbox', { name: 'Code à six chiffres', exact: true }).fill('123456');
  await page.getByRole('button', { name: 'Vérifier le code', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Cette action est refusée');
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toHaveCount(0);
  expect(fixture.calls.filter((call) => call.path.endsWith('/mfa/challenge')).length).toBe(1);
});

for (const accountStatus of ['SUSPENDED', 'REJECTED'] as const) {
  test(`account ${accountStatus} never renders a private shell (simulated)`, async ({ page }) => {
    await identityFixture(page, { accountStatus }); await loginFixture(page);
    await expect(page.getByRole('heading', { name: 'Votre accès WorldTech', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Espace client', exact: true })).toHaveCount(0);
  });
}

test('another tab logout invalidates private content and browser-back cannot restore it (simulated)', async ({ page, context }) => {
  await identityFixture(page); await loginFixture(page);
  await expect(page.getByRole('heading', { name: 'Espace client', exact: true })).toBeVisible();
  const other = await context.newPage();
  await identityFixture(other, { authenticated: true }); await other.goto('/client');
  await expect(other.getByRole('heading', { name: 'Espace client', exact: true })).toBeVisible();
  await other.getByRole('button', { name: 'Se déconnecter', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Session indisponible', exact: true })).toBeVisible();
  await page.goBack();
  await expect(page.getByRole('heading', { name: 'Espace client', exact: true })).toHaveCount(0);
  await other.close();
});

test('MFA logout failure keeps an explicit retry rather than claiming success (simulated)', async ({ page }) => {
  await identityFixture(page, { admin: true, logoutFailure: true }); await loginFixture(page);
  await page.getByRole('button', { name: 'Se déconnecter', exact: true }).click();
  await expect(page.getByText('La déconnexion n’a pas été confirmée.', { exact: false })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Réessayer la déconnexion', exact: true })).toBeVisible();
});

test('a second action link on an already-open page is scrubbed and needs fresh confirmation (simulated)', async ({ page }) => {
  const fixture = await identityFixture(page);
  await page.goto('/auth/email-verification');
  await expect(page.getByRole('heading', { name: 'Confirmer votre adresse email', exact: true })).toBeVisible();
  await page.goto('/auth/email-verification#token=' + 'a'.repeat(43));
  await expect(page.getByRole('heading', { name: 'Confirmation de votre adresse email', exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/auth\/email-verification$/);
  expect(fixture.calls.filter((call) => call.path.endsWith('/confirm')).length).toBe(0);
  await page.getByRole('button', { name: 'Confirmer mon adresse', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Votre adresse email est confirmée');
  await page.goto('/auth/email-verification#token=' + 'b'.repeat(43));
  await expect(page.getByRole('button', { name: 'Confirmer mon adresse', exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/auth\/email-verification$/);
  expect(fixture.calls.filter((call) => call.path.endsWith('/confirm')).length).toBe(1);
});
