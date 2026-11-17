import type { AuthenticationView, CsrfView, EnrollmentView, MeView, ActivationView } from '../../api/generated/types.gen';

export type { AuthenticationView, CsrfView, EnrollmentView, MeView, ActivationView };
export type WebRole = MeView['roles'][number];
export type SessionState =
  | { readonly kind: 'initializing' | 'anonymous' | 'expired' | 'unavailable' | 'blocked' | 'mfaRequired' | 'emailPending' | 'signingOut' | 'logoutFailed' | 'recoveryCodes' }
  | { readonly kind: 'authenticated' | 'onboarding' | 'suspended' | 'rejected' | 'technician'; readonly user: MeView };

export class IdentityProtocolError extends Error {
  constructor() { super('Identity response rejected'); }
}

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function parseCsrf(value: unknown): CsrfView {
  if (!record(value) || value['headerName'] !== 'X-CSRF-TOKEN' || typeof value['token'] !== 'string' || !/^[A-Za-z0-9+/=_-]{20,512}$/.test(value['token'])) throw new IdentityProtocolError();
  return { headerName: 'X-CSRF-TOKEN', token: value['token'] };
}
export function parseAuthentication(value: unknown): AuthenticationView {
  if (!record(value) || !['AUTHENTICATED', 'EMAIL_PENDING', 'MFA_PENDING'].includes(String(value['status']))) throw new IdentityProtocolError();
  return { status: value['status'] as AuthenticationView['status'] };
}
export function parseMe(value: unknown): MeView {
  if (!record(value) || typeof value['userId'] !== 'string' || !uuid.test(value['userId']) ||
    !['PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED'].includes(String(value['status'])) || typeof value['emailVerified'] !== 'boolean' ||
    !Array.isArray(value['roles']) || !value['roles'].length || value['roles'].length > 3 || new Set(value['roles']).size !== value['roles'].length ||
    !value['roles'].every((role: unknown) => role === 'ADMIN' || role === 'CLIENT' || role === 'TECHNICIAN')) throw new IdentityProtocolError();
  return { userId: value['userId'], status: value['status'] as MeView['status'], roles: [...value['roles']] as WebRole[], emailVerified: value['emailVerified'] };
}
export function parseEnrollment(value: unknown): EnrollmentView {
  if (!record(value) || typeof value['enrollmentId'] !== 'string' || !uuid.test(value['enrollmentId']) ||
    typeof value['secret'] !== 'string' || !/^[A-Z2-7]{16,128}$/.test(value['secret']) ||
    typeof value['uri'] !== 'string' || !value['uri'].startsWith('otpauth://totp/') || value['uri'].length > 2048) throw new IdentityProtocolError();
  return { enrollmentId: value['enrollmentId'], secret: value['secret'], uri: value['uri'] };
}
export function parseActivation(value: unknown): ActivationView {
  if (!record(value) || !['AUTHENTICATED', 'REAUTHENTICATION_REQUIRED'].includes(String(value['status'])) ||
    !Array.isArray(value['recoveryCodes']) || value['recoveryCodes'].length !== 10 || new Set(value['recoveryCodes']).size !== 10 ||
    !value['recoveryCodes'].every((code: unknown) => typeof code === 'string' && /^[A-Za-z0-9_-]{43}$/.test(code))) throw new IdentityProtocolError();
  return { status: value['status'] as ActivationView['status'], recoveryCodes: [...value['recoveryCodes']] as ActivationView['recoveryCodes'] };
}
export function stateFromMe(user: MeView): SessionState {
  if (user.status === 'SUSPENDED') return { kind: 'suspended', user };
  if (user.status === 'REJECTED') return { kind: 'rejected', user };
  if (!user.emailVerified) return { kind: 'emailPending' };
  if (user.status === 'PENDING') return { kind: 'onboarding', user };
  if (!user.roles.includes('ADMIN') && !user.roles.includes('CLIENT')) return { kind: 'technician', user };
  // A successful /me is the server's proof of effective identity, including ADMIN MFA.
  return { kind: 'authenticated', user };
}
