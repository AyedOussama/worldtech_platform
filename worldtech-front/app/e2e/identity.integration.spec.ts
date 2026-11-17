import { expect, Locator, Page, test } from '@playwright/test';
import { randomBytes } from 'node:crypto';
import { createHmac } from 'node:crypto';

// Test-only RFC 6238 via Node HMAC. Never part of the Angular application.
function totp(secret: string): string {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  let bits = ''; for (const character of secret) bits += alphabet.indexOf(character).toString(2).padStart(5, '0');
  const bytes: number[] = []; for (let offset = 0; offset + 8 <= bits.length; offset += 8) bytes.push(Number.parseInt(bits.slice(offset, offset + 8), 2));
  const counter = Buffer.alloc(8); counter.writeBigUInt64BE(BigInt(Math.floor(Date.now() / 30_000)));
  const digest = createHmac('sha1', Buffer.from(bytes)).update(counter).digest(); const offset = (digest[digest.length - 1] ?? 0) & 15;
  return ((digest.readUInt32BE(offset) & 0x7fffffff) % 1_000_000).toString().padStart(6, '0');
}
async function sensitiveFill(locator: Locator, value: string): Promise<void> {
  try { await locator.fill(value); } catch { throw new Error('Sensitive field could not be filled; diagnostics intentionally redacted'); }
}
async function login(page: Page, admin = false, email?: string) {
  const password = process.env[admin ? 'WT_TEST_ADMIN_PASSWORD' : 'WT_TEST_CLIENT_PASSWORD'];
  if (!password) throw new Error('Isolated fixture credentials unavailable');
  await page.goto('/connexion');
  await page.getByRole('textbox', { name: 'E-mail professionnel', exact: true }).fill(email ?? (admin ? 'admin-fe03@example.test' : 'client-fe03@example.test'));
  await sensitiveFill(page.getByLabel('Mot de passe', { exact: true }), password);
  await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
}
test('Spring real: HttpOnly session, CSRF refusal/rotation, client login and logout via Angular proxy', async ({ page, context }) => {
  const csrfResponse = await context.request.get('/api/web/v1/auth/csrf'); expect(csrfResponse.status()).toBe(200);
  const before = (await csrfResponse.json()) as { token: string };
  const denied = await context.request.post('/api/web/v1/auth/login', { data: { email: 'client-fe03@example.test', password: 'irrelevant' } }); expect(denied.status()).toBe(403);
  const altered = await context.request.post('/api/web/v1/auth/login', { headers: { 'X-CSRF-TOKEN': 'altered' }, data: { email: 'client-fe03@example.test', password: 'irrelevant' } }); expect(altered.status()).toBe(403);
  await login(page);
  await expect(page.getByRole('heading', { name: 'Espace client', exact: true })).toBeVisible();
  const cookies = await context.cookies();
  expect(cookies.some((cookie) => cookie.name === 'MAINT_TEST_SESSION' && cookie.httpOnly && cookie.sameSite === 'Lax')).toBe(true);
  expect(await page.evaluate(() => document.cookie.includes('MAINT_TEST_SESSION'))).toBe(false);
  const current = (await (await context.request.get('/api/web/v1/auth/csrf')).json()) as { token: string };
  expect(current.token !== before.token).toBe(true);
  expect((await context.request.post('/api/web/v1/auth/logout', { headers: { 'X-CSRF-TOKEN': before.token }, data: {} })).status()).toBe(403);
  await page.getByRole('button', { name: 'Se déconnecter', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
  expect((await context.request.get('/api/web/v1/me')).status()).toBe(401);
  expect(await page.evaluate(() => Object.keys(localStorage).filter((key) => key !== 'wt-theme').length)).toBe(0);
});
test('Spring real: admin enrollment, one-use recovery, reauthentication, code renewal and rate limit', async ({ page, context }) => {
  test.setTimeout(60_000);
  await login(page, true);
  await expect(page.getByRole('heading', { name: 'Vérifier votre accès', exact: true })).toBeVisible();
  expect((await context.request.get('/api/web/v1/me')).status()).toBe(403);
  expect((await context.request.get('/api/web/v1/admin/client-accounts')).status()).toBe(403);
  await page.getByRole('button', { name: 'Configurer mon application', exact: true }).click();
  await page.getByLabel('Afficher la clé de configuration', { exact: true }).check();
  const secret = (await page.getByLabel('Clé de configuration TOTP').textContent())?.trim();
  if (!secret) throw new Error('No enrollment secret received');
  await sensitiveFill(page.getByRole('textbox', { name: 'Code à six chiffres', exact: true }), totp(secret));
  await page.getByRole('button', { name: 'Confirmer la configuration', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Conservez vos codes de récupération' })).toBeVisible();
  const codes = await page.getByRole('list', { name: 'Codes de récupération' }).getByRole('listitem').allTextContents();
  expect(codes.length === 10).toBe(true);
  await page.getByLabel('J’ai conservé ces codes hors ligne', { exact: true }).check();
  await page.getByRole('button', { name: 'Continuer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  expect((await context.request.get('/api/web/v1/admin/client-accounts')).status()).toBe(200);
  await page.reload(); await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Se déconnecter', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();

  await login(page, true);
  await page.getByRole('button', { name: 'Utiliser un code de récupération', exact: true }).click();
  await sensitiveFill(page.getByLabel('Code de récupération', { exact: true }), codes[0]?.trim() ?? '');
  await page.getByRole('button', { name: 'Vérifier le code', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Sécurité', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Double authentification', exact: true })).toBeVisible();
  // Reusing the consumed recovery code is denied without discarding the active session.
  await page.getByRole('button', { name: 'Utiliser un code de récupération', exact: true }).click();
  await sensitiveFill(page.getByLabel('Code de récupération', { exact: true }), codes[0]?.trim() ?? '');
  await page.getByRole('button', { name: 'Vérifier le code', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Cette action est refusée');
  expect((await context.request.get('/api/web/v1/admin/client-accounts')).status()).toBe(200);
  await sensitiveFill(page.getByLabel('Mot de passe actuel', { exact: true }), process.env['WT_TEST_ADMIN_PASSWORD'] ?? '');
  await page.getByRole('button', { name: 'Vérifier mon mot de passe', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Votre mot de passe a été vérifié');
  await page.getByRole('button', { name: 'Renouveler les codes de récupération', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Conservez vos codes de récupération' })).toBeVisible();
  const renewed = await page.getByRole('list', { name: 'Codes de récupération' }).getByRole('listitem').allTextContents();
  expect(renewed.length === 10 && renewed.every((code) => !codes.includes(code))).toBe(true);
  await page.getByLabel('J’ai conservé ces codes hors ligne', { exact: true }).check();
  await page.getByRole('button', { name: 'Continuer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Sécurité', exact: true }).click();
  const limited = page.waitForResponse((response) => response.url().endsWith('/auth/mfa/replacement/start'));
  await page.getByRole('button', { name: 'Remplacer le facteur', exact: true }).click();
  expect((await limited).status()).toBe(429);
  await expect(page.getByRole('alert')).toContainText('Trop de tentatives');
  await expect(page.getByText('Nouvel essai dans', { exact: false })).toBeVisible();
  expect((await context.request.get('/api/web/v1/admin/client-accounts')).status()).toBe(200);
});

test('Spring real: factor replacement revokes sessions and requires new password plus MFA proof', async ({ page, context }) => {
  test.setTimeout(60_000);
  const email = 'replacement-admin-fe03@example.test';
  await login(page, true, email);
  await page.getByRole('button', { name: 'Configurer mon application', exact: true }).click();
  await page.getByLabel('Afficher la clé de configuration', { exact: true }).check();
  const original = (await page.getByLabel('Clé de configuration TOTP').textContent())?.trim();
  if (!original) throw new Error('No initial secret received');
  await sensitiveFill(page.getByRole('textbox', { name: 'Code à six chiffres', exact: true }), totp(original));
  await page.getByRole('button', { name: 'Confirmer la configuration', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Conservez vos codes de récupération' })).toBeVisible();
  await page.getByLabel('J’ai conservé ces codes hors ligne', { exact: true }).check();
  await page.getByRole('button', { name: 'Continuer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Sécurité', exact: true }).click();
  await page.getByRole('button', { name: 'Remplacer le facteur', exact: true }).click();
  await page.getByLabel('Afficher la clé de configuration', { exact: true }).check();
  const replacement = (await page.getByLabel('Clé de configuration TOTP').textContent())?.trim();
  if (!replacement) throw new Error('No replacement secret received');
  await sensitiveFill(page.getByRole('textbox', { name: 'Code à six chiffres', exact: true }), totp(replacement));
  await page.getByRole('button', { name: 'Confirmer la configuration', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Conservez vos codes de récupération' })).toBeVisible();
  const replacementCodes = await page.getByRole('list', { name: 'Codes de récupération' }).getByRole('listitem').allTextContents();
  expect((await context.request.get('/api/web/v1/me')).status()).toBe(401);
  await page.getByLabel('J’ai conservé ces codes hors ligne', { exact: true }).check();
  await page.getByRole('button', { name: 'Continuer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Connexion à votre espace', exact: true })).toBeVisible();
  await login(page, true, email);
  await page.getByRole('button', { name: 'Utiliser un code de récupération', exact: true }).click();
  await sensitiveFill(page.getByLabel('Code de récupération', { exact: true }), replacementCodes[0]?.trim() ?? '');
  await page.getByRole('button', { name: 'Vérifier le code', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Administration', exact: true })).toBeVisible();
  expect((await context.request.get('/api/web/v1/admin/client-accounts')).status()).toBe(200);
});

test('Spring real: password action is explicit and single-use, reset revokes existing sessions', async ({ page, context, browser }) => {
  const token = process.env['WT_TEST_RESET_TOKEN'];
  if (!token) throw new Error('Isolated action token unavailable');
  const observer = await browser.newContext({ baseURL: 'http://localhost:4311' });
  try {
    const privatePage = await observer.newPage(); await login(privatePage);
    await expect(privatePage.getByRole('heading', { name: 'Espace client', exact: true })).toBeVisible();
    const unknown = await context.request.get('/api/web/v1/auth/csrf');
    const csrf = (await unknown.json()) as { token: string };
    expect((await context.request.post('/api/web/v1/auth/password-reset/requests', { headers: { 'X-CSRF-TOKEN': csrf.token }, data: { email: 'absent-fe03@example.test' } })).status()).toBe(202);
    await page.goto('/auth/password-reset#token=' + token);
    await expect(page.getByRole('heading', { name: 'Nouveau mot de passe', exact: true })).toBeVisible();
    expect(page.url().includes('#')).toBe(false);
    expect((await observer.request.get('/api/web/v1/me')).status()).toBe(200); // GET has not consumed it.
    const password = randomBytes(24).toString('base64url') + ' isolated reset';
    await sensitiveFill(page.getByLabel('Nouveau mot de passe', { exact: true }), password);
    await sensitiveFill(page.getByLabel('Confirmer le mot de passe', { exact: true }), password);
    await page.getByRole('button', { name: 'Mettre à jour le mot de passe', exact: true }).click();
    await expect(page.getByRole('status')).toContainText('Votre mot de passe a été mis à jour');
    expect((await observer.request.get('/api/web/v1/me')).status()).toBe(401);
    await page.goto('/auth/password-reset#token=' + token);
    await sensitiveFill(page.getByLabel('Nouveau mot de passe', { exact: true }), password);
    await sensitiveFill(page.getByLabel('Confirmer le mot de passe', { exact: true }), password);
    await page.getByRole('button', { name: 'Mettre à jour le mot de passe', exact: true }).click();
    await expect(page.getByRole('alert')).toContainText('Le lien ou le code peut être invalide');
  } finally { await observer.close(); }
});

test('Spring real: email action is single-use and never bypasses client approval', async ({ page, context }) => {
  const token = process.env['WT_TEST_VERIFICATION_TOKEN'];
  if (!token) throw new Error('Isolated action token unavailable');
  await login(page, false, 'pending-fe03@example.test');
  await expect(page.getByRole('heading', { name: 'Confirmer votre adresse email', exact: true })).toBeVisible();
  await page.goto('/auth/email-verification#token=' + token);
  await expect(page.getByRole('heading', { name: 'Confirmation de votre adresse email', exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/auth\/email-verification$/);
  await page.getByRole('button', { name: 'Confirmer mon adresse', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Votre adresse email est confirmée');
  await login(page, false, 'pending-fe03@example.test');
  await expect(page.getByRole('heading', { name: 'Votre accès WorldTech', exact: true })).toBeVisible();
  await expect(page.getByText('Votre compte est en attente de validation.', { exact: false })).toBeVisible();
  expect((await context.request.get('/api/web/v1/client/accounts')).status()).toBe(403);
  await page.goto('/auth/email-verification#token=' + token);
  await page.getByRole('button', { name: 'Confirmer mon adresse', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Le lien ou le code peut être invalide');
});
