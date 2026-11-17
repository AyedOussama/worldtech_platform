import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'wt-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<section class="empty-state"><h2>{{ title() }}</h2><p>{{ message() }}</p><ng-content /></section>',
  styles: `
    .empty-state { padding: 48px 24px; text-align: center; }
    h2 { font-size: 1.125rem; font-weight: 500; margin: 0 0 8px; }
    p { color: var(--wt-secondary); max-width: 48ch; margin: 0 auto 16px; }
  `,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
}
