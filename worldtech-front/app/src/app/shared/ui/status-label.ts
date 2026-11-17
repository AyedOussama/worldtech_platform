import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type StatusTone = 'neutral' | 'info' | 'warning' | 'success' | 'error';

@Component({
  selector: 'wt-status-label',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<span class="status-label" [attr.data-tone]="tone()"><span class="status-dot" aria-hidden="true"></span>{{ label() }}</span>',
  styles: `
    :host { display: inline-block; }
    .status-label { display: inline-flex; gap: 8px; align-items: center; padding: 4px 8px; border-radius: 4px; background: var(--wt-selected); color: var(--wt-secondary); font-size: .8125rem; font-weight: 500; line-height: 1.5; }
    .status-dot { height: 6px; width: 6px; border-radius: 50%; background: currentColor; flex-shrink: 0; }
    [data-tone=info] { color: var(--wt-accent); background: var(--wt-info-surface); }
    [data-tone=warning] { color: var(--wt-warning); background: var(--wt-warning-surface); }
    [data-tone=success] { color: var(--wt-success); background: var(--wt-success-surface); }
    [data-tone=error] { color: var(--wt-error); background: var(--wt-error-surface); }
  `,
})
export class StatusLabel {
  readonly label = input.required<string>();
  readonly tone = input<StatusTone>('neutral');
}
