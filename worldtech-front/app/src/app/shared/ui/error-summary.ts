import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, inject, input, viewChild } from '@angular/core';

export interface FieldIssue { readonly targetId: string; readonly message: string; }

@Component({
  selector: 'wt-error-summary',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section #summary class="error-summary" [class.compact]="compact()" tabindex="-1" aria-label="Champs à corriger">
      <h3 [class.visually-hidden]="compact()">Vérifiez les champs suivants</h3>
      <ul>@for (issue of issues(); track issue.targetId) {
        <li><a [href]="'#' + issue.targetId" (click)="focusField($event, issue.targetId)">{{ issue.message }}</a></li>
      }</ul>
    </section>
  `,
  styles: `
    .error-summary { padding: 16px; background: var(--wt-error-surface); border: 1px solid var(--wt-error); border-radius: 8px; margin-bottom: 24px; color: var(--wt-error); outline-offset: 4px; }
    .error-summary:focus { outline: 3px solid var(--wt-accent); }
    h3 { font-size: 1rem; font-weight: 500; margin: 0 0 8px; }
    ul { margin: 0; padding-left: 24px; } a { color: inherit; display: inline-block; padding-block: 4px; }
    .compact { margin: 0; padding: 8px; font-size: .8125rem; line-height: 1.4; }
    .compact ul { padding-left: 16px; } .compact a { padding-block: 2px; }
  `,
})
export class ErrorSummary {
  readonly issues = input.required<readonly FieldIssue[]>();
  readonly compact = input(false);
  private readonly document = inject(DOCUMENT);
  private readonly summary = viewChild.required<ElementRef<HTMLElement>>('summary');
  focus(): void { this.summary().nativeElement.focus(); }
  protected focusField(event: Event, id: string): void {
    event.preventDefault();
    this.document.getElementById(id)?.focus();
  }
}
