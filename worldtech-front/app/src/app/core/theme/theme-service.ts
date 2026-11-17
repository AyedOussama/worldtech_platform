import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';

function isThemeMode(value: unknown): value is ThemeMode {
  return value === 'light' || value === 'dark' || value === 'system';
}

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly window = this.document.defaultView;
  private readonly selected = signal<ThemeMode>(this.initialMode());
  readonly mode = this.selected.asReadonly();
  private media: MediaQueryList | undefined;

  constructor() {
    const destroy = inject(DestroyRef);
    try {
      this.media = this.window?.matchMedia('(prefers-color-scheme: dark)');
      const onSystemChange = () => this.apply();
      this.media?.addEventListener('change', onSystemChange);
      destroy.onDestroy(() => this.media?.removeEventListener('change', onSystemChange));
    } catch { /* Browser API unavailable: keep an explicit light/dark mode usable. */ }
    this.apply();
  }

  setMode(mode: string): void {
    if (!isThemeMode(mode)) return;
    this.selected.set(mode);
    try {
      this.window?.localStorage.setItem('wt-theme', mode);
    } catch { /* Preference is optional; the active theme still changes. */ }
    this.apply();
  }

  private initialMode(): ThemeMode {
    const value = this.document.documentElement.dataset['wtTheme'];
    return isThemeMode(value) ? value : 'system';
  }

  private apply(): void {
    const dark = this.mode() === 'dark' || (this.mode() === 'system' && !!this.media?.matches);
    const root = this.document.documentElement;
    root.classList.toggle('wt-dark', dark);
    root.style.colorScheme = dark ? 'dark' : 'light';
    root.dataset['wtTheme'] = this.mode();
  }
}
