import { Routes } from '@angular/router';
import { PrivateShell } from '../../layouts/private-shell';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: PrivateShell,
    children: [
      { path: '', title: 'Administration — WorldTech', loadComponent: () => import('./admin-entry').then((m) => m.AdminEntry) },
      { path: 'missions', title: 'Missions — WorldTech', loadComponent: () => import('./missions/admin-missions-page').then((m) => m.AdminMissionsPage) },
      { path: 'techniciens', title: 'Techniciens — WorldTech', loadComponent: () => import('./technicians/admin-technicians-page').then((m) => m.AdminTechniciansPage) },
      { path: 'clients', title: 'Clients — WorldTech', loadComponent: () => import('./clients/admin-clients-page').then((m) => m.AdminClientsPage) },
    ],
  },
];
