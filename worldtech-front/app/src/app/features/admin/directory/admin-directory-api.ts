import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Observable } from 'rxjs';
import type {
  AdminAdmissionHistoryEvent,
  AdmissionSummary,
  AssetSummary,
  ClientAccountPage,
  ClientAccountView,
  DossierState,
  GetApiWebV1AdminOnboardingsClientsByIdResponse,
  GetApiWebV1AdminOnboardingsTechniciansByIdResponse,
} from '../../../api/generated/types.gen';
import { RUNTIME_CONFIG } from '../../../core/config/runtime-config';

export type TechnicianAdmissionDetail = GetApiWebV1AdminOnboardingsTechniciansByIdResponse;
export type ClientAdmissionDetail = GetApiWebV1AdminOnboardingsClientsByIdResponse;
export type AdmissionKind = 'technicians' | 'clients';

@Injectable({ providedIn: 'root' })
export class AdminDirectoryApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;

  technicians(state: DossierState): Observable<readonly AdmissionSummary[]> {
    return this.admissions('technicians', state);
  }

  clients(state: DossierState): Observable<readonly AdmissionSummary[]> {
    return this.admissions('clients', state);
  }

  technicianDetail(id: string): Observable<TechnicianAdmissionDetail> {
    return this.http.get<TechnicianAdmissionDetail>(this.admissionUrl('technicians', id), this.headers());
  }

  clientDetail(id: string): Observable<ClientAdmissionDetail> {
    return this.http.get<ClientAdmissionDetail>(this.admissionUrl('clients', id), this.headers());
  }

  technicianHistory(id: string): Observable<readonly AdminAdmissionHistoryEvent[]> {
    return this.history('technicians', id);
  }

  clientHistory(id: string): Observable<readonly AdminAdmissionHistoryEvent[]> {
    return this.history('clients', id);
  }

  clientAccounts(): Observable<ClientAccountPage> {
    return this.http.get<ClientAccountPage>(`${this.base}/admin/client-accounts`, {
      ...this.headers(), params: new HttpParams().set('page', 0).set('size', 100),
    });
  }

  clientAccount(accountId: string): Observable<ClientAccountView> {
    return this.http.get<ClientAccountView>(`${this.base}/admin/client-accounts/${encodeURIComponent(accountId)}`, this.headers());
  }

  clientAccountSummary(accountId: string): Observable<AssetSummary> {
    return this.http.get<AssetSummary>(`${this.base}/admin/client-accounts/${encodeURIComponent(accountId)}/summary`, this.headers());
  }

  private admissions(kind: AdmissionKind, state: DossierState): Observable<readonly AdmissionSummary[]> {
    return this.http.get<readonly AdmissionSummary[]>(`${this.base}/admin/onboardings/${kind}`, {
      ...this.headers(),
      params: new HttpParams().set('page', 0).set('size', 100).set('state', state),
    });
  }

  private history(kind: AdmissionKind, id: string): Observable<readonly AdminAdmissionHistoryEvent[]> {
    return this.http.get<readonly AdminAdmissionHistoryEvent[]>(`${this.admissionUrl(kind, id)}/history`, {
      ...this.headers(), params: new HttpParams().set('after', -1).set('size', 100),
    });
  }

  private admissionUrl(kind: AdmissionKind, id: string): string {
    return `${this.base}/admin/onboardings/${kind}/${encodeURIComponent(id)}`;
  }

  private headers(): { headers: Record<string, string>; timeout: number } {
    return { headers: { Accept: 'application/json' }, timeout: 15_000 };
  }
}
