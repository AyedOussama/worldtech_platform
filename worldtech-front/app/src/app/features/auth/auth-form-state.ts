import { DestroyRef, Injectable, Injector, afterNextRender, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize, timer } from 'rxjs';
import { authFeedback } from '../../core/auth/auth-feedback';
import { ErrorSummary, FieldIssue } from '../../shared/ui/error-summary';

@Injectable()
export class AuthFormState {
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly seconds = signal(0);
  readonly issues = signal<readonly FieldIssue[]>([]);
  private readonly destroy = inject(DestroyRef);
  private readonly injector = inject(Injector);

  invalid(issues: readonly FieldIssue[], summary: () => ErrorSummary | undefined): void {
    this.issues.set(issues);
    afterNextRender(() => summary()?.focus(), { injector: this.injector });
  }

  run<T>(source: Observable<T>, success: (value: T) => void): void {
    if (this.busy() || this.seconds() > 0) return;
    this.busy.set(true); this.error.set(''); this.notice.set(''); this.issues.set([]);
    source.pipe(takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: success,
      error: (error: unknown) => {
        const feedback = authFeedback(error);
        this.error.set(feedback.message);
        if (feedback.retryAfter > 0) {
          const deadline = Date.now() + feedback.retryAfter * 1000;
          this.seconds.set(feedback.retryAfter);
          const countdown = timer(1000, 1000).pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
            this.seconds.set(Math.max(0, Math.ceil((deadline - Date.now()) / 1000)));
            if (!this.seconds()) countdown.unsubscribe();
          });
        }
      },
    });
  }
}
