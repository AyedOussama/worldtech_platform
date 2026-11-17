import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'wt-reference-info-dialog',
  imports: [MatDialogModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>À propos de cet aperçu</h2>
    <mat-dialog-content tabindex="0" role="region" aria-label="Informations sur l’aperçu">
      <p>Ces données sont fictives et déterministes. Aucune connexion ni requête métier n’est effectuée.</p>
      <p>La liste globale, la recherche, les références et les dates d’actualisation sont des propositions UX absentes de l’API 0.6.0. Leur intégration sera rapprochée du contrat en FE06.</p>
      <p>Les menus « À venir » ne donnent aucun droit. Les routes privées Client et Administrateur restent fermées jusqu’à l’intégration de l’identité réelle.</p>
    </mat-dialog-content>
    <mat-dialog-actions><button matButton="outlined" mat-dialog-close>Fermer</button></mat-dialog-actions>
  `,
})
export class ReferenceInfoDialog {}
