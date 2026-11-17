import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { SessionService } from './session-service';
import { SessionStore } from './session-state';
import { safeReturnUrl } from './safe-return-url';
import { sessionDestination } from './session-navigation';
import { map } from 'rxjs';

export const requireSession: CanMatchFn = (_route, segments) => {
  const session = inject(SessionService);
  const store = inject(SessionStore);
  const router = inject(Router);
  const returnTo = safeReturnUrl('/' + segments.map((segment) => segment.path).join('/'));
  const role = segments[0]?.path === 'admin' ? 'ADMIN' : 'CLIENT';
  return session.resolve().pipe(map((state) => store.allows(role) ? true : router.createUrlTree(
    [state.kind === 'authenticated' ? '/acces-refuse' : sessionDestination(state)],
    { queryParams: returnTo ? { retour: returnTo } : {} },
  )));
};
