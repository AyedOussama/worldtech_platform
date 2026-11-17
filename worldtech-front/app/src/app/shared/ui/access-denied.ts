import { ChangeDetectionStrategy, Component } from '@angular/core';
import { StatusPage } from './status-page';

@Component({
  selector: 'wt-access-denied',
  imports: [StatusPage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<wt-status-page title="Accès refusé" message="Votre accès à cette page n’est pas autorisé. Contactez votre administrateur si nécessaire." />',
})
export class AccessDenied {}
