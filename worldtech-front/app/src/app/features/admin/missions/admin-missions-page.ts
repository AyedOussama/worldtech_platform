import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucidePlus, LucideRefreshCw } from '@lucide/angular';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import type {
  ClientAccountView,
  MissionDetailView,
  MissionFinancialView,
  MissionSummaryView,
  SiteView,
} from '../../../api/generated/types.gen';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorSummary, type FieldIssue } from '../../../shared/ui/error-summary';
import { MoneyDisplay } from '../../../shared/ui/money-display';
import { PageHeader } from '../../../shared/ui/page-header';
import { StatusLabel, type StatusTone } from '../../../shared/ui/status-label';
import { AdminMissionApi } from './admin-mission-api';
import { isNetworkFailure, missionFailureMessage } from './mission-feedback';
import {
  buildMissionCreateBody,
  buildMissionFinancialBody,
  formatDateTime,
  formatWindow,
  localDateTime,
  validateMissionDraft,
  validateMissionFinancial,
} from './mission-form';

@Component({
  selector: 'wt-admin-missions-page',
  imports: [
    ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    PageHeader, ErrorSummary, EmptyState, MoneyDisplay, StatusLabel, LucidePlus, LucideRefreshCw,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-missions-page.html',
})
export class AdminMissionsPage {
  private readonly api = inject(AdminMissionApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly issueSummary = viewChild(ErrorSummary);

  protected readonly accounts = signal<readonly ClientAccountView[]>([]);
  protected readonly sites = signal<readonly SiteView[]>([]);
  protected readonly missions = signal<readonly MissionSummaryView[]>([]);
  protected readonly detail = signal<MissionDetailView | null>(null);
  protected readonly history = signal<readonly MissionFinancialView[]>([]);
  protected readonly accountId = signal('');
  protected readonly siteId = signal('');
  protected readonly selectedId = signal('');
  protected readonly issues = signal<readonly FieldIssue[]>([]);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly loading = signal(false);
  protected readonly detailLoading = signal(false);
  protected readonly busy = signal(false);
  protected readonly mode = signal<'detail' | 'create'>('detail');
  protected readonly filter = signal<'ALL' | MissionSummaryView['state']>('ALL');

  protected readonly formatDateTime = formatDateTime;
  protected readonly formatWindow = formatWindow;
  protected readonly counts = computed(() => {
    const all = this.missions();
    return {
      all: all.length,
      draft: all.filter((mission) => mission.state === 'DRAFT').length,
      published: all.filter((mission) => mission.state === 'PUBLISHED').length,
    };
  });
  protected readonly filteredMissions = computed(() => {
    const filter = this.filter();
    const all = this.missions();
    return filter === 'ALL' ? all : all.filter((mission) => mission.state === filter);
  });

  protected readonly missionForm = new FormGroup({
    title: new FormControl('Maintenance réseau', { nonNullable: true }),
    publicSummary: new FormControl('Intervention préventive sur l’infrastructure réseau du site.', { nonNullable: true }),
    skills: new FormControl('Réseau, Windows Server', { nonNullable: true }),
    windowStart: new FormControl(localDateTime(60 * 60 * 1000), { nonNullable: true }),
    windowEnd: new FormControl(localDateTime(5 * 60 * 60 * 1000), { nonNullable: true }),
    publicLatitude: new FormControl('48.856600', { nonNullable: true }),
    publicLongitude: new FormControl('2.352200', { nonNullable: true }),
    publicLabel: new FormControl('Paris centre', { nonNullable: true }),
  });

  protected readonly financialForm = new FormGroup({
    remuneration: new FormControl('', { nonNullable: true }),
    expenses: new FormControl('', { nonNullable: true }),
    reason: new FormControl('', { nonNullable: true }),
  });

  private createKey: string | null = null;
  private publishKey: string | null = null;
  private revisionKey: string | null = null;

  constructor() {
    this.loadAccounts();
  }

  protected reload(): void {
    this.loadAccounts();
  }

  protected openCreate(): void {
    this.mode.set('create');
    this.error.set('');
    this.notice.set('');
    this.issues.set([]);
    setTimeout(() => (document.getElementById('mission-title') as HTMLElement | null)?.focus());
  }

  protected cancelCreate(): void {
    this.mode.set('detail');
    this.issues.set([]);
    this.error.set('');
  }

  protected setFilter(filter: 'ALL' | MissionSummaryView['state']): void {
    this.filter.set(filter);
  }

  protected selectAccount(accountId: string): void {
    this.accountId.set(accountId);
    this.siteId.set('');
    this.sites.set([]);
    this.resetMissionSelection();
    this.loadSites(accountId);
  }

  protected selectSite(siteId: string): void {
    this.siteId.set(siteId);
    this.resetMissionSelection();
    this.loadMissions();
  }

  protected selectMission(missionId: string): void {
    const accountId = this.accountId();
    const siteId = this.siteId();
    if (!accountId || !siteId) return;
    this.selectedId.set(missionId);
    this.mode.set('detail');
    this.resetCommandKeys();
    this.issues.set([]);
    this.detailLoading.set(true);
    forkJoin({
      detail: this.api.detail(accountId, siteId, missionId),
      history: this.api.history(accountId, siteId, missionId),
    }).pipe(
      finalize(() => this.detailLoading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: ({ detail, history }) => {
        this.detail.set(detail);
        this.history.set(history.items);
        this.financialForm.reset({
          remuneration: detail.financial?.remuneration ?? '',
          expenses: detail.financial?.expenses ?? '',
          reason: '',
        });
      },
      error: (error: unknown) => {
        this.detail.set(null);
        this.history.set([]);
        this.error.set(missionFailureMessage(error));
      },
    });
  }

  protected refreshSelected(): void {
    if (this.selectedId()) this.selectMission(this.selectedId());
  }

  protected createMission(): void {
    if (this.busy()) return;
    const accountId = this.accountId();
    const siteId = this.siteId();
    if (!accountId || !siteId) return;
    const input = this.missionForm.getRawValue();
    const issues = validateMissionDraft(input);
    if (!this.showIssues(issues)) return;
    this.startCommand();
    const key = this.createKey ?? this.newIdempotencyKey();
    this.createKey = key;
    this.api.create(accountId, siteId, buildMissionCreateBody(input), key).pipe(
      finalize(() => this.busy.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (command) => {
        this.createKey = null;
        this.mode.set('detail');
        this.notice.set('Brouillon créé. La mission est prête à être publiée.');
        this.missionForm.patchValue({
          windowStart: localDateTime(60 * 60 * 1000),
          windowEnd: localDateTime(5 * 60 * 60 * 1000),
        });
        this.loadMissions(command.resourceId);
      },
      error: (error: unknown) => this.commandFailed(error, 'create'),
    });
  }

  protected submitFinancial(): void {
    if (this.busy()) return;
    const current = this.detail();
    const accountId = this.accountId();
    const siteId = this.siteId();
    if (!current || !accountId || !siteId) return;
    const input = this.financialForm.getRawValue();
    const issues = validateMissionFinancial(input);
    if (!this.showIssues(issues)) return;
    const publishing = current.mission.state === 'DRAFT';
    this.startCommand();
    const key = publishing
      ? (this.publishKey ?? this.newIdempotencyKey())
      : (this.revisionKey ?? this.newIdempotencyKey());
    if (publishing) this.publishKey = key;
    else this.revisionKey = key;
    const body = buildMissionFinancialBody(input);
    const request = publishing
      ? this.api.publish(accountId, siteId, current.mission.missionId, current.mission.version, body, key)
      : this.api.revise(accountId, siteId, current.mission.missionId, current.mission.version, body, key);
    request.pipe(
      finalize(() => this.busy.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (command) => {
        if (publishing) this.publishKey = null;
        else this.revisionKey = null;
        this.notice.set(publishing ? 'Mission publiée avec ses conditions financières.' : 'Révision financière enregistrée.');
        this.loadMissions(command.resourceId);
      },
      error: (error: unknown) => this.commandFailed(error, publishing ? 'publish' : 'revision'),
    });
  }

  protected stateLabel(state: MissionSummaryView['state']): string {
    return state === 'DRAFT' ? 'Brouillon' : 'Publiée';
  }

  protected stateTone(state: MissionSummaryView['state']): StatusTone {
    return state === 'DRAFT' ? 'warning' : 'success';
  }

  protected nextAction(state: MissionSummaryView['state']): string {
    return state === 'DRAFT' ? 'À publier' : 'Révision possible';
  }

  protected siteLabel(site: SiteView): string {
    return site.profile.name?.trim() || site.siteId;
  }

  protected locationLabel(mission: MissionSummaryView): string {
    const label = mission.publicLabel?.trim();
    return `${mission.publicLatitude}, ${mission.publicLongitude}${label ? ` · ${label}` : ''}`;
  }

  private loadAccounts(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.accounts().pipe(
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (page) => {
        this.accounts.set(page.items);
        const first = page.items[0];
        if (first) this.selectAccount(first.accountId);
      },
      error: (error: unknown) => this.error.set(missionFailureMessage(error)),
    });
  }

  private loadSites(accountId: string): void {
    this.loading.set(true);
    this.error.set('');
    this.api.sites(accountId).pipe(
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (page) => {
        this.sites.set(page.items);
        const first = page.items[0];
        if (first) this.selectSite(first.siteId);
      },
      error: (error: unknown) => this.error.set(missionFailureMessage(error)),
    });
  }

  private loadMissions(preferredId?: string): void {
    const accountId = this.accountId();
    const siteId = this.siteId();
    if (!accountId || !siteId) return;
    this.loading.set(true);
    this.error.set('');
    this.api.missions(accountId, siteId).pipe(
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (page) => {
        this.missions.set(page.items);
        const preferred = preferredId ?? this.selectedId();
        const selected = page.items.find((mission) => mission.missionId === preferred) ?? page.items[0];
        if (selected) this.selectMission(selected.missionId);
        else this.resetMissionSelection();
      },
      error: (error: unknown) => this.error.set(missionFailureMessage(error)),
    });
  }

  private resetMissionSelection(): void {
    this.selectedId.set('');
    this.detail.set(null);
    this.history.set([]);
    this.issues.set([]);
    this.resetCommandKeys();
    this.financialForm.reset({ remuneration: '', expenses: '', reason: '' });
  }

  private resetCommandKeys(): void {
    this.createKey = null;
    this.publishKey = null;
    this.revisionKey = null;
  }

  private startCommand(): void {
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    this.issues.set([]);
  }

  private commandFailed(error: unknown, operation: 'create' | 'publish' | 'revision'): void {
    this.error.set(missionFailureMessage(error));
    if (isNetworkFailure(error)) return;
    if (operation === 'create') this.createKey = null;
    else if (operation === 'publish') this.publishKey = null;
    else this.revisionKey = null;
  }

  private showIssues(issues: readonly FieldIssue[]): boolean {
    this.issues.set(issues);
    if (issues.length === 0) return true;
    this.error.set('');
    setTimeout(() => this.issueSummary()?.focus());
    return false;
  }

  private newIdempotencyKey(): string {
    if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
    return [...crypto.getRandomValues(new Uint8Array(16))]
      .map((value) => value.toString(16).padStart(2, '0'))
      .join('');
  }
}
