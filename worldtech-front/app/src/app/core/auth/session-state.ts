import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { SessionState, WebRole } from './identity-model';

@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly current = signal<SessionState>({ kind: 'initializing' });
  readonly state = this.current.asReadonly();
  private readonly invalidate = new Subject<void>();
  readonly invalidated$ = this.invalidate.asObservable();
  private revision = 0;
  get generation(): number { return this.revision; }

  replace(state: SessionState): void {
    this.revision++;
    this.invalidate.next();
    this.current.set(state);
  }

  expire(): void {
    if (!['anonymous', 'expired', 'signingOut', 'logoutFailed'].includes(this.state().kind)) this.replace({ kind: 'expired' });
  }

  allows(role?: WebRole): boolean {
    const state = this.state();
    return state.kind === 'authenticated' && (!role || state.user.roles.includes(role));
  }
}
