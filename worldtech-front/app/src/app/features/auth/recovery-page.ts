import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { IdentityApi } from '../../core/auth/identity-api';
import { ErrorSummary } from '../../shared/ui/error-summary';
import { AuthFormState } from './auth-form-state';
import { AuthMessage } from './auth-message';

@Component({
  selector: 'wt-recovery-page',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatInputModule, MatFormFieldModule, ErrorSummary, AuthMessage],
  providers: [AuthFormState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="auth-frame"><div class="auth-card">
      <header class="auth-heading"><h1>{{ verification ? 'Confirmer votre adresse email' : 'Mot de passe oublié' }}</h1>
        <p>{{ verification ? 'Demandez un nouveau lien de confirmation' : 'Demandez un lien pour choisir un nouveau mot de passe' }}</p></header>
      @if (!sent()) {
        <form [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="ui.busy()">
          @if (ui.issues().length) { <wt-error-summary [issues]="ui.issues()" /> }
          <div class="auth-fields"><mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Adresse email</mat-label>
            <input matInput id="recovery-email" type="email" [formControl]="email" autocomplete="email" maxlength="254" required [readonly]="ui.busy()" />
            <mat-error>Renseignez une adresse email valide</mat-error>
          </mat-form-field></div>
          <wt-auth-message [error]="ui.error()" [seconds]="ui.seconds()" />
          <button matButton="filled" class="auth-submit" type="submit" [disabled]="ui.busy() || ui.seconds() > 0">{{ ui.busy() ? 'Demande en cours…' : 'Demander le lien' }}</button>
        </form>
      } @else { <p role="status">Si cette adresse correspond à un compte éligible, un lien lui sera envoyé. Consultez aussi vos courriers indésirables.</p> }
      <nav class="auth-secondary"><a routerLink="/connexion">Retour à la connexion</a></nav>
    </div></section>
  `,
})
export class RecoveryPage {
  protected readonly verification = inject(ActivatedRoute).snapshot.data['verification'] === true;
  protected readonly ui = inject(AuthFormState);
  protected readonly sent = signal(false);
  protected readonly email = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(254)] });
  protected readonly form = new FormGroup({ email: this.email });
  private readonly api = inject(IdentityApi);
  private readonly summary = viewChild(ErrorSummary);
  protected submit(): void {
    this.email.markAsTouched();
    if (this.email.invalid) { this.ui.invalid([{ targetId: 'recovery-email', message: 'Renseignez une adresse email valide' }], () => this.summary()); return; }
    const body = { email: this.email.value.trim() };
    this.ui.run(this.verification ? this.api.requestVerification(body) : this.api.requestReset(body), () => this.sent.set(true));
  }
}
