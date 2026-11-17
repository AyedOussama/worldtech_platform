type Action = { readonly purpose: 'reset' | 'verify'; readonly token: string };
let captured: Action | null = null;

/** Runs before runtime fetch/bootstrap. Never persist action secrets or consume them by GET. */
export function captureActionFragment(browser: Pick<Window, 'location' | 'history'>): void {
  const purpose = browser.location.pathname === '/auth/password-reset' ? 'reset' : browser.location.pathname === '/auth/email-verification' ? 'verify' : null;
  if (!purpose) return;
  const params = new URLSearchParams(browser.location.hash.slice(1));
  const values = params.getAll('token');
  browser.history.replaceState(null, '', browser.location.pathname);
  captured = values.length === 1 && /^[A-Za-z0-9_-]{43}$/.test(values[0] ?? '') ? { purpose, token: values[0] ?? '' } : null;
}
export function consumeActionToken(purpose: Action['purpose']): string | null {
  const token = captured?.purpose === purpose ? captured.token : null;
  captured = null;
  return token;
}
