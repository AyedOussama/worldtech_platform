import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import type { Login, Reset, Email, Enrollment, Totp, Recovery, Password } from '../../api/generated/types.gen';
import { RUNTIME_CONFIG } from '../config/runtime-config';
import { IDENTITY_PROBE } from '../http/web-session.interceptor';
import { parseActivation, parseAuthentication, parseEnrollment, parseMe } from './identity-model';

/** Narrow Angular transport over generated OpenAPI types. No generated source is edited. */
@Injectable({ providedIn: 'root' })
export class IdentityApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;
  private readonly options = { timeout: 10_000, headers: { Accept: 'application/json' } };
  me() { return this.http.get<unknown>(`${this.base}/me`, { ...this.options, context: new HttpContext().set(IDENTITY_PROBE, true) }).pipe(map(parseMe)); }
  login(body: Login) { return this.http.post<unknown>(`${this.base}/auth/login`, body, this.options).pipe(map(parseAuthentication)); }
  logout() { return this.http.post<void>(`${this.base}/auth/logout`, {}, this.options); }
  requestReset(body: Email) { return this.http.post<void>(`${this.base}/auth/password-reset/requests`, body, this.options); }
  requestVerification(body: Email) { return this.http.post<void>(`${this.base}/auth/email-verification/requests`, body, this.options); }
  reset(body: Reset) { return this.http.post<void>(`${this.base}/auth/password-reset/confirm`, body, this.options); }
  verify(token: string) { return this.http.post<void>(`${this.base}/auth/email-verification/confirm`, { token }, this.options); }
  startEnrollment(replacement = false) { return this.http.post<unknown>(`${this.base}/auth/mfa/${replacement ? 'replacement' : 'enrollment'}/start`, {}, this.options).pipe(map(parseEnrollment)); }
  confirmEnrollment(body: Enrollment) { return this.http.post<unknown>(`${this.base}/auth/mfa/enrollment/confirm`, body, this.options).pipe(map(parseActivation)); }
  challenge(body: Totp) { return this.http.post<void>(`${this.base}/auth/mfa/challenge`, body, this.options); }
  recovery(body: Recovery) { return this.http.post<void>(`${this.base}/auth/mfa/recovery`, body, this.options); }
  replaceCodes() { return this.http.post<unknown>(`${this.base}/auth/mfa/recovery-codes/replace`, {}, this.options).pipe(map(parseActivation)); }
  reauthenticate(body: Password) { return this.http.post<void>(`${this.base}/auth/reauthenticate`, body, this.options); }
}
