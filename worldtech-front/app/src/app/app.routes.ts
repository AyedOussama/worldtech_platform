import { Routes } from '@angular/router';
import { requireSession } from './core/auth/require-session';
import { DEVELOPMENT_ROUTES } from './dev.routes';
import { PublicShell } from './layouts/public-shell';

export const routes: Routes = [
  {
    path: 'connexion', title: 'Connexion — WorldTech',
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'client',
    canMatch: [requireSession],
    loadChildren: () => import('./features/client/client.routes').then((m) => m.CLIENT_ROUTES),
  },
  {
    path: 'admin',
    canMatch: [requireSession],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  ...DEVELOPMENT_ROUTES,
  {
    path: '',
    component: PublicShell,
    children: [
      { path: '', pathMatch: 'full', redirectTo: '/connexion' },
      { path: 'auth/mot-de-passe-oublie', title: 'Mot de passe oublié — WorldTech', loadComponent: () => import('./features/auth/recovery-page').then((m) => m.RecoveryPage) },
      { path: 'auth/password-reset', data: { reset: true }, title: 'Nouveau mot de passe — WorldTech', loadComponent: () => import('./features/auth/action-confirmation-page').then((m) => m.ActionConfirmationPage) },
      { path: 'auth/email-verification', data: { verification: true }, title: 'Confirmer votre email — WorldTech', loadComponent: () => import('./features/auth/action-confirmation-page').then((m) => m.ActionConfirmationPage) },
      { path: 'auth/mfa', title: 'Vérifier votre accès — WorldTech', loadComponent: () => import('./features/auth/mfa-page').then((m) => m.MfaPage) },
      { path: 'auth/securite', data: { manage: true }, title: 'Sécurité du compte — WorldTech', loadComponent: () => import('./features/auth/mfa-page').then((m) => m.MfaPage) },
      { path: 'compte/acces', title: 'Votre accès — WorldTech', loadComponent: () => import('./features/auth/account-access-page').then((m) => m.AccountAccessPage) },
      {
        path: 'acces-refuse', title: 'Accès refusé — WorldTech',
        loadComponent: () => import('./shared/ui/access-denied').then((m) => m.AccessDenied),
      },
      {
        path: 'session-indisponible', title: 'Session indisponible — WorldTech',
        loadComponent: () => import('./features/auth/session-unavailable').then((m) => m.SessionUnavailable),
      },
      {
        path: '**', title: 'Page introuvable — WorldTech',
        loadComponent: () => import('./shared/ui/not-found').then((m) => m.NotFound),
      },
    ],
  },
];
