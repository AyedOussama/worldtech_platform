import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterOutlet } from '@angular/router';
import { LucideBuilding2, LucideCalendarDays, LucideClipboardList, LucideFileText, LucideMonitor, LucideTicket, LucideUsers, LucideWrench } from '@lucide/angular';
import { ApplicationShell, NavigationItem } from '../../layouts/application-shell';

@Component({
  selector: 'wt-preview-shell',
  imports: [ApplicationShell, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<wt-application-shell [space]="space" [items]="items" [preview]="true"><router-outlet /></wt-application-shell>',
})
export class PreviewShell {
  protected readonly space: 'client' | 'admin' = inject(ActivatedRoute).snapshot.data['space'] === 'admin' ? 'admin' : 'client';
  protected readonly items: readonly NavigationItem[] = this.space === 'admin' ? [
    { label: 'Tickets', icon: LucideTicket, path: '/dev/admin/tickets' },
    { label: 'Missions', icon: LucideClipboardList },
    { label: 'Planning', icon: LucideCalendarDays },
    { label: 'Clients', icon: LucideBuilding2 },
    { label: 'Techniciens', icon: LucideUsers },
    { label: 'Parc', icon: LucideMonitor },
    { label: 'Rapports', icon: LucideFileText },
  ] : [
    { label: 'Incidents', icon: LucideTicket, path: '/dev/client/incidents' },
    { label: 'Sites', icon: LucideBuilding2 },
    { label: 'Équipements', icon: LucideMonitor },
    { label: 'Interventions', icon: LucideWrench },
    { label: 'Rapports', icon: LucideFileText },
  ];
}
