import { describe, expect, it, vi } from 'vitest';
import { captureActionFragment, consumeActionToken } from './action-fragment';
import { newPassword } from '../../features/auth/action-confirmation-page';
import { FormControl } from '@angular/forms';

describe('sensitive action links', () => {
  it('removes fragment and query before bootstrap, keeps the opaque token only once in memory', () => {
    const token = 'A'.repeat(43); const replaceState = vi.fn();
    captureActionFragment({ location: { pathname: '/auth/password-reset', hash: '#token=' + token } as Location, history: { replaceState } as unknown as History });
    expect(replaceState).toHaveBeenCalledExactlyOnceWith(null, '', '/auth/password-reset');
    expect(consumeActionToken('reset')).toBe(token); expect(consumeActionToken('reset')).toBeNull();
  });
  it.each(['#token=invalid', '#token=' + 'A'.repeat(43) + '&token=' + 'B'.repeat(43)])('rejects malformed or ambiguous challenges while still scrubbing the URL', (hash) => {
    const replaceState = vi.fn(); captureActionFragment({ location: { pathname: '/auth/email-verification', hash } as Location, history: { replaceState } as unknown as History });
    expect(replaceState).toHaveBeenCalledOnce(); expect(consumeActionToken('verify')).toBeNull();
  });
  it('applies password limits in Unicode code points without trimming spaces', () => {
    expect(newPassword(new FormControl(' '.repeat(15), { nonNullable: true }))).toBeNull();
    expect(newPassword(new FormControl('😀'.repeat(128), { nonNullable: true }))).toBeNull();
    expect(newPassword(new FormControl('😀'.repeat(129), { nonNullable: true }))).not.toBeNull();
  });
});
