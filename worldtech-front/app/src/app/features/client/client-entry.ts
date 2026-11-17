import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../shared/ui/page-header';

@Component({
  selector: 'wt-client-entry',
  imports: [PageHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <wt-page-header title="Espace client" description="Session client vérifiée par le serveur" />
    <p class="text-secondary">Les sites et incidents seront disponibles après leur intégration. Aucune donnée fictive n’est affichée dans cet espace.</p>
  `,
})
export class ClientEntry {}
