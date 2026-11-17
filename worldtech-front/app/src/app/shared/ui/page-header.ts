import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'wt-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-heading">
      <div><h1>{{ title() }}</h1><p>{{ description() }}</p></div>
      <div class="page-actions"><ng-content /></div>
    </header>
  `,
  styles: `
    .page-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px 24px; flex-wrap: wrap; margin-bottom: 24px; }
    h1 { margin: 0 0 4px; font-size: 1.75rem; font-weight: 500; line-height: 1.3; }
    p { margin: 0; color: var(--wt-secondary); font-size: .9375rem; }
    .page-actions { display: flex; align-items: center; gap: 8px; }
    @media (max-width: 600px) { h1 { font-size: 1.5rem; } }
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly description = input('');
}
