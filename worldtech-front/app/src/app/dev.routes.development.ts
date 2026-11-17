import { Routes } from '@angular/router';
import { PublicShell } from './layouts/public-shell';

export const DEVELOPMENT_ROUTES: Routes = [
  {
    path: 'dev',
    component: PublicShell,
    children: [
      {
        path: '', pathMatch: 'full', title: 'Composants de développement — WorldTech',
        loadComponent: () => import('./features/dev/component-example').then((m) => m.ComponentExample),
      },
    ],
  },
  ...(['admin', 'client'] as const).map((space) => ({
    path: `dev/${space}`,
    data: { space },
    loadComponent: () => import('./features/dev/preview-shell').then((m) => m.PreviewShell),
    children: [
      { path: '', pathMatch: 'full' as const, redirectTo: space === 'admin' ? 'tickets' : 'incidents' },
      {
        path: space === 'admin' ? 'tickets' : 'incidents',
        data: { space },
        title: `${space === 'admin' ? 'Tickets' : 'Incidents'} fictifs — WorldTech`,
        loadComponent: () => import('./features/dev/tickets/ticket-reference-page').then((m) => m.TicketReferencePage),
      },
    ],
  })),
];
