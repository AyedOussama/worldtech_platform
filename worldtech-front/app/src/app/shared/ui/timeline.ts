import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export interface TimelineEvent {
  readonly id: string;
  readonly at: string;
  readonly label: string;
  readonly actor: string;
}

export function formatParisDate(value: string): string {
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Paris' }).format(new Date(value));
}

@Component({
  selector: 'wt-timeline',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ol class="timeline">@for (event of events(); track event.id) {
      <li><span class="timeline-point" aria-hidden="true"></span><div>
        <p>{{ event.label }}</p><span>{{ event.actor }} · <time [attr.datetime]="event.at">{{ formatDate(event.at) }}</time></span>
      </div></li>
    }</ol>
  `,
  styles: `
    .timeline { list-style: none; padding: 0; margin: 0; }
    li { display: flex; align-items: start; gap: 16px; padding: 0 0 24px; position: relative; }
    li:not(:last-child)::before { content: ''; position: absolute; width: 1px; top: 12px; bottom: 0; left: 4px; background: var(--wt-border); }
    li:last-child { padding-bottom: 0; }
    .timeline-point { margin-top: 8px; width: 9px; height: 9px; border-radius: 50%; background: var(--wt-control-border); z-index: 1; flex-shrink: 0; }
    li:first-child .timeline-point { background: var(--wt-accent); }
    p { margin: 0 0 4px; font-size: .9375rem; color: var(--wt-text); }
    span:not(.timeline-point) { font-size: .8125rem; color: var(--wt-secondary); }
  `,
})
export class Timeline {
  readonly events = input.required<readonly TimelineEvent[]>();
  protected readonly formatDate = formatParisDate;
}
