import { Page } from '@playwright/test';

// Browser-only HTTP double. This fixture is never imported by Angular or used on API failure.
export async function identityFixture(page: Page, options: {
  admin?: boolean; pending?: boolean; authenticated?: boolean; loginFailure?: number; logoutFailure?: boolean;
  resetFailure?: number; mfaFailure?: number; accountStatus?: 'ACTIVE' | 'SUSPENDED' | 'REJECTED';
} = {}) {
  let state: 'anonymous' | 'mfa' | 'active' = options.authenticated ? 'active' : 'anonymous';
  let revision = 0;
  let replacement = false;
  let codeSet = 0;
  const calls: { method: string; path: string }[] = [];
  await page.route('**/api/web/v1/**', async (route) => {
    const request = route.request(); const path = new URL(request.url()).pathname; const method = request.method();
    calls.push({ method, path });
    const reply = (status: number, json?: unknown) => route.fulfill({ status, ...(json === undefined ? { body: '' } : { json }) });
    if (path.endsWith('/auth/csrf')) return reply(200, { headerName: 'X-CSRF-TOKEN', token: String(revision).padEnd(36, 'x') });
    if (path.endsWith('/me')) {
      if (state !== 'active') return reply(state === 'mfa' ? 403 : 401, { status: state === 'mfa' ? 403 : 401, code: 'ACCESS_DENIED' });
      return reply(200, { userId: '00000000-0000-4000-8000-000000000001', status: options.pending ? 'PENDING' : options.accountStatus ?? 'ACTIVE', roles: [options.admin ? 'ADMIN' : 'CLIENT'], emailVerified: true });
    }
    if (method !== 'GET' && request.headers()['x-csrf-token'] !== String(revision).padEnd(36, 'x')) return reply(403, { status: 403, code: 'ACCESS_DENIED' });
    if (path.endsWith('/auth/login')) {
      if (options.loginFailure) return route.fulfill({ status: options.loginFailure, headers: options.loginFailure === 429 ? { 'Retry-After': '2' } : {}, json: { status: options.loginFailure, code: 'AUTHENTICATION_REQUIRED' } });
      state = options.admin ? 'mfa' : 'active'; revision++;
      return reply(200, { status: options.admin ? 'MFA_PENDING' : 'AUTHENTICATED' });
    }
    if (path.endsWith('/auth/logout')) {
      if (options.logoutFailure) return route.abort('failed');
      state = 'anonymous'; revision++; return reply(204);
    }
    if (path.endsWith('/auth/mfa/challenge') || path.endsWith('/auth/mfa/recovery')) {
      if (options.mfaFailure) return reply(options.mfaFailure, { status: options.mfaFailure, code: 'ACCESS_DENIED' });
      state = 'active'; revision++; return reply(204);
    }
    if (path.endsWith('/auth/reauthenticate')) { revision++; return reply(204); }
    if (path.endsWith('/auth/mfa/enrollment/start') || path.endsWith('/auth/mfa/replacement/start')) {
      replacement = path.includes('/replacement/');
      return reply(200, { enrollmentId: '00000000-0000-4000-8000-000000000002', secret: 'JBSWY3DPEHPK3PXP', uri: 'otpauth://totp/Synthetic?secret=JBSWY3DPEHPK3PXP' });
    }
    if (path.endsWith('/auth/mfa/enrollment/confirm')) {
      state = replacement ? 'anonymous' : 'active'; revision++;
      return reply(200, { status: replacement ? 'REAUTHENTICATION_REQUIRED' : 'AUTHENTICATED', recoveryCodes: Array.from({ length: 10 }, (_, index) => String(index).padEnd(43, 'a')) });
    }
    if (path.endsWith('/auth/mfa/recovery-codes/replace')) return reply(200, { status: 'AUTHENTICATED', recoveryCodes: Array.from({ length: 10 }, (_, index) => `${codeSet++}_${index}`.padEnd(43, 'b')) });
    if (path.endsWith('/requests')) return reply(202);
    if (path.endsWith('/auth/password-reset/confirm')) {
      if (options.resetFailure) return reply(options.resetFailure, { status: options.resetFailure, code: 'INVALID_INPUT' });
      state = 'anonymous'; revision++; return reply(204);
    }
    if (path.endsWith('/confirm')) return reply(204);
    return reply(404, { status: 404, code: 'RESOURCE_NOT_FOUND' });
  });
  return { calls };
}

export async function loginFixture(page: Page) {
  await page.goto('/connexion');
  await page.getByRole('textbox', { name: 'E-mail professionnel', exact: true }).fill('fixture@example.test');
  await page.getByLabel('Mot de passe', { exact: true }).fill('Synthetic passphrase only');
  await page.getByRole('button', { name: 'Se connecter', exact: true }).click();
}
