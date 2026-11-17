import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, NavigationEnd, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { IdentityApi } from '../../core/auth/identity-api';
import { captureActionFragment, consumeActionToken } from '../../core/auth/action-fragment';
import { SessionService } from '../../core/auth/session-service';
import { ErrorSummary } from '../../shared/ui/error-summary';
import { AuthFormState } from './auth-form-state';
import { AuthMessage } from './auth-message';
import { RecoveryPage } from './recovery-page';

export function newPassword(control: AbstractControl<string>) {
  const length = Array.from(control.value).length;
  return length >= 15 && length <= 128 && control.value.length <= 256 ? null : { passwordLength: true };
}

@Component({
  selector: 'wt-action-confirmation-page',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatInputModule, MatFormFieldModule, ErrorSummary, AuthMessage, RecoveryPage],
  providers: [AuthFormState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './action-confirmation-page.html',
})
export class ActionConfirmationPage {
  protected readonly reset = inject(ActivatedRoute).snapshot.data['reset'] === true;
  private token: string | null = consumeActionToken(this.reset ? 'reset' : 'verify');
  protected readonly hasToken = signal(!!this.token);
  protected readonly ui = inject(AuthFormState);
  protected readonly done = signal(false);
  protected readonly form = new FormGroup({ password: new FormControl('', { nonNullable: true, validators: [newPassword] }), confirmation: new FormControl('', { nonNullable: true, validators: [Validators.required] }) });
  private readonly api = inject(IdentityApi);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly summary = viewChild(ErrorSummary);
  constructor() {
    const browser = inject(DOCUMENT).defaultView;
    const path = this.reset ? '/auth/password-reset' : '/auth/email-verification';
    // A second email link to the same path can be a fragment-only navigation:
    // main.ts does not run again. Scrub it and start a fresh explicit action.
    const receiveLink = () => {
      if (!browser?.location.hash || browser.location.pathname !== path) return;
      captureActionFragment(browser);
      const token = consumeActionToken(this.reset ? 'reset' : 'verify');
      // Synchronize Router too: its pending popstate navigation must not write
      // the original secret fragment back after history.replaceState scrubbed it.
      void this.router.navigateByUrl(path, { replaceUrl: true });
      if (token === this.token && !this.done()) return;
      if (this.ui.busy()) {
        this.ui.notice.set('Une opération est en cours. Rouvrez le nouveau lien après sa fin.'); return;
      }
      this.token = token; this.hasToken.set(!!token); this.done.set(false);
      this.form.reset(); this.ui.issues.set([]); this.ui.error.set(''); this.ui.notice.set('');
    };
    browser?.addEventListener('hashchange', receiveLink);
    // PathLocationStrategy schedules popstate navigation asynchronously. Reconcile
    // its final URL as well, without resetting an already captured action twice.
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event instanceof NavigationEnd && event.urlAfterRedirects.split(/[?#]/)[0] === path && event.urlAfterRedirects.includes('#')) {
        if (browser?.location.hash) receiveLink();
        else void this.router.navigateByUrl(path, { replaceUrl: true });
      }
    });
    receiveLink();
    inject(DestroyRef).onDestroy(() => {
      browser?.removeEventListener('hashchange', receiveLink);
      this.token = null; this.form.reset();
    });
  }
  protected submit(): void {
    if (!this.token) return;
    if (this.reset) {
      this.form.markAllAsTouched();
      if (this.form.invalid || this.form.controls.password.value !== this.form.controls.confirmation.value) {
        this.ui.invalid([
          ...(this.form.controls.password.invalid ? [{ targetId: 'new-password', message: 'Choisissez un mot de passe de 15 à 128 caractères' }] : []),
          ...(this.form.controls.confirmation.invalid || this.form.controls.password.value !== this.form.controls.confirmation.value ? [{ targetId: 'confirm-password', message: 'Les mots de passe doivent être identiques' }] : []),
        ], () => this.summary()); return;
      }
    }
    const command = this.reset ? this.api.reset({ token: this.token, password: this.form.controls.password.value }) : this.api.verify(this.token);
    this.ui.run(command, () => { this.token = null; this.form.reset(); this.done.set(true); if (this.reset) this.session.afterPasswordReset(); });
  }
}
