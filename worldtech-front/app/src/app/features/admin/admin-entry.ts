import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import {
  LucideArrowRight,
  LucideBriefcaseBusiness,
  LucideHardHat,
  LucideShieldCheck,
  LucideUsersRound,
} from '@lucide/angular';
import { PageHeader } from '../../shared/ui/page-header';

@Component({
  selector: 'wt-admin-entry',
  imports: [
    PageHeader,
    RouterLink,
    MatButtonModule,
    LucideArrowRight,
    LucideBriefcaseBusiness,
    LucideHardHat,
    LucideShieldCheck,
    LucideUsersRound,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <wt-page-header title="Administration" description="Pilotez les missions, les techniciens et les comptes clients" />

    <section class="admin-home" aria-label="Espaces d’administration">
      <article class="mission-panel">
        <div class="panel-heading">
          <span class="card-icon accent"><svg lucideBriefcaseBusiness class="wt-icon" aria-hidden="true"></svg></span>
          <div>
            <p class="kicker">Opérations</p>
            <h2>Missions</h2>
          </div>
        </div>

        <p class="panel-copy">Préparez les interventions, publiez les conditions financières et suivez les révisions</p>

        <ol class="mission-flow" aria-label="Déroulé d’une mission">
          <li>
            <span class="flow-step">1</span>
            <div class="flow-copy"><strong>Préparer</strong><span>Cadrez la demande et les conditions</span></div>
          </li>
          <li>
            <span class="flow-step">2</span>
            <div class="flow-copy"><strong>Publier</strong><span>Rendez la mission visible aux techniciens</span></div>
          </li>
          <li>
            <span class="flow-step">3</span>
            <div class="flow-copy"><strong>Suivre</strong><span>Gardez la main sur les révisions</span></div>
          </li>
        </ol>

        <a matButton="filled" routerLink="/admin/missions" class="primary-action">
          Ouvrir les missions<svg lucideArrowRight class="wt-icon" aria-hidden="true"></svg>
        </a>
      </article>

      <div class="admin-links" aria-label="Autres espaces">
        <a class="access-card" routerLink="/admin/techniciens">
          <span class="card-icon"><svg lucideHardHat class="wt-icon" aria-hidden="true"></svg></span>
          <span class="access-copy">
            <span class="kicker">Réseau</span>
            <span class="access-title">Techniciens</span>
            <span class="access-description">Consultez les candidatures en attente et les techniciens validés</span>
          </span>
          <svg lucideArrowRight class="access-arrow" aria-hidden="true"></svg>
        </a>

        <a class="access-card" routerLink="/admin/clients">
          <span class="card-icon"><svg lucideUsersRound class="wt-icon" aria-hidden="true"></svg></span>
          <span class="access-copy">
            <span class="kicker">Comptes</span>
            <span class="access-title">Clients</span>
            <span class="access-description">Retrouvez les comptes validés et les demandes d’inscription en attente</span>
          </span>
          <svg lucideArrowRight class="access-arrow" aria-hidden="true"></svg>
        </a>

        <a class="access-card" routerLink="/auth/securite">
          <span class="card-icon"><svg lucideShieldCheck class="wt-icon" aria-hidden="true"></svg></span>
          <span class="access-copy">
            <span class="kicker">Accès</span>
            <span class="access-title">Sécurité</span>
            <span class="access-description">Gérez la double authentification et les preuves demandées pour les actions sensibles</span>
          </span>
          <svg lucideArrowRight class="access-arrow" aria-hidden="true"></svg>
        </a>
      </div>
    </section>

    <aside class="security-note">
      <svg lucideShieldCheck class="wt-icon" aria-hidden="true"></svg>
      <p>Session vérifiée par le serveur. Les actions sensibles demandent une preuve MFA récente</p>
    </aside>
  `,
  styles: `
    :host { display: block; }
    .admin-home { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(320px, .85fr); gap: 24px; align-items: start; }
    .mission-panel { display: flex; flex-direction: column; gap: 24px; padding: 28px; background: var(--wt-surface); border: 1px solid var(--wt-border); border-radius: var(--wt-radius-panel); }
    .panel-heading { display: flex; align-items: center; gap: 16px; }
    .card-icon { display: grid; width: 44px; height: 44px; flex: none; place-items: center; color: var(--wt-muted); background: var(--wt-selected); border-radius: 8px; }
    .card-icon.accent { color: var(--wt-accent); background: var(--wt-info-surface); }
    .kicker { margin: 0 0 4px; color: var(--wt-muted); font-size: .8125rem; line-height: 1.4; }
    h2 { margin: 0; font-size: 1.5rem; font-weight: 500; line-height: 1.2; }
    .panel-copy { max-width: 52ch; margin: 0; color: var(--wt-secondary); font-size: .9375rem; line-height: 1.6; }
    .mission-flow { display: grid; margin: 0; padding: 20px; list-style: none; background: var(--wt-info-surface); border: 1px solid var(--wt-border); border-radius: 8px; }
    .mission-flow li { position: relative; display: grid; grid-template-columns: 32px minmax(0, 1fr); gap: 12px; padding-bottom: 20px; }
    .mission-flow li:last-child { padding-bottom: 0; }
    .mission-flow li:not(:last-child)::before { content: ''; position: absolute; top: 32px; bottom: 0; left: 15px; width: 1px; background: var(--wt-border); }
    .flow-step { display: grid; width: 32px; height: 32px; place-items: center; color: var(--wt-muted); background: var(--wt-surface); border: 1px solid var(--wt-border); border-radius: 50%; font-size: .8125rem; font-variant-numeric: tabular-nums; }
    .flow-copy { display: grid; gap: 2px; padding-top: 4px; }
    .flow-copy strong { font-size: .9375rem; font-weight: 500; }
    .flow-copy span { color: var(--wt-secondary); font-size: .875rem; line-height: 1.45; }
    .primary-action { align-self: flex-start; }
    .primary-action .wt-icon { margin-left: 8px; }
    .admin-links { display: grid; gap: 16px; }
    .access-card { display: grid; grid-template-columns: 44px minmax(0, 1fr) 18px; gap: 16px; align-items: start; padding: 20px; color: inherit; text-decoration: none; background: var(--wt-surface); border: 1px solid var(--wt-border); border-radius: var(--wt-radius-panel); transition: background-color var(--wt-feedback) var(--wt-ease), border-color var(--wt-feedback) var(--wt-ease); }
    .access-card:hover { background: var(--wt-selected); border-color: var(--wt-control-border); }
    .access-card:focus-visible { outline: 3px solid var(--wt-accent); outline-offset: 2px; }
    .access-copy { display: grid; gap: 4px; min-width: 0; }
    .access-copy .kicker { margin: 0; }
    .access-title { font-size: 1.125rem; font-weight: 500; line-height: 1.3; }
    .access-description { color: var(--wt-secondary); font-size: .875rem; line-height: 1.5; }
    .access-arrow { align-self: center; color: var(--wt-muted); transition: color var(--wt-feedback) var(--wt-ease), transform var(--wt-feedback) var(--wt-ease); }
    .access-card:hover .access-arrow { color: var(--wt-accent); transform: translateX(2px); }
    .security-note { display: flex; align-items: flex-start; gap: 12px; margin-top: 24px; padding: 16px 20px; color: var(--wt-muted); background: var(--wt-surface); border: 1px solid var(--wt-border); border-radius: var(--wt-radius-panel); }
    .security-note .wt-icon { flex: none; margin-top: 1px; }
    .security-note p { margin: 0; font-size: .8125rem; line-height: 1.5; }
    @media (max-width: 1100px) {
      .admin-home { grid-template-columns: 1fr; }
      .admin-links { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .access-card:last-child { grid-column: 1 / -1; }
    }
    @media (max-width: 700px) {
      .admin-links { grid-template-columns: 1fr; }
      .access-card:last-child { grid-column: auto; }
    }
    @media (max-width: 600px) {
      .mission-panel { padding: 20px; }
      .access-card { grid-template-columns: 44px minmax(0, 1fr); padding: 16px; }
      .access-arrow { display: none; }
    }
    @media (prefers-reduced-motion: reduce) {
      .access-card, .access-arrow { transition: none; }
    }
  `,
})
export class AdminEntry {}
