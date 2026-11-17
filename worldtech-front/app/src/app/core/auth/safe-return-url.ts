/** Allow only private internal paths, never arbitrary URLs, queries or action tokens. */
export function safeReturnUrl(value: unknown): string | null {
  if (typeof value !== 'string' || value.length > 1_024) return null;
  return /^\/(?:client|admin)(?:\/[A-Za-z0-9_-]+)*$/.test(value) ? value : null;
}
