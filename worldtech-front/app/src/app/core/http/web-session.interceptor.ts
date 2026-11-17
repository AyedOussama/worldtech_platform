import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, takeUntil, throwError } from 'rxjs';
import { CsrfContext } from '../auth/csrf-context';
import { SessionStore } from '../auth/session-state';
import { RUNTIME_CONFIG } from '../config/runtime-config';

export const IDENTITY_PROBE = new HttpContextToken(() => false);

export const webSessionInterceptor: HttpInterceptorFn = (request, next) => {
  const base = inject(RUNTIME_CONFIG).apiBaseUrl;
  // Relative same-origin API only. No secret on external, protocol-relative, encoded or mobile paths.
  if (!(request.url === base || request.url.startsWith(base + '/')) || /[\\%]/.test(request.url) || /(?:^|\/)\.\.?(?:\/|[?#]|$)/.test(request.url)) return next(request);
  const csrf = inject(CsrfContext);
  const store = inject(SessionStore);
  const identityCommand = request.url.startsWith(`${base}/auth/`);
  const privateRequest = !identityCommand && !request.context.get(IDENTITY_PROBE);
  const generation = store.generation;
  const send = (secured = request) => {
    const response = next(secured).pipe(catchError((error: unknown) => {
      if (privateRequest && error instanceof HttpErrorResponse && error.status === 401) { csrf.clear(); store.expire(); }
      return throwError(() => error);
    }));
    return privateRequest ? response.pipe(takeUntil(store.invalidated$)) : response;
  };
  if (['GET', 'HEAD', 'OPTIONS'].includes(request.method)) return send();
  const mutation = csrf.ensure().pipe(switchMap((token) => {
    if (privateRequest && generation !== store.generation) return throwError(() => new Error('Session changed'));
    return send(request.clone({ setHeaders: { [token.headerName]: token.token } }));
  }));
  return privateRequest ? mutation.pipe(takeUntil(store.invalidated$)) : mutation;
};
