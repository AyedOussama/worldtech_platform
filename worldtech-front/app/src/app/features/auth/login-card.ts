import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Page-specific brand frame; projected form keeps its existing auth owner. */
@Component({
  selector: 'wt-login-card',
  imports: [RouterLink],
  host: { class: 'login-card', '[class.has-feedback]': 'feedback()' },
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login-card.html',
  styleUrl: './login-card.scss',
})
export class LoginCard {
  readonly authenticated = input(false);
  readonly feedback = input(false);
}
