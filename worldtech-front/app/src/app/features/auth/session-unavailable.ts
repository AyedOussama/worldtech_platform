import { ChangeDetectionStrategy, Component } from '@angular/core';
import { StatusPage } from '../../shared/ui/status-page';
import { inject } from '@angular/core';
import { SessionService } from '../../core/auth/session-service';
import { AuthFormState } from './auth-form-state';
import { AuthMessage } from './auth-message';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { sessionDestination } from '../../core/auth/session-navigation';

@Component({
  selector: 'wt-session-unavailable',
  imports: [StatusPage, AuthMessage, MatButtonModule],
  providers: [AuthFormState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<wt-status-page title="Session indisponible" message="Votre session ne peut pas être vérifiée. Aucun espace privé n’a été ouvert." />
    <section class="auth-frame"><button matButton="outlined" class="auth-submit" [disabled]="ui.busy()" (click)="retry()">Vérifier à nouveau la session</button><wt-auth-message [error]="ui.error()" /></section>`,
})
export class SessionUnavailable {
  protected readonly ui = inject(AuthFormState);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  protected retry(): void { this.ui.run(this.session.resolve(true), (state) => { if (!['unavailable', 'blocked'].includes(state.kind)) void this.router.navigateByUrl(sessionDestination(state)); }); }
}
