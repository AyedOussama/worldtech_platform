import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { RUNTIME_CONFIG } from '../config/runtime-config';
import { toApiFailure } from './api-failure';

export const apiFailureInterceptor: HttpInterceptorFn = (request, next) => {
  const base = inject(RUNTIME_CONFIG).apiBaseUrl;
  const isWebApi = request.url === base || request.url.startsWith(base + '/') || request.url.startsWith(base + '?');
  if (!isWebApi) return next(request);

  // No Bearer, retries, redirect, mock fallback or replay of mutations here.
  return next(request).pipe(catchError((error: unknown) =>
    throwError(() => error instanceof HttpErrorResponse ? toApiFailure(error) : error),
  ));
};
