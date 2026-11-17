import { Routes } from '@angular/router';
import { PrivateShell } from '../../layouts/private-shell';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: PrivateShell,
    children: [{ path: '', title: 'Administration — WorldTech', loadComponent: () => import('./admin-entry').then((m) => m.AdminEntry) }],
  },
];
