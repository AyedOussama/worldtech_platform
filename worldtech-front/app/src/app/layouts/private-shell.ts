import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { LucideShieldCheck, LucideUserRound } from '@lucide/angular';
import { ApplicationShell, NavigationItem } from './application-shell';
import { SessionService } from '../core/auth/session-service';
import { AuthFormState } from '../features/auth/auth-form-state';

@Component({
  selector: 'wt-private-shell',
  imports: [ApplicationShell, RouterOutlet, MatButtonModule],
  providers: [AuthFormState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (session.store.allows(space === 'admin' ? 'ADMIN' : 'CLIENT')) {
      <wt-application-shell [space]="space" [items]="items">
        <button shell-actions matButton type="button" [disabled]="ui.busy()" (click)="logout()">Se déconnecter</button>
        <router-outlet />
      </wt-application-shell>
    } @else {
      <main class="startup-failure" role="status">Vérification de votre session…</main>
    }
  `,
})
export class PrivateShell {
  protected readonly session = inject(SessionService);
  protected readonly ui = inject(AuthFormState);
  private readonly router = inject(Router);
  protected readonly space: 'admin' | 'client' = inject(ActivatedRoute).snapshot.pathFromRoot.some((route) => route.routeConfig?.path === 'admin') ? 'admin' : 'client';
  protected readonly items: readonly NavigationItem[] = [{ label: 'Mon accès', icon: LucideUserRound, path: '/' + this.space },
    ...(this.space === 'admin' ? [{ label: 'Sécurité', icon: LucideShieldCheck, path: '/auth/securite' }] : []),
  ];
  protected logout(): void {
    this.ui.run(this.session.logout(), () => void this.router.navigateByUrl('/connexion', { replaceUrl: true }));
  }
}
