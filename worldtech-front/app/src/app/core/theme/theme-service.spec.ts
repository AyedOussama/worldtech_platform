import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeService } from './theme-service';

describe('theme preference', () => {
  let systemChange: (() => void) | undefined;
  let prefersDark: boolean;
  beforeEach(() => {
    prefersDark = true;
    document.documentElement.dataset['wtTheme'] = 'system';
    vi.stubGlobal('matchMedia', vi.fn(() => ({
      get matches() { return prefersDark; },
      addEventListener: (_type: string, listener: () => void) => { systemChange = listener; },
      removeEventListener: vi.fn(),
    })));
    TestBed.configureTestingModule({});
  });
  afterEach(() => { vi.unstubAllGlobals(); localStorage.clear(); });

  it('tracks the system, then keeps an explicit preference stable', () => {
    const theme = TestBed.inject(ThemeService);
    expect(document.documentElement.classList.contains('wt-dark')).toBe(true);
    theme.setMode('light');
    systemChange?.();
    expect(document.documentElement.style.colorScheme).toBe('light');
    expect(localStorage.getItem('wt-theme')).toBe('light');
    theme.setMode('system');
    prefersDark = false;
    systemChange?.();
    expect(document.documentElement.style.colorScheme).toBe('light');
    prefersDark = true;
    systemChange?.();
    expect(document.documentElement.style.colorScheme).toBe('dark');
  });

  it('still changes theme if local storage is blocked and ignores invalid values', () => {
    const theme = TestBed.inject(ThemeService);
    const storage = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    theme.setMode('dark');
    expect(theme.mode()).toBe('dark');
    expect(document.documentElement.style.colorScheme).toBe('dark');
    theme.setMode('not-a-theme');
    expect(theme.mode()).toBe('dark');
    storage.mockRestore();
  });
});
