import { InjectionToken } from '@angular/core';

export interface RuntimeConfig {
  readonly appName: 'WorldTech';
  readonly apiBaseUrl: '/api/web/v1';
  readonly googleLoginEnabled: false;
  readonly telemetryEnabled: false;
  readonly buildVersion: string;
}

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('WorldTech public configuration');

export function parseRuntimeConfig(value: unknown): RuntimeConfig {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error('Invalid public configuration');
  }
  const config = value as Record<string, unknown>;
  const keys = ['appName', 'apiBaseUrl', 'googleLoginEnabled', 'telemetryEnabled', 'buildVersion'];
  if (
    Object.keys(config).length !== keys.length ||
    keys.some((key) => !Object.hasOwn(config, key)) ||
    config['appName'] !== 'WorldTech' ||
    config['apiBaseUrl'] !== '/api/web/v1' ||
    config['googleLoginEnabled'] !== false ||
    config['telemetryEnabled'] !== false ||
    typeof config['buildVersion'] !== 'string' ||
    !/^[A-Za-z0-9][A-Za-z0-9._+-]{0,99}$/.test(config['buildVersion'])
  ) {
    // Do not echo untrusted config or URLs into logs/UI.
    throw new Error('Invalid public configuration');
  }
  return Object.freeze({
    appName: 'WorldTech',
    apiBaseUrl: '/api/web/v1',
    googleLoginEnabled: false,
    telemetryEnabled: false,
    buildVersion: config['buildVersion'],
  });
}

export async function loadRuntimeConfig(fetcher: typeof fetch = globalThis.fetch): Promise<RuntimeConfig> {
  const response = await fetcher('/runtime-config.json', {
    cache: 'no-store',
    credentials: 'same-origin',
    redirect: 'error',
    signal: AbortSignal.timeout(8_000),
  });
  if (!response.ok) {
    throw new Error('Public configuration unavailable');
  }
  const value: unknown = await response.json();
  return parseRuntimeConfig(value);
}
