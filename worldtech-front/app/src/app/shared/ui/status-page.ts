import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'wt-status-page',
  imports: [RouterLink, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="status-page" aria-labelledby="page-title">
      <h1 id="page-title">{{ title() }}</h1>
      <p>{{ message() }}</p>
      <a matButton="filled" routerLink="/connexion">Retour à la connexion</a>
    </section>
  `,
})
export class StatusPage {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
}
