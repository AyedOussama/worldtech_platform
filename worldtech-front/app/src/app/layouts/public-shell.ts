import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ThemePicker } from '../shared/ui/theme-picker';

@Component({
  selector: 'wt-public-shell',
  imports: [RouterLink, RouterOutlet, ThemePicker],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="skip-link" href="#contenu" (click)="skipToContent($event, content)">Aller au contenu</a>
    <header class="public-header">
      <a class="brand" routerLink="/connexion" aria-label="WorldTech — accueil">WorldTech</a>
      <wt-theme-picker />
    </header>
    <main #content id="contenu" tabindex="-1" class="public-content"><router-outlet /></main>
    <footer class="public-footer">Portail client et administration</footer>
  `,
})
export class PublicShell {
  protected skipToContent(event: Event, target: HTMLElement): void {
    // With <base href="/"> a raw fragment would navigate away from a deep link.
    event.preventDefault();
    target.focus();
    target.scrollIntoView({ block: 'start' });
  }
}
