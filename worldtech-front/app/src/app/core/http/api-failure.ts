import { HttpErrorResponse } from '@angular/common/http';

/** Shape from the real API 0.6.0; its text is untrusted, not a UI translation. */
export interface ProblemDetail {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly detail: string;
  readonly instance: string;
  readonly code: string;
  readonly traceId: string;
  readonly fieldErrors?: readonly { readonly field: string; readonly code: string }[];
}

export interface ApiFailure {
  readonly kind: 'network' | 'http';
  readonly status: number;
  readonly code: string;
  readonly traceId: string | null;
  readonly fieldErrors: readonly { readonly field: string; readonly code: string }[];
  readonly retryAfterSeconds: number | null;
}

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function toApiFailure(error: HttpErrorResponse): ApiFailure {
  const body: unknown = error.error;
  const validBody = record(body) && body['status'] === error.status &&
    typeof body['code'] === 'string' && /^[A-Z][A-Z0-9_]{0,79}$/.test(body['code']);
  const traceId = validBody && typeof body['traceId'] === 'string' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(body['traceId'])
    ? body['traceId'] : null;
  const fields: { field: string; code: string }[] = [];
  if (validBody && Array.isArray(body['fieldErrors'])) {
    for (const value of body['fieldErrors'].slice(0, 20) as unknown[]) {
      if (record(value) && typeof value['field'] === 'string' &&
        /^[A-Za-z0-9_.[\]]{1,160}$/.test(value['field']) &&
        typeof value['code'] === 'string' && /^[A-Za-z][A-Za-z0-9_.-]{0,79}$/.test(value['code'])) {
        fields.push(Object.freeze({ field: value['field'], code: value['code'] }));
      }
    }
  }
  const retry = error.headers.get('Retry-After');
  return Object.freeze({
    kind: error.status === 0 ? 'network' : 'http',
    status: error.status,
    code: error.status === 0 ? 'NETWORK_UNAVAILABLE' : validBody ? body['code'] as string : 'UNEXPECTED_RESPONSE',
    traceId,
    fieldErrors: Object.freeze(fields),
    retryAfterSeconds: retry !== null && /^[0-9]{1,5}$/.test(retry) && Number(retry) <= 86_400 ? Number(retry) : null,
  });
}
