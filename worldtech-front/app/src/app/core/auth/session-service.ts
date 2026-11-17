import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, defer, finalize, map, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import type { Login } from '../../api/generated/types.gen';
import { IdentityApi } from './identity-api';
import { CsrfContext } from './csrf-context';
import { ActivationView, IdentityProtocolError, SessionState, stateFromMe } from './identity-model';
import { SessionStore } from './session-state';
import { SessionEvents } from './session-events';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly api = inject(IdentityApi);
  private readonly csrf = inject(CsrfContext);
  private readonly events = inject(SessionEvents);
  readonly store = inject(SessionStore);
  readonly state = this.store.state;
  private readonly working = signal(false);
  readonly busy = this.working.asReadonly();
  private pending: { readonly generation: number; readonly request: Observable<SessionState> } | undefined;

  resolve(force = false): Observable<SessionState> {
    if (!force && this.state().kind !== 'initializing') return of(this.state());
    if (this.pending?.generation === this.store.generation) return this.pending.request;
    const generation = this.store.generation;
    const request = this.api.me().pipe(
      map(stateFromMe),
      catchError((error: unknown) => {
        const status = error && typeof error === 'object' && 'status' in error ? error.status : undefined;
        return of<SessionState>({ kind: status === 401 ? 'anonymous' : status === 403 ? 'blocked' : 'unavailable' });
      }),
      map((state) => {
        if (generation === this.store.generation) this.store.replace(state);
        return this.state();
      }),
      finalize(() => { if (this.pending?.request === request) this.pending = undefined; }),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    this.pending = { generation, request };
    return request;
  }

  login(body: Login): Observable<SessionState> {
    return this.exclusive(() => {
      this.pending = undefined;
      this.events.changed();
      this.store.replace({ kind: 'initializing' }); this.csrf.clear();
      const generation = this.store.generation;
      return this.api.login(body).pipe(
        switchMap((result) => {
          this.assertGeneration(generation);
          this.csrf.clear();
          return this.csrf.ensure().pipe(switchMap(() => {
            this.assertGeneration(generation);
            if (result.status === 'MFA_PENDING') { this.store.replace({ kind: 'mfaRequired' }); return of(this.state()); }
            if (result.status === 'EMAIL_PENDING') { this.store.replace({ kind: 'emailPending' }); return of(this.state()); }
            return this.resolve(true);
          }));
        }),
        catchError((error: unknown) => {
          if (generation === this.store.generation) {
            const status = error && typeof error === 'object' && 'status' in error ? error.status : undefined;
            this.store.replace({ kind: [400, 401, 429].includes(Number(status)) ? 'anonymous' : status === 403 ? 'blocked' : 'unavailable' });
          }
          return throwError(() => error);
        }),
      );
    });
  }

  logout(): Observable<void> {
    return this.exclusive(() => {
      this.store.replace({ kind: 'signingOut' });
      this.events.changed();
      const generation = this.store.generation;
      return this.api.logout().pipe(
        tap(() => { this.assertGeneration(generation); this.csrf.clear(); this.store.replace({ kind: 'anonymous' }); }),
        catchError((error: unknown) => {
          if (generation === this.store.generation) { this.csrf.clear(); this.store.replace({ kind: 'logoutFailed' }); }
          return throwError(() => error);
        }),
      );
    });
  }

  prove(code: string, recovery = false): Observable<SessionState> {
    return this.exclusive(() => {
      const generation = this.store.generation;
      return (recovery ? this.api.recovery({ code }) : this.api.challenge({ code })).pipe(switchMap(() => {
        this.assertGeneration(generation);
        return this.afterRotation();
      }));
    });
  }

  enroll(enrollmentId: string, code: string): Observable<ActivationView> {
    return this.exclusive(() => {
      const generation = this.store.generation;
      return this.api.confirmEnrollment({ enrollmentId, code }).pipe(tap(() => {
        this.assertGeneration(generation);
        this.csrf.clear(); this.store.replace({ kind: 'recoveryCodes' });
      }));
    });
  }

  reauthenticate(password: string): Observable<SessionState> {
    return this.exclusive(() => {
      const generation = this.store.generation;
      return this.api.reauthenticate({ password }).pipe(switchMap(() => {
        this.assertGeneration(generation);
        return this.afterRotation();
      }));
    });
  }

  finishRecoveryCodes(requiresLogin: boolean): Observable<SessionState> {
    return this.exclusive(() => {
      if (requiresLogin) {
        this.pending = undefined; this.events.changed();
        this.csrf.clear(); this.store.replace({ kind: 'anonymous' }); return of(this.state());
      }
      return this.afterRotation();
    });
  }

  afterPasswordReset(): void {
    this.pending = undefined; this.events.changed();
    this.csrf.clear(); this.store.replace({ kind: 'anonymous' });
  }

  private afterRotation(): Observable<SessionState> {
    this.pending = undefined;
    this.store.replace({ kind: 'initializing' });
    const generation = this.store.generation;
    this.csrf.clear();
    return this.csrf.ensure().pipe(
      switchMap(() => { this.assertGeneration(generation); return this.resolve(true); }),
      catchError((error: unknown) => {
        if (generation === this.store.generation) this.store.replace({ kind: 'unavailable' });
        return throwError(() => error);
      }),
    );
  }

  private assertGeneration(generation: number): void {
    if (generation !== this.store.generation) throw new IdentityProtocolError();
  }

  private exclusive<T>(work: () => Observable<T>): Observable<T> {
    return defer(() => {
      if (this.working()) return throwError(() => new IdentityProtocolError());
      this.working.set(true);
      return defer(work).pipe(finalize(() => this.working.set(false)));
    });
  }
}
