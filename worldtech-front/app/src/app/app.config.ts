import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors, withNoXsrfProtection } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { RUNTIME_CONFIG, RuntimeConfig } from './core/config/runtime-config';
import { apiFailureInterceptor } from './core/http/api-failure.interceptor';
import { webSessionInterceptor } from './core/http/web-session.interceptor';

export function createAppConfig(config: RuntimeConfig): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      { provide: RUNTIME_CONFIG, useValue: config },
      // Spring exposes CSRF as JSON, not the Angular XSRF cookie convention.
      provideHttpClient(withNoXsrfProtection(), withInterceptors([apiFailureInterceptor, webSessionInterceptor])),
      provideRouter(routes),
    ],
  };
}
