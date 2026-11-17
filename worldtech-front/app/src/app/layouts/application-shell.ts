import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { LucideDynamicIcon, LucideIcon, LucideMenu, LucideX } from '@lucide/angular';
import { ThemePicker } from '../shared/ui/theme-picker';

export interface NavigationItem {
  readonly label: string;
  readonly icon: LucideIcon;
  readonly path?: string;
}

/** Layout only: items are not capabilities and this component grants no access. */
@Component({
  selector: 'wt-application-shell',
  imports: [RouterLink, RouterLinkActive, MatSidenavModule, MatButtonModule, LucideDynamicIcon, LucideMenu, LucideX, ThemePicker],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './application-shell.html',
  styleUrl: './application-shell.scss',
})
export class ApplicationShell {
  readonly space = input.required<'client' | 'admin'>();
  readonly items = input.required<readonly NavigationItem[]>();
  readonly preview = input(false);
  protected readonly spaceLabel = computed(() => this.space() === 'admin' ? 'Administration' : 'Espace client');
  private readonly breakpoint = toSignal(inject(BreakpointObserver).observe('(max-width: 959px)'));
  protected readonly mobile = computed(() => this.breakpoint()?.matches ?? false);
  protected readonly navigationOpen = signal(false);
  private readonly menuButton = viewChild<unknown, ElementRef<HTMLButtonElement>>('menuButton', { read: ElementRef });

  protected closeNavigation(): void { this.navigationOpen.set(false); }

  protected restoreNavigationFocus(): void {
    if (this.mobile()) this.menuButton()?.nativeElement.focus();
  }

  protected skip(event: Event, target: HTMLElement): void {
    event.preventDefault();
    target.focus();
    target.scrollIntoView({ block: 'start' });
  }
}
