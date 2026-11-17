import { Routes } from '@angular/router';
import { PrivateShell } from '../../layouts/private-shell';

export const CLIENT_ROUTES: Routes = [
  {
    path: '',
    component: PrivateShell,
    children: [{ path: '', title: 'Espace client — WorldTech', loadComponent: () => import('./client-entry').then((m) => m.ClientEntry) }],
  },
];
