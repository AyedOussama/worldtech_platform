import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'wt-auth-message',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error()) { <p class="auth-error" role="alert">{{ error() }}</p> }
    @if (notice()) { <p class="auth-notice" role="status">{{ notice() }}</p> }
    @if (seconds() > 0) { <p class="auth-wait">Nouvel essai dans {{ seconds() }} s</p> }
  `,
})
export class AuthMessage {
  readonly error = input('');
  readonly notice = input('');
  readonly seconds = input(0);
}
