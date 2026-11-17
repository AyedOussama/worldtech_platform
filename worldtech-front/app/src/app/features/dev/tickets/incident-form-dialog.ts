import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { timer } from 'rxjs';
import { IncidentForm } from '../../../shared/tickets/incident-form';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/ui/confirm-dialog';
import { REFERENCE_SITES } from './ticket-fixtures';

type SimulatedOutcome = 'local' | 'network' | 'forbidden' | 'stale';

@Component({
  selector: 'wt-incident-form-dialog',
  imports: [MatDialogModule, MatButtonModule, IncidentForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Déclarer un incident fictif</h2>
    <mat-dialog-content>
      <p class="dialog-note">Formulaire de référence · Aucune création ni pièce jointe envoyée</p>
      <wt-incident-form #editor formId="reference-incident" [sites]="sites" [busy]="busy()" (validated)="simulate()" (edited)="feedback.set('')" />
      <div class="simulation-options">
        <label for="form-outcome">Résultat simulé</label>
        <select id="form-outcome" #outcome [disabled]="busy()" [value]="scenario()" (change)="setScenario(outcome.value)">
          <option value="local">Vérification locale uniquement</option>
          <option value="network">Réseau indisponible</option>
          <option value="forbidden">Accès refusé</option>
          <option value="stale">Version périmée</option>
        </select>
      </div>
      @if (feedback()) {
        <div class="form-feedback" [class.failure]="failed()" role="status">{{ feedback() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions>
      <button matButton type="button" (click)="requestClose()">Annuler</button>
      <button matButton="filled" type="submit" form="reference-incident" [disabled]="busy()">
        {{ busy() ? 'Vérification…' : 'Tester la saisie' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .simulation-options { display: grid; gap: 8px; margin-top: 32px; font-size: .875rem; color: var(--wt-secondary); }
    select { min-width: 0; width: 100%; min-height: 44px; padding: 8px 12px; background: var(--wt-surface); color: var(--wt-text); border: 1px solid var(--wt-control-border); border-radius: 8px; }
    .form-feedback { margin-top: 16px; padding: 16px; border-left: 3px solid var(--wt-accent); color: var(--wt-text); background: var(--wt-info-surface); }
    .failure { border-color: var(--wt-error); background: var(--wt-error-surface); color: var(--wt-error); }
  `,
})
export class IncidentFormDialog {
  protected readonly sites = REFERENCE_SITES.slice(0, 2);
  protected readonly busy = signal(false);
  protected readonly feedback = signal('');
  protected readonly failed = signal(false);
  protected readonly scenario = signal<SimulatedOutcome>('local');
  private readonly editor = viewChild.required(IncidentForm);
  private readonly destroy = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly ref = inject(MatDialogRef<IncidentFormDialog>);
  private closing = false;

  constructor() {
    this.ref.backdropClick().pipe(takeUntilDestroyed()).subscribe(() => this.requestClose());
    this.ref.keydownEvents().pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event.key === 'Escape') { event.preventDefault(); this.requestClose(); }
    });
  }

  protected setScenario(value: string): void {
    if (['local', 'network', 'forbidden', 'stale'].includes(value)) {
      this.scenario.set(value as SimulatedOutcome); this.feedback.set('');
    }
  }

  protected simulate(): void {
    if (this.busy()) return;
    this.busy.set(true); this.feedback.set('');
    const outcome = this.scenario();
    // Explicit local fixture delay, not an HTTP request or a fallback on API failure.
    timer(350).pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      this.busy.set(false);
      this.failed.set(outcome !== 'local');
      this.feedback.set({
        local: 'Saisie vérifiée localement. Aucun incident créé, rien n’a été envoyé.',
        network: 'Simulation : réseau indisponible. Votre saisie est conservée, aucun envoi effectué.',
        forbidden: 'Simulation : accès refusé. Votre saisie est conservée ; aucun droit n’a été accordé.',
        stale: 'Simulation : version périmée. Votre saisie est conservée ; relire la version serveur avant toute nouvelle commande réelle.',
      }[outcome]);
    });
  }

  protected requestClose(): void {
    if (this.closing || this.busy()) return;
    if (!this.editor().form.dirty) { this.ref.close(); return; }
    this.closing = true;
    this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
      data: { title: 'Abandonner la saisie fictive ?', message: 'Cette saisie n’a pas été envoyée. Elle sera perdue si vous fermez le formulaire.', confirmLabel: 'Abandonner la saisie' },
      width: '480px', maxWidth: 'calc(100vw - 32px)', autoFocus: 'first-tabbable',
    }).afterClosed().pipe(takeUntilDestroyed(this.destroy)).subscribe((confirmed) => {
      this.closing = false;
      if (confirmed) this.ref.close();
    });
  }
}
