import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { LucideRefreshCw } from '@lucide/angular';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import type {
  AdminAdmissionHistoryEvent,
  AdmissionSummary,
  AssetSummary,
  ClientAccountView,
} from '../../../api/generated/types.gen';
import { EmptyState } from '../../../shared/ui/empty-state';
import { PageHeader } from '../../../shared/ui/page-header';
import { StatusLabel } from '../../../shared/ui/status-label';
import {
  PENDING_STATES,
  admissionActionLabel,
  admissionStateLabel,
  admissionStateTone,
  clientAccountTypeLabel,
  clientDisplayName,
  clientProfileTypeLabel,
  directoryFailureMessage,
  dossierReference,
  formatDateTime,
  sortAdmissions,
} from '../directory/admin-directory';
import { AdminDirectoryApi, type ClientAdmissionDetail } from '../directory/admin-directory-api';

type ClientFilter = 'VALIDATED' | 'PENDING';

interface AccountRow {
  readonly kind: 'account';
  readonly id: string;
  readonly label: string;
  readonly meta: string;
  readonly version: number;
  readonly account: ClientAccountView;
}

interface AdmissionRow {
  readonly kind: 'admission';
  readonly id: string;
  readonly label: string;
  readonly meta: string;
  readonly version: number;
  readonly summary: AdmissionSummary;
}

type ClientRow = AccountRow | AdmissionRow;

@Component({
  selector: 'wt-admin-clients-page',
  imports: [MatButtonModule, PageHeader, EmptyState, StatusLabel, LucideRefreshCw],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-clients-page.html',
})
export class AdminClientsPage {
  private readonly api = inject(AdminDirectoryApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly filter = signal<ClientFilter>('VALIDATED');
  protected readonly accounts = signal<readonly ClientAccountView[]>([]);
  protected readonly accountTotal = signal(0);
  protected readonly pending = signal<readonly AdmissionSummary[]>([]);
  protected readonly selected = signal<ClientRow | null>(null);
  protected readonly accountDetail = signal<ClientAccountView | null>(null);
  protected readonly assetSummary = signal<AssetSummary | null>(null);
  protected readonly admissionDetail = signal<ClientAdmissionDetail | null>(null);
  protected readonly history = signal<readonly AdminAdmissionHistoryEvent[]>([]);
  protected readonly loading = signal(false);
  protected readonly detailLoading = signal(false);
  protected readonly error = signal('');

  protected readonly formatDateTime = formatDateTime;
  protected readonly dossierReference = dossierReference;
  protected readonly stateLabel = admissionStateLabel;
  protected readonly stateTone = admissionStateTone;
  protected readonly actionLabel = admissionActionLabel;
  protected readonly accountTypeLabel = clientAccountTypeLabel;
  protected readonly profileTypeLabel = clientProfileTypeLabel;
  protected readonly clientName = clientDisplayName;

  protected readonly rows = computed<readonly ClientRow[]>(() => this.filter() === 'VALIDATED'
    ? this.accounts().map((account): AccountRow => ({
      kind: 'account',
      id: account.accountId,
      label: account.displayName,
      meta: clientAccountTypeLabel(account.accountType),
      version: account.version,
      account,
    }))
    : this.pending().map((summary): AdmissionRow => ({
      kind: 'admission',
      id: summary.dossierId,
      label: dossierReference(summary.dossierId),
      meta: formatDateTime(summary.createdAt),
      version: summary.version,
      summary,
    })));

  protected readonly counts = computed(() => ({ validated: this.accountTotal(), pending: this.pending().length }));

  constructor() {
    this.load();
  }

  protected reload(): void {
    this.clearSelection();
    this.load();
  }

  protected setFilter(filter: ClientFilter): void {
    this.filter.set(filter);
    const selected = this.selected();
    if (selected && !this.rows().some((row) => row.kind === selected.kind && row.id === selected.id)) this.clearSelection();
  }

  protected isSelected(row: ClientRow): boolean {
    const selected = this.selected();
    return selected !== null && selected.kind === row.kind && selected.id === row.id;
  }

  protected select(row: ClientRow): void {
    this.selected.set(row);
    this.resetDetail();
    this.error.set('');
    this.detailLoading.set(true);
    if (row.kind === 'account') {
      forkJoin({
        account: this.api.clientAccount(row.id),
        summary: this.api.clientAccountSummary(row.id),
      }).pipe(
        finalize(() => this.detailLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      ).subscribe({
        next: ({ account, summary }) => {
          this.accountDetail.set(account);
          this.assetSummary.set(summary);
        },
        error: (error: unknown) => this.error.set(directoryFailureMessage(error)),
      });
      return;
    }
    forkJoin({
      detail: this.api.clientDetail(row.id),
      history: this.api.clientHistory(row.id),
    }).pipe(
      finalize(() => this.detailLoading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: ({ detail, history }) => {
        this.admissionDetail.set(detail);
        this.history.set(history);
      },
      error: (error: unknown) => this.error.set(directoryFailureMessage(error)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      pending: forkJoin(PENDING_STATES.map((state) => this.api.clients(state))),
      accounts: this.api.clientAccounts(),
    }).pipe(
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: ({ pending, accounts }) => {
        this.pending.set(sortAdmissions(pending.flat()));
        this.accounts.set(accounts.items);
        this.accountTotal.set(accounts.total);
      },
      error: (error: unknown) => this.error.set(directoryFailureMessage(error)),
    });
  }

  private clearSelection(): void {
    this.selected.set(null);
    this.resetDetail();
  }

  private resetDetail(): void {
    this.accountDetail.set(null);
    this.assetSummary.set(null);
    this.admissionDetail.set(null);
    this.history.set([]);
  }
}
