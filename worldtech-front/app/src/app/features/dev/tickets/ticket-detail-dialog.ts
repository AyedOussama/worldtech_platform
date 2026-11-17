import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { StatusLabel } from '../../../shared/ui/status-label';
import { MoneyDisplay } from '../../../shared/ui/money-display';
import { Timeline, TimelineEvent } from '../../../shared/ui/timeline';
import { REFERENCE_STATES, REFERENCE_TIMELINE, ReferenceTicket } from './ticket-fixtures';

@Component({
  selector: 'wt-ticket-detail-dialog',
  imports: [MatDialogModule, MatButtonModule, StatusLabel, MoneyDisplay, Timeline],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ ticket.reference }} · Aperçu fictif</h2>
    <mat-dialog-content tabindex="0" role="region" aria-label="Contenu du ticket fictif">
      <p class="ticket-title">{{ ticket.title }}</p>
      <p class="dialog-note">{{ ticket.site }} · Europe/Paris</p>
      <wt-status-label [label]="state.label" [tone]="state.tone" />
      <section class="next-step dialog-section" aria-label="Prochaine action">
        <span>Prochaine action · {{ ticket.owner }}</span><p>{{ ticket.nextAction }}</p>
      </section>
      <section class="dialog-section" aria-label="Proposition client fictive">
        <h3>Proposition du client</h3>
        <dl class="amounts">
          <div><dt>Rémunération fixe · version 1</dt><dd><wt-money-display value="120.50" /> HT</dd></div>
          <div><dt>Frais proposés · version 1</dt><dd><wt-money-display value="0.00" /> HT</dd></div>
        </dl>
        <p class="dialog-note">Exemple indépendant du ticket. Aucun montant publié par l’administration ni accepté par un technicien ; aucun calcul de marge ou paiement.</p>
      </section>
      <section class="dialog-section" aria-label="Chronologie illustrative">
        <h3>Chronologie illustrative</h3>
        <wt-timeline [events]="events" />
      </section>
      <p class="dialog-note">Les références, dates d’actualisation et prochaines actions sont des propositions UX, pas des données de l’API actuelle.</p>
    </mat-dialog-content>
    <mat-dialog-actions><button matButton="outlined" mat-dialog-close>Fermer l’aperçu</button></mat-dialog-actions>
  `,
  styles: `
    .ticket-title { font-size: 1.125rem; font-weight: 500; color: var(--wt-text); margin-top: 0; }
    .next-step { border-left: 2px solid var(--wt-accent); padding-left: 16px; }
    .next-step span { font-size: .8125rem; color: var(--wt-secondary); }
    .next-step p { margin: 4px 0 0; color: var(--wt-text); }
    .amounts { margin: 0; display: grid; gap: 12px; font-size: .875rem; }
    .amounts > div { display: flex; flex-wrap: wrap; gap: 4px 16px; justify-content: space-between; }
    dt { color: var(--wt-secondary); } dd { margin: 0; color: var(--wt-text); }
  `,
})
export class TicketDetailDialog {
  protected readonly ticket = inject<ReferenceTicket>(MAT_DIALOG_DATA);
  protected readonly state = REFERENCE_STATES[this.ticket.state];
  protected readonly events: readonly TimelineEvent[] = this.ticket.reference === 'DEMO-2048' ? REFERENCE_TIMELINE : [
    { id: this.ticket.reference, at: this.ticket.updatedAt, label: `État illustré : ${this.state.label}`, actor: this.ticket.owner },
  ];
}
