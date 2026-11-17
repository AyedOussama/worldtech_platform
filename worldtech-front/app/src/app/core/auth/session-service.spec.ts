import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RUNTIME_CONFIG, parseRuntimeConfig } from '../config/runtime-config';
import { apiFailureInterceptor } from '../http/api-failure.interceptor';
import { webSessionInterceptor } from '../http/web-session.interceptor';
import { CsrfContext } from './csrf-context';
import { SessionService } from './session-service';
import { SessionStore } from './session-state';
import { IdentityProtocolError, parseMe } from './identity-model';

const base = '/api/web/v1';
const csrfA = 'a'.repeat(36); const csrfB = 'b'.repeat(36); const csrfC = 'c'.repeat(36);
const client = { userId: '00000000-0000-4000-8000-000000000001', status: 'ACTIVE', roles: ['CLIENT'], emailVerified: true };
const admin = { ...client, roles: ['ADMIN'] };

describe('real web identity protocol (HTTP double, not Spring integration)', () => {
  let requests: HttpTestingController; let service: SessionService; let http: HttpClient; let store: SessionStore;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      { provide: RUNTIME_CONFIG, useValue: parseRuntimeConfig({ appName: 'WorldTech', apiBaseUrl: base, googleLoginEnabled: false, telemetryEnabled: false, buildVersion: 'test' }) },
      provideHttpClient(withInterceptors([apiFailureInterceptor, webSessionInterceptor])), provideHttpClientTesting(),
    ] });
    requests = TestBed.inject(HttpTestingController); service = TestBed.inject(SessionService); http = TestBed.inject(HttpClient); store = TestBed.inject(SessionStore);
  });
  afterEach(() => { requests.verify(); });
  const flushCsrf = (requests: HttpTestingController, token: string) => requests.expectOne(base + '/auth/csrf').flush({ headerName: 'X-CSRF-TOKEN', token });

  it('single-flights CSRF for concurrent mutations, preserves bodies and never retries', () => {
    http.post(base + '/auth/password-reset/requests', { email: 'one@example.test' }).subscribe();
    http.post(base + '/auth/email-verification/requests', { email: 'two@example.test' }).subscribe();
    flushCsrf(requests, csrfA);
    for (const path of ['password-reset', 'email-verification']) {
      const request = requests.expectOne(base + '/auth/' + path + '/requests');
      expect(request.request.headers.get('X-CSRF-TOKEN')).toBe(csrfA);
      expect(request.request.headers.has('Authorization')).toBe(false); request.flush(null, { status: 202, statusText: 'Accepted' });
    }
  });

  it.each(['https://outside.example/api/web/v1/auth/login', '//outside.example/api/web/v1/auth/login', '/api/mobile/v1/auth/login', '/api/web/v1/%2foutside', '/api/web/v1/../mobile/v1/login', '/api/web/v1/auth/./login'])('does not attach the web CSRF secret to %s', (url) => {
    http.post(url, {}).subscribe();
    const request = requests.expectOne(url); expect(request.request.headers.has('X-CSRF-TOKEN')).toBe(false); request.flush({});
    requests.expectNone(base + '/auth/csrf');
  });

  it('refuses a malicious CSRF header name rather than forwarding it', () => {
    const error = vi.fn(); http.post(base + '/auth/login', {}).subscribe({ error });
    requests.expectOne(base + '/auth/csrf').flush({ headerName: 'Authorization', token: csrfA });
    expect(error).toHaveBeenCalledWith(expect.any(IdentityProtocolError)); requests.expectNone(base + '/auth/login');
  });

  it('rotates CSRF after login and MFA, grants admin only after server /me, suppresses duplicate login', () => {
    service.login({ email: 'admin@example.test', password: 'not-a-real-password' }).subscribe();
    const duplicate = vi.fn(); service.login({ email: 'other@example.test', password: 'x' }).subscribe({ error: duplicate });
    expect(duplicate).toHaveBeenCalledOnce();
    flushCsrf(requests, csrfA);
    const login = requests.expectOne(base + '/auth/login'); expect(login.request.headers.get('X-CSRF-TOKEN')).toBe(csrfA);
    login.flush({ status: 'MFA_PENDING' }); flushCsrf(requests, csrfB);
    expect(store.state().kind).toBe('mfaRequired'); expect(store.allows('ADMIN')).toBe(false); requests.expectNone(base + '/me');
    service.prove('123456').subscribe();
    const challenge = requests.expectOne(base + '/auth/mfa/challenge'); expect(challenge.request.headers.get('X-CSRF-TOKEN')).toBe(csrfB);
    challenge.flush(null, { status: 204, statusText: 'No Content' });
    expect(store.allows('ADMIN')).toBe(false); flushCsrf(requests, csrfC);
    requests.expectOne(base + '/me').flush(admin); expect(store.allows('ADMIN')).toBe(true);
  });

  it.each([{ status: 401, expected: 'anonymous' }, { status: 403, expected: 'blocked' }, { status: 503, expected: 'unavailable' }])('distinguishes startup failure $status from a proven MFA challenge', ({ status, expected }) => {
    service.resolve().subscribe(); requests.expectOne(base + '/me').flush({}, { status, statusText: 'Failure' });
    expect(store.state().kind).toBe(expected); expect(store.allows('ADMIN')).toBe(false);
  });

  it('keeps a network failure unavailable rather than anonymous', () => {
    service.resolve().subscribe(); requests.expectOne(base + '/me').error(new ProgressEvent('error'));
    expect(store.state().kind).toBe('unavailable');
  });

  it.each([{ ...client, status: 'PENDING' }, { ...client, emailVerified: false }, { ...client, status: 'SUSPENDED' }, { ...client, roles: ['TECHNICIAN'] }])('does not grant client/admin based on stored roles alone: %j', (user) => {
    service.resolve().subscribe(); requests.expectOne(base + '/me').flush(user);
    expect(store.allows('CLIENT')).toBe(false); expect(store.allows('ADMIN')).toBe(false);
  });

  it('cancels private requests and ignores a late identity probe after logout', () => {
    store.replace({ kind: 'authenticated', user: parseMe(client) });
    const next = vi.fn(); http.get(base + '/client/accounts').subscribe(next);
    const privateRequest = requests.expectOne(base + '/client/accounts');
    service.resolve(true).subscribe(); const oldMe = requests.expectOne(base + '/me');
    service.logout().subscribe(); expect(privateRequest.cancelled).toBe(true);
    flushCsrf(requests, csrfA); requests.expectOne(base + '/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
    oldMe.flush(admin);
    expect(store.state().kind).toBe('anonymous'); expect(next).not.toHaveBeenCalled(); expect(store.allows('ADMIN')).toBe(false);
  });

  it('expires once for concurrent 401 and never rewrites a 403 as logout', () => {
    store.replace({ kind: 'authenticated', user: parseMe(client) });
    http.get(base + '/client/accounts').subscribe({ error: () => undefined });
    requests.expectOne(base + '/client/accounts').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(store.allows('CLIENT')).toBe(true);
    http.get(base + '/client/accounts').subscribe({ error: () => undefined });
    http.get(base + '/client/accounts/other').subscribe({ error: () => undefined });
    const first = requests.expectOne(base + '/client/accounts'); const second = requests.expectOne(base + '/client/accounts/other');
    first.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(store.state().kind).toBe('expired'); expect(second.cancelled).toBe(true);
  });

  it('does not claim server logout success on a network failure or replay the command', () => {
    store.replace({ kind: 'authenticated', user: parseMe(client) });
    const error = vi.fn(); service.logout().subscribe({ error }); flushCsrf(requests, csrfA);
    requests.expectOne(base + '/auth/logout').error(new ProgressEvent('error'));
    expect(error).toHaveBeenCalledOnce(); expect(store.state().kind).toBe('logoutFailed'); expect(store.allows('CLIENT')).toBe(false);
    requests.expectNone(base + '/auth/logout');
  });

  it('does not cache or transmit a stale CSRF response after identity invalidation', () => {
    const csrf = TestBed.inject(CsrfContext); const error = vi.fn(); csrf.ensure().subscribe({ error });
    const stale = requests.expectOne(base + '/auth/csrf'); csrf.clear(); stale.flush({ headerName: 'X-CSRF-TOKEN', token: csrfA });
    expect(error).toHaveBeenCalledWith(expect.any(IdentityProtocolError));
    csrf.ensure().subscribe(); flushCsrf(requests, csrfB);
  });

  it('does not reuse an old pending /me after the session generation changed', () => {
    service.resolve().subscribe(); const old = requests.expectOne(base + '/me');
    store.replace({ kind: 'blocked' });
    service.resolve(true).subscribe(); const current = requests.expectOne(base + '/me');
    old.flush(admin); expect(store.state().kind).toBe('blocked');
    current.flush(client); expect(store.allows('CLIENT')).toBe(true); expect(store.allows('ADMIN')).toBe(false);
  });

  it('never restores MFA context from a login response whose CSRF rotation finished after invalidation', () => {
    const error = vi.fn(); service.login({ email: 'admin@example.test', password: 'not-real' }).subscribe({ error });
    flushCsrf(requests, csrfA); requests.expectOne(base + '/auth/login').flush({ status: 'MFA_PENDING' });
    const rotated = requests.expectOne(base + '/auth/csrf');
    store.replace({ kind: 'blocked' }); rotated.flush({ headerName: 'X-CSRF-TOKEN', token: csrfB });
    expect(error).toHaveBeenCalledWith(expect.any(IdentityProtocolError));
    expect(store.state().kind).toBe('blocked'); expect(service.busy()).toBe(false);
  });

  it('never restores access from late reauthentication after an identity change', () => {
    store.replace({ kind: 'authenticated', user: parseMe(admin) });
    const error = vi.fn(); service.reauthenticate('not-real').subscribe({ error });
    flushCsrf(requests, csrfA); const pending = requests.expectOne(base + '/auth/reauthenticate');
    store.replace({ kind: 'blocked' }); pending.flush(null, { status: 204, statusText: 'No Content' });
    expect(error).toHaveBeenCalledWith(expect.any(IdentityProtocolError));
    expect(store.state().kind).toBe('blocked'); requests.expectNone(base + '/me');
  });

  it('does not stay initializing forever if post-MFA CSRF renewal fails', () => {
    store.replace({ kind: 'mfaRequired' });
    const error = vi.fn(); service.prove('123456').subscribe({ error });
    flushCsrf(requests, csrfA); requests.expectOne(base + '/auth/mfa/challenge').flush(null, { status: 204, statusText: 'No Content' });
    requests.expectOne(base + '/auth/csrf').error(new ProgressEvent('error'));
    expect(error).toHaveBeenCalledOnce(); expect(store.state().kind).toBe('unavailable'); expect(service.busy()).toBe(false);
    requests.expectNone(base + '/me');
  });

  it('cancels a private mutation waiting for CSRF before it reaches the server', () => {
    store.replace({ kind: 'authenticated', user: parseMe(client) });
    const completed = vi.fn(); http.post(base + '/client/accounts', {}).subscribe({ complete: completed });
    const pending = requests.expectOne(base + '/auth/csrf');
    store.replace({ kind: 'blocked' });
    expect(pending.cancelled).toBe(true); expect(completed).toHaveBeenCalledOnce(); requests.expectNone(base + '/client/accounts');
  });

  it.each([{ ...client, userId: '-'.repeat(36) }, { ...client, roles: ['ADMIN', 'ADMIN'] }])('rejects malformed identities rather than trusting their roles', (user) => {
    service.resolve().subscribe(); requests.expectOne(base + '/me').flush(user);
    expect(store.state().kind).toBe('unavailable'); expect(store.allows('ADMIN')).toBe(false);
  });
});
