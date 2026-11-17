import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, finalize, map, of, shareReplay, tap } from 'rxjs';
import { RUNTIME_CONFIG } from '../config/runtime-config';
import { CsrfView, IdentityProtocolError, parseCsrf } from './identity-model';

@Injectable({ providedIn: 'root' })
export class CsrfContext {
  // Bypass the interceptors ONLY to acquire CSRF, avoiding recursion and DI cycles.
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;
  private cached: CsrfView | undefined;
  private pending: Observable<CsrfView> | undefined;
  private generation = 0;

  clear(): void { this.generation++; this.cached = undefined; this.pending = undefined; }

  ensure(): Observable<CsrfView> {
    if (this.cached) return of(this.cached);
    if (this.pending) return this.pending;
    const generation = this.generation;
    const request = this.http.get<unknown>(`${this.base}/auth/csrf`, { headers: { Accept: 'application/json' }, timeout: 10_000 }).pipe(
      map(parseCsrf),
      tap((csrf) => {
        if (generation !== this.generation) throw new IdentityProtocolError();
        this.cached = csrf;
      }),
      finalize(() => { if (this.pending === request) this.pending = undefined; }),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    this.pending = request;
    return request;
  }
}
