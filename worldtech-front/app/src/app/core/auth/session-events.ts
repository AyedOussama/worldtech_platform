import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject } from '@angular/core';
import { CsrfContext } from './csrf-context';
import { SessionStore } from './session-state';

@Injectable({ providedIn: 'root' })
export class SessionEvents {
  private channel: BroadcastChannel | undefined;
  constructor() {
    const store = inject(SessionStore);
    const csrf = inject(CsrfContext);
    const window = inject(DOCUMENT).defaultView;
    try {
      if (window && 'BroadcastChannel' in window) {
        this.channel = new BroadcastChannel('wt-session-change');
        this.channel.onmessage = (event: MessageEvent<unknown>) => {
          if (event.data === 'changed') { csrf.clear(); store.replace({ kind: 'blocked' }); }
        };
      }
    } catch { /* Optional coordination; server authorization remains mandatory. */ }
    inject(DestroyRef).onDestroy(() => this.channel?.close());
  }
  changed(): void { this.channel?.postMessage('changed'); }
}
