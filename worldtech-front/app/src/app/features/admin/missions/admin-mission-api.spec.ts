import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { MissionCreateBody, MissionFinancialBody } from '../../../api/generated/types.gen';
import { RUNTIME_CONFIG, parseRuntimeConfig } from '../../../core/config/runtime-config';
import { AdminMissionApi } from './admin-mission-api';

const base = '/api/web/v1';
const accountId = '8bbb9301-a0b6-4b99-b0fd-7eb5804fa4c3';
const siteId = 'd421d526-4e9c-47d1-9048-aca8777882f0';
const missionId = '5e2f07b9-6fea-430b-b57c-b63fc74c8b19';
const missionUrl = `${base}/admin/client-accounts/${accountId}/sites/${siteId}/missions`;

const createBody: MissionCreateBody = {
  title: 'Maintenance réseau',
  publicSummary: 'Intervention préventive.',
  skills: ['Réseau'],
  windowStart: '2026-11-20T09:00:00.000Z',
  windowEnd: '2026-11-20T12:00:00.000Z',
  publicLatitude: 48.8566,
  publicLongitude: 2.3522,
  publicLabel: 'Paris',
};

const financialBody: MissionFinancialBody = { remuneration: '180.00', expenses: null, reason: 'Ajustement' };

describe('AdminMissionApi command contract', () => {
  let requests: HttpTestingController;
  let api: AdminMissionApi;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: RUNTIME_CONFIG, useValue: parseRuntimeConfig({ appName: 'WorldTech', apiBaseUrl: base, googleLoginEnabled: false, telemetryEnabled: false, buildVersion: 'test' }) },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    requests = TestBed.inject(HttpTestingController);
    api = TestBed.inject(AdminMissionApi);
  });

  afterEach(() => requests.verify());

  it('reads account and site pages with bounded server-side paging', () => {
    let accountTotal = 0;
    api.accounts().subscribe((page) => { accountTotal = page.total; });
    const accounts = requests.expectOne((request) => request.url === `${base}/admin/client-accounts`);
    expect(accounts.request.method).toBe('GET');
    expect(accounts.request.params.get('page')).toBe('0');
    expect(accounts.request.params.get('size')).toBe('100');
    expect(accounts.request.headers.get('Accept')).toBe('application/json');
    accounts.flush({ items: [], total: 3, page: 0, size: 100 });
    expect(accountTotal).toBe(3);

    api.sites(accountId).subscribe();
    const sites = requests.expectOne((request) => request.url === `${base}/admin/client-accounts/${accountId}/sites`);
    expect(sites.request.params.get('page')).toBe('0');
    expect(sites.request.params.get('size')).toBe('100');
    sites.flush({ items: [], total: 0, page: 0, size: 100 });
  });

  it('creates a draft with an idempotency key and no precondition header', () => {
    api.create(accountId, siteId, createBody, 'create-key').subscribe();
    const request = requests.expectOne(missionUrl);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toBe('create-key');
    expect(request.request.headers.has('If-Match')).toBe(false);
    expect(request.request.body).toEqual(createBody);
    request.flush({ resourceId: missionId, version: 0, state: 'DRAFT' });
  });

  it('publishes with the exact quoted version and revision number', () => {
    api.publish(accountId, siteId, missionId, 7, financialBody, 'publish-key').subscribe();
    const publish = requests.expectOne(`${missionUrl}/${missionId}/publish`);
    expect(publish.request.headers.get('If-Match')).toBe('"7"');
    expect(publish.request.headers.get('Idempotency-Key')).toBe('publish-key');
    expect(publish.request.body).toEqual({ remuneration: '180.00', expenses: null, reason: 'Ajustement' });
    publish.flush({ resourceId: missionId, version: 8, state: 'PUBLISHED' });

    api.revise(accountId, siteId, missionId, 8, financialBody, 'revision-key').subscribe();
    const revision = requests.expectOne(`${missionUrl}/${missionId}/financial-revisions`);
    expect(revision.request.headers.get('If-Match')).toBe('"8"');
    expect(revision.request.headers.get('Idempotency-Key')).toBe('revision-key');
    revision.flush({ resourceId: missionId, version: 9, state: 'PUBLISHED' });
  });

  it('loads the detail and the complete financial history from the same mission scope', () => {
    api.detail(accountId, siteId, missionId).subscribe();
    requests.expectOne(`${missionUrl}/${missionId}`).flush({ mission: { missionId }, financial: null });

    api.history(accountId, siteId, missionId).subscribe();
    const history = requests.expectOne((request) => request.url === `${missionUrl}/${missionId}/financial-revisions`);
    expect(history.request.method).toBe('GET');
    expect(history.request.params.get('after')).toBe('-1');
    expect(history.request.params.get('size')).toBe('100');
    history.flush({ items: [], nextAfter: -1 });
  });
});
