import { describe, expect, it, vi } from 'vitest';
import { loadRuntimeConfig, parseRuntimeConfig } from './runtime-config';

const valid = {
  appName: 'WorldTech', apiBaseUrl: '/api/web/v1', googleLoginEnabled: false,
  telemetryEnabled: false, buildVersion: '0.1.0-test',
};

describe('public startup configuration', () => {
  it('accepts the explicit same-origin configuration and makes it immutable', () => {
    const config = parseRuntimeConfig(valid);
    expect(config.apiBaseUrl).toBe('/api/web/v1');
    expect(Object.isFrozen(config)).toBe(true);
  });

  it.each([
    null, [], {},
    { ...valid, apiBaseUrl: 'https://outside.example/api/web/v1' },
    { ...valid, apiBaseUrl: '//outside.example/api/web/v1' },
    { ...valid, apiBaseUrl: '/api/mobile/v1' },
    { ...valid, demoEnabled: true },
    { ...valid, googleLoginEnabled: true },
    { ...valid, telemetryEnabled: 'false' },
    { ...valid, buildVersion: '<script>untrusted</script>' },
  ])('refuses invalid data without echoing it: %j', (value) => {
    expect(() => parseRuntimeConfig(value)).toThrow('Invalid public configuration');
  });

  it('loads only the fixed local file, without cache or redirects', async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify(valid)));
    await expect(loadRuntimeConfig(fetcher)).resolves.toMatchObject({ appName: 'WorldTech' });
    expect(fetcher).toHaveBeenCalledExactlyOnceWith('/runtime-config.json', expect.objectContaining({
      cache: 'no-store', redirect: 'error', credentials: 'same-origin', signal: expect.any(AbortSignal),
    }));
  });

  it('fails closed on missing, malformed or unavailable configuration', async () => {
    await expect(loadRuntimeConfig(vi.fn<typeof fetch>().mockResolvedValue(new Response('', { status: 404 }))))
      .rejects.toThrow('Public configuration unavailable');
    await expect(loadRuntimeConfig(vi.fn<typeof fetch>().mockResolvedValue(new Response('not json'))))
      .rejects.toThrow();
    await expect(loadRuntimeConfig(vi.fn<typeof fetch>().mockRejectedValue(new TypeError('offline'))))
      .rejects.toThrow();
  });
});
