import { SessionState } from './identity-model';
import { safeReturnUrl } from './safe-return-url';

export function sessionDestination(state: SessionState, requested: unknown = null): string {
  if (state.kind === 'mfaRequired') return '/auth/mfa';
  if (state.kind === 'emailPending') return '/auth/email-verification';
  if (state.kind === 'authenticated') {
    const safe = safeReturnUrl(requested);
    if (safe && ((safe.startsWith('/admin') && state.user.roles.includes('ADMIN')) || (safe.startsWith('/client') && state.user.roles.includes('CLIENT')))) return safe;
    return state.user.roles.includes('ADMIN') ? '/admin' : '/client';
  }
  if (['onboarding', 'suspended', 'rejected', 'technician', 'logoutFailed', 'signingOut'].includes(state.kind)) return '/compte/acces';
  if (['unavailable', 'blocked', 'initializing'].includes(state.kind)) return '/session-indisponible';
  return '/connexion';
}
