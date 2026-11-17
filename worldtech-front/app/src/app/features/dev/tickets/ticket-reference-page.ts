import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { LucideChevronLeft, LucideChevronRight, LucideEllipsis, LucidePlus, LucideSearch } from '@lucide/angular';
import { PageHeader } from '../../../shared/ui/page-header';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ReferenceTicketList } from './reference-ticket-list';
import { IncidentFormDialog } from './incident-form-dialog';
import { TicketDetailDialog } from './ticket-detail-dialog';
import { ReferenceInfoDialog } from './reference-info-dialog';
import { REFERENCE_PRIORITIES, REFERENCE_STATES, REFERENCE_TICKETS, ReferencePriority, ReferenceState, ReferenceTicket } from './ticket-fixtures';

type ListScenario = 'ready' | 'loading' | 'empty' | 'network' | 'forbidden' | 'stale';
const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('fr-FR');

@Component({
  selector: 'wt-ticket-reference-page',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatMenuModule,
    LucidePlus, LucideSearch, LucideEllipsis, LucideChevronLeft, LucideChevronRight,
    PageHeader, EmptyState, ReferenceTicketList],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ticket-reference-page.html',
  styleUrl: './ticket-reference-page.scss',
})
export class TicketReferencePage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly params = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });
  protected readonly space: 'client' | 'admin' = this.route.snapshot.data['space'] === 'admin' ? 'admin' : 'client';
  protected readonly title = this.space === 'admin' ? 'Tickets' : 'Incidents';
  protected readonly tickets = REFERENCE_TICKETS.filter((ticket) => this.space === 'admin' || ticket.account === 'Atelier Boréal');
  protected readonly sites = [...new Set(this.tickets.map((ticket) => ticket.site))];
  protected readonly states = REFERENCE_STATES;
  protected readonly priorities = REFERENCE_PRIORITIES;
  protected readonly priorityKeys = Object.keys(REFERENCE_PRIORITIES) as ReferencePriority[];
  protected readonly tabs: readonly { label: string; value: ReferenceState | '' }[] = [
    { label: 'Tous', value: '' }, { label: 'À qualifier', value: 'OPEN' },
    { label: 'En cours', value: 'IN_PROGRESS' }, { label: 'En attente client', value: 'WAITING_CLIENT' },
  ];
  protected readonly scenario = signal<ListScenario>('ready');
  protected readonly lastOpened = signal<string | null>(null);
  protected readonly query = computed(() => (this.params().get('q') ?? '').slice(0, 160));
  protected readonly site = computed(() => this.sites.includes(this.params().get('site') ?? '') ? this.params().get('site') ?? '' : '');
  protected readonly priority = computed(() => {
    const value = this.params().get('priority') ?? '';
    return Object.hasOwn(this.priorities, value) ? value as ReferencePriority : '';
  });
  protected readonly state = computed(() => {
    const value = this.params().get('state') ?? '';
    return Object.hasOwn(this.states, value) ? value as ReferenceState : '';
  });
  protected readonly activeFilters = computed(() => !!(this.query() || this.site() || this.priority() || this.state()));
  protected readonly filtered = computed(() => {
    const query = normalize(this.query().trim());
    return this.tickets.filter((ticket) =>
      (!query || normalize(`${ticket.reference} ${ticket.title} ${ticket.site} ${ticket.account}`).includes(query)) &&
      (!this.site() || ticket.site === this.site()) && (!this.priority() || ticket.priority === this.priority()) &&
      (!this.state() || ticket.state === this.state()),
    );
  });
  protected readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / 6)));
  protected readonly pageIndex = computed(() => {
    const value = this.params().get('page') ?? '0';
    return /^[0-9]{1,4}$/.test(value) ? Math.min(Number(value), this.pageCount() - 1) : 0;
  });
  protected readonly displayed = computed(() => this.filtered().slice(this.pageIndex() * 6, this.pageIndex() * 6 + 6));
  protected readonly range = computed(() => `${this.pageIndex() * 6 + 1}–${Math.min((this.pageIndex() + 1) * 6, this.filtered().length)} sur ${this.filtered().length}`);

  protected count(state: ReferenceState | ''): number { return this.tickets.filter((ticket) => !state || ticket.state === state).length; }

  protected setFilter(key: 'q' | 'site' | 'priority' | 'state', value: string): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { [key]: value || null, page: null },
      queryParamsHandling: 'merge', replaceUrl: key === 'q' });
  }

  protected resetFilters(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  protected setPage(page: number): void {
    if (page < 0 || page >= this.pageCount()) return;
    void this.router.navigate([], { relativeTo: this.route, queryParams: { page: page || null }, queryParamsHandling: 'merge' });
  }

  protected openTicket(ticket: ReferenceTicket): void {
    this.lastOpened.set(ticket.reference);
    this.dialog.open(TicketDetailDialog, { data: ticket, width: '680px', maxWidth: 'calc(100vw - 32px)', maxHeight: 'calc(100dvh - 32px)', autoFocus: 'first-heading' });
  }

  protected openForm(): void {
    this.dialog.open(IncidentFormDialog, { width: '720px', maxWidth: 'calc(100vw - 32px)', maxHeight: 'calc(100dvh - 32px)', autoFocus: 'first-heading', disableClose: true });
  }

  protected showInfo(): void {
    this.dialog.open(ReferenceInfoDialog, { width: '560px', maxWidth: 'calc(100vw - 32px)', autoFocus: 'first-heading' });
  }

  protected setScenario(value: string): void {
    if (['ready', 'loading', 'empty', 'network', 'forbidden', 'stale'].includes(value)) this.scenario.set(value as ListScenario);
  }
}
