import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { LucideRefreshCw } from '@lucide/angular';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import type { AdminAdmissionHistoryEvent, AdmissionSummary } from '../../../api/generated/types.gen';
import { EmptyState } from '../../../shared/ui/empty-state';
import { PageHeader } from '../../../shared/ui/page-header';
import { StatusLabel } from '../../../shared/ui/status-label';
import {
  PENDING_STATES,
  admissionActionLabel,
  admissionStateLabel,
  admissionStateTone,
  directoryFailureMessage,
  documentsLabel,
  dossierReference,
  formatDateTime,
  radiusLabel,
  sortAdmissions,
  technicianDisplayName,
} from '../directory/admin-directory';
import { AdminDirectoryApi, type TechnicianAdmissionDetail } from '../directory/admin-directory-api';

type TechnicianFilter = 'PENDING' | 'APPROVED';

@Component({
  selector: 'wt-admin-technicians-page',
  imports: [MatButtonModule, PageHeader, EmptyState, StatusLabel, LucideRefreshCw],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-technicians-page.html',
})
export class AdminTechniciansPage {
  private readonly api = inject(AdminDirectoryApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly filter = signal<TechnicianFilter>('PENDING');
  protected readonly pending = signal<readonly AdmissionSummary[]>([]);
  protected readonly approved = signal<readonly AdmissionSummary[]>([]);
  protected readonly selectedId = signal('');
  protected readonly detail = signal<TechnicianAdmissionDetail | null>(null);
  protected readonly history = signal<readonly AdminAdmissionHistoryEvent[]>([]);
  protected readonly loading = signal(false);
  protected readonly detailLoading = signal(false);
  protected readonly error = signal('');

  protected readonly formatDateTime = formatDateTime;
  protected readonly dossierReference = dossierReference;
  protected readonly stateLabel = admissionStateLabel;
  protected readonly stateTone = admissionStateTone;
  protected readonly actionLabel = admissionActionLabel;
  protected readonly technicianName = technicianDisplayName;
  protected readonly documentsLabel = documentsLabel;
  protected readonly radiusLabel = radiusLabel;

  protected readonly list = computed(() => this.filter() === 'PENDING' ? this.pending() : this.approved());
  protected readonly counts = computed(() => ({ pending: this.pending().length, approved: this.approved().length }));

  constructor() {
    this.load();
  }

  protected reload(): void {
    this.clearSelection();
    this.load();
  }

  protected setFilter(filter: TechnicianFilter): void {
    this.filter.set(filter);
    const selected = this.selectedId();
    if (selected && !this.list().some((item) => item.dossierId === selected)) this.clearSelection();
  }

  protected select(item: AdmissionSummary): void {
    this.selectedId.set(item.dossierId);
    this.detail.set(null);
    this.history.set([]);
    this.error.set('');
    this.detailLoading.set(true);
    forkJoin({
      detail: this.api.technicianDetail(item.dossierId),
      history: this.api.technicianHistory(item.dossierId),
    }).pipe(
      finalize(() => this.detailLoading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: ({ detail, history }) => {
        this.detail.set(detail);
        this.history.set(history);
      },
      error: (error: unknown) => this.error.set(directoryFailureMessage(error)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      pending: forkJoin(PENDING_STATES.map((state) => this.api.technicians(state))),
      approved: this.api.technicians('APPROVED'),
    }).pipe(
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: ({ pending, approved }) => {
        this.pending.set(sortAdmissions(pending.flat()));
        this.approved.set(sortAdmissions(approved));
      },
      error: (error: unknown) => this.error.set(directoryFailureMessage(error)),
    });
  }

  private clearSelection(): void {
    this.selectedId.set('');
    this.detail.set(null);
    this.history.set([]);
  }
}
