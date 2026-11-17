import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { SessionStore } from './core/auth/session-state';
import { SessionEvents } from './core/auth/session-events';
import { sessionDestination } from './core/auth/session-navigation';

@Component({
  imports: [RouterOutlet],
  selector: 'wt-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<router-outlet />',
})
export class App {
  constructor() {
    const store = inject(SessionStore);
    const router = inject(Router);
    inject(SessionEvents);
    effect(() => {
      const state = store.state();
      if (/^\/(admin|client)(\/|\?|$)/.test(router.url) && !['authenticated', 'initializing', 'signingOut'].includes(state.kind)) {
        void router.navigateByUrl(sessionDestination(state), { replaceUrl: true });
      }
    });
  }
}
