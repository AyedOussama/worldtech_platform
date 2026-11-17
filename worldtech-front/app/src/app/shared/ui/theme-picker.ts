import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ThemeService } from '../../core/theme/theme-service';

@Component({
  selector: 'wt-theme-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="theme-picker">Thème
      <select #choice [value]="theme.mode()" (change)="theme.setMode(choice.value)">
        <option value="light">Clair</option>
        <option value="dark">Sombre</option>
        <option value="system">Système</option>
      </select>
    </label>
  `,
})
export class ThemePicker {
  protected readonly theme = inject(ThemeService);
}
