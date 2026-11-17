import { describe, expect, it } from 'vitest';
import { formatEuro } from './money-display';

describe('exact euro display', () => {
  it('distinguishes zero from absent and preserves cents beyond JS safe integers', () => {
    expect(formatEuro(null)).toBe('Non renseigné');
    expect(formatEuro('0')).toBe('0,00\u00a0€');
    expect(formatEuro('120.5')).toBe('120,50\u00a0€');
    expect(formatEuro('9007199254740993.01')).toBe('9\u202f007\u202f199\u202f254\u202f740\u202f993,01\u00a0€');
  });
  it.each(['NaN', '1,20', '1e3', '-1', '1.234', '<script>'])('rejects malformed decimal text: %s', (value) => {
    expect(formatEuro(value)).toBe('Montant invalide');
  });
});
