import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, takeUntil, timer } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { IdentityApi } from '../../core/auth/identity-api';
import { SessionService } from '../../core/auth/session-service';
import { ActivationView, EnrollmentView } from '../../core/auth/identity-model';
import { sessionDestination } from '../../core/auth/session-navigation';
import { ErrorSummary } from '../../shared/ui/error-summary';
import { AuthFormState } from './auth-form-state';
import { AuthMessage } from './auth-message';

@Component({
  selector: 'wt-mfa-page',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatInputModule, MatFormFieldModule, ErrorSummary, AuthMessage],
  providers: [AuthFormState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mfa-page.html',
})
export class MfaPage {
  protected readonly session = inject(SessionService);
  protected readonly ui = inject(AuthFormState);
  protected readonly manage = inject(ActivatedRoute).snapshot.data['manage'] === true;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(IdentityApi);
  private readonly destroy = inject(DestroyRef);
  private readonly summary = viewChild(ErrorSummary);
  private enrollmentExpiry: Subscription | undefined;
  protected readonly mode = signal<'challenge' | 'recovery' | 'enroll'>('challenge');
  protected readonly allowed = computed(() => {
    const state = this.session.state();
    return state.kind === 'mfaRequired' || state.kind === 'recoveryCodes' || (state.kind === 'authenticated' && state.user.roles.includes('ADMIN'));
  });
  protected readonly enrollment = signal<EnrollmentView | null>(null);
  protected readonly activation = signal<ActivationView | null>(null);
  protected readonly showSecret = signal(false);
  protected readonly acknowledged = signal(false);
  protected readonly code = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^[0-9]{6}$/)] });
  protected readonly password = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(256)] });
  protected readonly codeForm = new FormGroup({ code: this.code });
  protected readonly passwordForm = new FormGroup({ password: this.password });

  constructor() {
    this.session.resolve().pipe(takeUntilDestroyed()).subscribe();
    effect(() => {
      if (!this.allowed() && this.session.state().kind !== 'initializing') {
        this.clearEnrollment(); this.activation.set(null); this.acknowledged.set(false);
        this.password.reset(); this.code.reset(); this.ui.issues.set([]);
      }
    });
    this.destroy.onDestroy(() => { this.code.reset(); this.password.reset(); this.clearEnrollment(); this.activation.set(null); });
  }

  protected choose(mode: 'challenge' | 'recovery'): void {
    this.clearEnrollment();
    this.mode.set(mode); this.code.reset(); this.ui.error.set(''); this.ui.issues.set([]);
    this.code.setValidators([Validators.required, Validators.pattern(mode === 'recovery' ? /^[A-Za-z0-9_-]{43}$/ : /^[0-9]{6}$/)]);
    this.code.updateValueAndValidity();
  }
  protected start(): void {
    this.ui.run(this.api.startEnrollment(this.manage).pipe(takeUntil(this.session.store.invalidated$)), (enrollment) => {
      this.clearEnrollment();
      this.mode.set('enroll'); this.enrollment.set(enrollment); this.showSecret.set(false);
      this.code.reset(); this.code.setValidators([Validators.required, Validators.pattern(/^[0-9]{6}$/)]); this.code.updateValueAndValidity();
      this.enrollmentExpiry = timer(300_000).pipe(takeUntilDestroyed(this.destroy)).subscribe(() => this.clearEnrollment());
    });
  }
  protected submit(): void {
    this.code.markAsTouched();
    if (this.code.invalid) {
      this.ui.invalid([{ targetId: 'mfa-code', message: this.mode() === 'recovery' ? 'Renseignez un code de récupération complet' : 'Renseignez les six chiffres du code' }], () => this.summary()); return;
    }
    const code = this.code.value;
    const enrollment = this.enrollment();
    if (this.mode() === 'enroll' && enrollment) {
      this.ui.run(this.session.enroll(enrollment.enrollmentId, code), (activation) => {
        this.clearEnrollment(); this.code.reset(); this.acknowledged.set(false); this.activation.set(activation);
      });
    } else if (this.mode() !== 'enroll') {
      this.ui.run(this.session.prove(code, this.mode() === 'recovery'), (state) => {
        this.code.reset();
        if (this.manage) this.ui.notice.set('Votre double authentification a été vérifiée.');
        else this.navigate(state);
      });
    }
  }
  protected finish(): void {
    const activation = this.activation();
    if (!activation || !this.acknowledged() || this.ui.busy() || this.ui.seconds() > 0) return;
    this.activation.set(null);
    this.ui.run(this.session.finishRecoveryCodes(activation.status === 'REAUTHENTICATION_REQUIRED'), (state) => this.navigate(state));
  }
  protected reauthenticate(): void {
    this.password.markAsTouched();
    if (this.password.invalid) {
      this.ui.invalid([{ targetId: 'mfa-password', message: 'Renseignez votre mot de passe actuel' }], () => this.summary()); return;
    }
    this.ui.run(this.session.reauthenticate(this.password.value), () => { this.password.reset(); this.ui.notice.set('Votre mot de passe a été vérifié. Les opérations sensibles restent contrôlées par le serveur.'); });
  }
  protected replaceCodes(): void {
    this.ui.run(this.api.replaceCodes().pipe(takeUntil(this.session.store.invalidated$)), (activation) => { this.acknowledged.set(false); this.activation.set(activation); });
  }
  protected logout(): void { this.ui.run(this.session.logout(), () => void this.router.navigateByUrl('/connexion', { replaceUrl: true })); }
  private clearEnrollment(): void {
    this.enrollmentExpiry?.unsubscribe(); this.enrollmentExpiry = undefined;
    this.enrollment.set(null); this.showSecret.set(false);
  }
  private navigate(state: ReturnType<SessionService['state']>): void {
    void this.router.navigateByUrl(sessionDestination(state, this.route.snapshot.queryParamMap.get('retour')), { replaceUrl: true });
  }
}
