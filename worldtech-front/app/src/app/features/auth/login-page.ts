import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SessionService } from '../../core/auth/session-service';
import { sessionDestination } from '../../core/auth/session-navigation';
import { ThemeService } from '../../core/theme/theme-service';
import { ErrorSummary, FieldIssue } from '../../shared/ui/error-summary';
import { AuthFormState } from './auth-form-state';
import { LoginVisual } from './login-visual';
import { LoginCard } from './login-card';

@Component({
  selector: 'wt-login-page',
  imports: [ReactiveFormsModule, RouterLink, ErrorSummary, LoginVisual, LoginCard],
  providers: [AuthFormState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  protected readonly session = inject(SessionService);
  protected readonly ui = inject(AuthFormState);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly summary = viewChild(ErrorSummary);
  protected readonly visible = signal(false);
  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(254)] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(256)] }),
  });

  constructor() {
    inject(ThemeService); // Apply the existing preference without displaying a login theme control.
    this.session.resolve().pipe(takeUntilDestroyed()).subscribe();
    inject(DestroyRef).onDestroy(() => this.form.controls.password.reset('', { emitEvent: false }));
  }

  protected submit(): void {
    if (this.ui.busy() || this.session.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.ui.invalid(this.fieldIssues(), () => this.summary()); return;
    }
    const value = this.form.getRawValue();
    this.ui.run(this.session.login({ email: value.email.trim(), password: value.password }), (state) => {
      this.form.controls.password.reset('');
      void this.router.navigate([sessionDestination(state, this.route.snapshot.queryParamMap.get('retour'))], { queryParams: this.returnParams() });
    });
  }

  protected continue(): void {
    void this.router.navigateByUrl(sessionDestination(this.session.state(), this.route.snapshot.queryParamMap.get('retour')));
  }
  protected togglePassword(input: HTMLInputElement): void {
    const active = document.activeElement === input;
    const start = input.selectionStart;
    const end = input.selectionEnd;
    this.visible.update((v) => !v);
    if (active) {
      queueMicrotask(() => {
        try { input.setSelectionRange(start, end); } catch { /* ignore */ }
      });
    }
  }
  protected fieldIssues(): readonly FieldIssue[] {
    return [
      ...(this.form.controls.email.touched && this.form.controls.email.invalid ? [{ targetId: 'login-email', message: 'Renseignez une adresse email valide' }] : []),
      ...(this.form.controls.password.touched && this.form.controls.password.invalid ? [{ targetId: 'login-password', message: 'Renseignez votre mot de passe' }] : []),
    ];
  }
  protected hasFeedback(): boolean {
    return this.fieldIssues().length > 0 || !!this.ui.error() || !!this.ui.notice()
      || ['expired', 'blocked'].includes(this.session.state().kind);
  }
  protected skipToContent(event: Event, target: HTMLElement): void {
    event.preventDefault(); target.focus({ preventScroll: true });
  }
  private returnParams() {
    const value = this.route.snapshot.queryParamMap.get('retour');
    return value && /^\/(admin|client)(\/[A-Za-z0-9_-]+)*$/.test(value) ? { retour: value } : {};
  }
}
