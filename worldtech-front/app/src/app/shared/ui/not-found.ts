import { ChangeDetectionStrategy, Component } from '@angular/core';
import { StatusPage } from './status-page';

@Component({
  selector: 'wt-not-found',
  imports: [StatusPage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<wt-status-page title="Page introuvable" message="Cette adresse ne correspond à aucune page. Vérifiez le lien ou revenez à la connexion." />',
})
export class NotFound {}
