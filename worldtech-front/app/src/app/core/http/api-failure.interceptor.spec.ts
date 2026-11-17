import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RUNTIME_CONFIG, parseRuntimeConfig } from '../config/runtime-config';
import { apiFailureInterceptor } from './api-failure.interceptor';

describe('web API failures', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      { provide: RUNTIME_CONFIG, useValue: parseRuntimeConfig({ appName: 'WorldTech', apiBaseUrl: '/api/web/v1',
        googleLoginEnabled: false, telemetryEnabled: false, buildVersion: 'test' }) },
      provideHttpClient(withInterceptors([apiFailureInterceptor])), provideHttpClientTesting(),
    ] });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
  });
  afterEach(() => requests.verify());

  it.each([403, 409, 412, 503])('does not retry or mask a failed mutation (%s)', (status) => {
    const error = vi.fn();
    http.post('/api/web/v1/example', {}).subscribe({ error });
    const request = requests.expectOne('/api/web/v1/example');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({ status, code: status === 403 ? 'ACCESS_DENIED' : 'STATE_CONFLICT',
      traceId: '00000000-0000-4000-8000-000000000001', detail: 'Never echo server text',
    }, { status, statusText: 'Refused' });
    expect(error).toHaveBeenCalledOnce();
    expect(error.mock.calls[0]?.[0]).toMatchObject({ kind: 'http', status });
    expect(JSON.stringify(error.mock.calls[0]?.[0])).not.toContain('Never echo');
    requests.expectNone('/api/web/v1/example');
  });

  it('distinguishes a network error without falling back to mock data', () => {
    const next = vi.fn();
    const error = vi.fn();
    http.get('/api/web/v1/me').subscribe({ next, error });
    requests.expectOne('/api/web/v1/me').error(new ProgressEvent('error'));
    expect(next).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalledWith(expect.objectContaining({ kind: 'network', code: 'NETWORK_UNAVAILABLE' }));
  });

  it('bounds field errors and preserves documented retry guidance', () => {
    const error = vi.fn();
    http.post('/api/web/v1/example', {}).subscribe({ error });
    requests.expectOne('/api/web/v1/example').flush({ status: 429, code: 'TOO_MANY_REQUESTS',
      fieldErrors: [{ field: 'name', code: 'NotBlank' }, { field: '<script>', code: 'unsafe' }],
    }, { status: 429, statusText: 'Limited', headers: { 'Retry-After': '60' } });
    expect(error).toHaveBeenCalledWith(expect.objectContaining({ retryAfterSeconds: 60,
      fieldErrors: [{ field: 'name', code: 'NotBlank' }],
    }));
  });

  it('does not trust a body claiming a different HTTP status', () => {
    const error = vi.fn();
    http.get('/api/web/v1/me').subscribe({ error });
    requests.expectOne('/api/web/v1/me').flush({ status: 200, code: 'AUTHENTICATED' }, { status: 503, statusText: 'Unavailable' });
    expect(error).toHaveBeenCalledWith(expect.objectContaining({ status: 503, code: 'UNEXPECTED_RESPONSE' }));
  });

  it('does not apply web API processing to unrelated destinations', () => {
    const error = vi.fn();
    http.get('https://outside.example/asset').subscribe({ error });
    const request = requests.expectOne('https://outside.example/asset');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({}, { status: 403, statusText: 'Refused' });
    expect(error.mock.calls[0]?.[0]).toBeInstanceOf(HttpErrorResponse);
  });
});
