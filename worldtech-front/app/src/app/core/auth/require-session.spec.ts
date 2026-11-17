import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, Routes } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { requireSession } from './require-session';
import { SessionService } from './session-service';
import { SessionStore } from './session-state';
import { of } from 'rxjs';
import { safeReturnUrl } from './safe-return-url';

describe('private navigation before real identity', () => {
  it.each(['anonymous', 'unavailable'] as const)('never opens a private route with state %s', async (status) => {
    const loadPrivateRoutes = vi.fn(async (): Promise<Routes> => []);
    TestBed.configureTestingModule({ providers: [provideRouter([
      { path: 'admin', canMatch: [requireSession], loadChildren: loadPrivateRoutes },
      { path: 'connexion', children: [] },
      { path: 'session-indisponible', children: [] },
    ]),
      { provide: SessionService, useValue: { resolve: () => of({ kind: status }) } },
      SessionStore,
    ] });
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/admin');
    expect(router.url).toBe((status === 'anonymous' ? '/connexion' : '/session-indisponible') + '?retour=%2Fadmin');
    expect(loadPrivateRoutes).not.toHaveBeenCalled();
  });

  it.each(['https://outside.example', '//outside.example', '/\\outside', '/admin?token=secret',
    '/admin#challenge', '/admin/%2foutside', '/admin/../client', null])('rejects unsafe return paths: %s', (path) => {
    expect(safeReturnUrl(path)).toBeNull();
  });

  it('preserves a normal internal deep link without granting any role', () => {
    expect(safeReturnUrl('/client/sites/abc-123')).toBe('/client/sites/abc-123');
  });
});
