import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { PageHeader } from '../../shared/ui/page-header';

@Component({
  selector: 'wt-admin-entry',
  imports: [PageHeader, RouterLink, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <wt-page-header title="Administration" description="Session et double authentification vérifiées par le serveur">
      <a matButton="filled" routerLink="/auth/securite">Gérer la double authentification</a>
    </wt-page-header>
    <p class="text-secondary">Les fonctions métier seront disponibles après leur intégration. Aucune donnée fictive n’est affichée dans cet espace.</p>
  `,
})
export class AdminEntry {}
