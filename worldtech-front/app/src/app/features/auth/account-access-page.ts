import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SessionService } from '../../core/auth/session-service';
import { sessionDestination } from '../../core/auth/session-navigation';
import { AuthFormState } from './auth-form-state';
import { AuthMessage } from './auth-message';

@Component({
  selector: 'wt-account-access-page',
  imports: [RouterLink, MatButtonModule, AuthMessage],
  providers: [AuthFormState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="auth-frame"><div class="auth-card"><header class="auth-heading"><h1>Votre accès WorldTech</h1></header>
      @switch (session.state().kind) {
        @case ('onboarding') { <p>Votre compte est en attente de validation. L’accès aux opérations client n’est pas encore ouvert.</p><p class="auth-caption">Le suivi du dossier d’admission sera disponible dans une prochaine étape.</p> }
        @case ('suspended') { <p>Votre compte est suspendu. Contactez votre administrateur ; aucun accès métier n’est ouvert.</p> }
        @case ('rejected') { <p>Votre compte ne permet pas l’accès à cet espace. Contactez votre administrateur.</p> }
        @case ('technician') { <p>L’espace technicien est disponible dans l’application Flutter, pas dans ce portail web.</p> }
        @case ('logoutFailed') { <p class="auth-error" role="alert">La déconnexion n’a pas été confirmée. Votre session serveur peut encore être active. Réessayez la déconnexion.</p> }
        @default { <p>Votre accès doit être vérifié avant d’ouvrir un espace privé.</p> }
      }
      <wt-auth-message [error]="ui.error()" />
      <div class="auth-actions"><button matButton="outlined" [disabled]="ui.busy()" (click)="logout()">{{ session.state().kind === 'logoutFailed' ? 'Réessayer la déconnexion' : 'Se déconnecter' }}</button><a matButton routerLink="/connexion">Connexion</a></div>
    </div></section>
  `,
})
export class AccountAccessPage {
  protected readonly session = inject(SessionService);
  protected readonly ui = inject(AuthFormState);
  private readonly router = inject(Router);
  constructor() { this.session.resolve().pipe(takeUntilDestroyed()).subscribe((state) => { if (state.kind === 'authenticated') void this.router.navigateByUrl(sessionDestination(state)); }); }
  protected logout(): void { this.ui.run(this.session.logout(), () => void this.router.navigateByUrl('/connexion', { replaceUrl: true })); }
}
