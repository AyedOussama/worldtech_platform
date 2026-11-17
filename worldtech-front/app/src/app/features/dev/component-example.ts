import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'wt-component-example',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="status-page" data-development-only="WT_DEV_CATALOGUE_ONLY" aria-labelledby="page-title">
      <h1 id="page-title">Composants de développement</h1>
      <p>Exemple local Angular Material. Aucune donnée métier ni requête API.</p>
      <nav class="catalogue-links" aria-label="Aperçus FE02">
        <a matButton="outlined" routerLink="/dev/admin/tickets">Aperçu administration</a>
        <a matButton="outlined" routerLink="/dev/client/incidents">Aperçu client</a>
      </nav>
      <form class="component-example" (submit)="validate($event)">
        <mat-form-field appearance="outline">
          <mat-label>Libellé</mat-label>
          <input matInput [formControl]="label" required maxlength="80" autocomplete="off" />
          <mat-error>Renseignez un libellé</mat-error>
        </mat-form-field>
        <button matButton="filled" type="submit">Vérifier la saisie</button>
      </form>
      <p role="status">{{ result() }}</p>
    </section>
  `,
})
export class ComponentExample {
  readonly label = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly result = signal('');

  validate(event: Event): void {
    event.preventDefault();
    this.label.markAsTouched();
    this.result.set(this.label.valid ? 'La saisie est valide. Aucun envoi réseau.' : 'La saisie est incomplète.');
  }
}
