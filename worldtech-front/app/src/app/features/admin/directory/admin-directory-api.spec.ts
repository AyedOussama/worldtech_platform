import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { RUNTIME_CONFIG, parseRuntimeConfig } from '../../../core/config/runtime-config';
import { AdminDirectoryApi } from './admin-directory-api';

const base = '/api/web/v1';
const dossierId = '8bbb9301-a0b6-4b99-b0fd-7eb5804fa4c3';
const accountId = 'd421d526-4e9c-47d1-9048-aca8777882f0';

describe('AdminDirectoryApi contract', () => {
  let requests: HttpTestingController;
  let api: AdminDirectoryApi;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: RUNTIME_CONFIG, useValue: parseRuntimeConfig({ appName: 'WorldTech', apiBaseUrl: base, googleLoginEnabled: false, telemetryEnabled: false, buildVersion: 'test' }) },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    requests = TestBed.inject(HttpTestingController);
    api = TestBed.inject(AdminDirectoryApi);
  });

  afterEach(() => requests.verify());

  it('reads technician and client admissions with bounded paging and an explicit state', () => {
    api.technicians('SUBMITTED').subscribe();
    const technicians = requests.expectOne((request) => request.url === `${base}/admin/onboardings/technicians`);
    expect(technicians.request.method).toBe('GET');
    expect(technicians.request.params.get('page')).toBe('0');
    expect(technicians.request.params.get('size')).toBe('100');
    expect(technicians.request.params.get('state')).toBe('SUBMITTED');
    expect(technicians.request.headers.get('Accept')).toBe('application/json');
    technicians.flush([]);

    api.clients('NEEDS_INFORMATION').subscribe();
    const clients = requests.expectOne((request) => request.url === `${base}/admin/onboardings/clients`);
    expect(clients.request.params.get('page')).toBe('0');
    expect(clients.request.params.get('size')).toBe('100');
    expect(clients.request.params.get('state')).toBe('NEEDS_INFORMATION');
    clients.flush([]);
  });

  it('loads admission detail and complete history from the same dossier', () => {
    api.technicianDetail(dossierId).subscribe();
    requests.expectOne(`${base}/admin/onboardings/technicians/${dossierId}`).flush({
      userId: accountId,
      dossier: { dossierId, state: 'SUBMITTED', version: 3, profile: {}, documentsStatus: 'REQUIRED_NOT_AVAILABLE' },
    });

    api.technicianHistory(dossierId).subscribe();
    const history = requests.expectOne((request) => request.url === `${base}/admin/onboardings/technicians/${dossierId}/history`);
    expect(history.request.method).toBe('GET');
    expect(history.request.params.get('after')).toBe('-1');
    expect(history.request.params.get('size')).toBe('100');
    history.flush([]);
  });

  it('loads validated client accounts and their asset summary', () => {
    api.clientAccounts().subscribe();
    const accounts = requests.expectOne((request) => request.url === `${base}/admin/client-accounts`);
    expect(accounts.request.params.get('page')).toBe('0');
    expect(accounts.request.params.get('size')).toBe('100');
    accounts.flush({ items: [], total: 0, page: 0, size: 100 });

    api.clientAccount(accountId).subscribe();
    requests.expectOne(`${base}/admin/client-accounts/${accountId}`).flush({ accountId, accountType: 'BUSINESS', displayName: 'Atelier', version: 2 });

    api.clientAccountSummary(accountId).subscribe();
    const summary = requests.expectOne(`${base}/admin/client-accounts/${accountId}/summary`);
    expect(summary.request.method).toBe('GET');
    summary.flush({ sites: 2, equipment: 5 });
  });

  it('loads client admission detail and history', () => {
    api.clientDetail(dossierId).subscribe();
    requests.expectOne(`${base}/admin/onboardings/clients/${dossierId}`).flush({
      userId: accountId,
      dossier: { dossierId, state: 'NEEDS_INFORMATION', version: 4, profile: {} },
    });

    api.clientHistory(dossierId).subscribe();
    const history = requests.expectOne((request) => request.url === `${base}/admin/onboardings/clients/${dossierId}/history`);
    expect(history.request.params.get('after')).toBe('-1');
    expect(history.request.params.get('size')).toBe('100');
    history.flush([]);
  });
});
