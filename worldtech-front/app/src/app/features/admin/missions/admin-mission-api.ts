import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Observable } from 'rxjs';
import type {
  ClientAccountPage,
  MissionCommandView,
  MissionCreateBody,
  MissionDetailView,
  MissionFinancialBody,
  MissionFinancialHistoryView,
  MissionPageView,
  SitePage,
} from '../../../api/generated/types.gen';
import { RUNTIME_CONFIG } from '../../../core/config/runtime-config';

@Injectable({ providedIn: 'root' })
export class AdminMissionApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;

  accounts(): Observable<ClientAccountPage> {
    return this.http.get<ClientAccountPage>(`${this.base}/admin/client-accounts`, {
      ...this.headers(), params: new HttpParams().set('page', 0).set('size', 100),
    });
  }

  sites(accountId: string): Observable<SitePage> {
    return this.http.get<SitePage>(`${this.base}/admin/client-accounts/${encodeURIComponent(accountId)}/sites`, {
      ...this.headers(), params: new HttpParams().set('page', 0).set('size', 100),
    });
  }

  missions(accountId: string, siteId: string): Observable<MissionPageView> {
    return this.http.get<MissionPageView>(this.missionsUrl(accountId, siteId), {
      ...this.headers(), params: new HttpParams().set('page', 0).set('size', 100),
    });
  }

  detail(accountId: string, siteId: string, missionId: string): Observable<MissionDetailView> {
    return this.http.get<MissionDetailView>(this.missionUrl(accountId, siteId, missionId), this.headers());
  }

  create(accountId: string, siteId: string, body: MissionCreateBody, idempotencyKey: string): Observable<MissionCommandView> {
    return this.http.post<MissionCommandView>(this.missionsUrl(accountId, siteId), body, this.headers({ 'Idempotency-Key': idempotencyKey }));
  }

  publish(accountId: string, siteId: string, missionId: string, version: number, body: MissionFinancialBody, idempotencyKey: string): Observable<MissionCommandView> {
    return this.http.post<MissionCommandView>(`${this.missionUrl(accountId, siteId, missionId)}/publish`, body,
      this.headers(this.commandHeaders(version, idempotencyKey)));
  }

  revise(accountId: string, siteId: string, missionId: string, version: number, body: MissionFinancialBody, idempotencyKey: string): Observable<MissionCommandView> {
    return this.http.post<MissionCommandView>(`${this.missionUrl(accountId, siteId, missionId)}/financial-revisions`, body,
      this.headers(this.commandHeaders(version, idempotencyKey)));
  }

  history(accountId: string, siteId: string, missionId: string): Observable<MissionFinancialHistoryView> {
    return this.http.get<MissionFinancialHistoryView>(`${this.missionUrl(accountId, siteId, missionId)}/financial-revisions`, {
      ...this.headers(), params: new HttpParams().set('after', -1).set('size', 100),
    });
  }

  private missionsUrl(accountId: string, siteId: string): string {
    return `${this.base}/admin/client-accounts/${encodeURIComponent(accountId)}/sites/${encodeURIComponent(siteId)}/missions`;
  }

  private missionUrl(accountId: string, siteId: string, missionId: string): string {
    return `${this.missionsUrl(accountId, siteId)}/${encodeURIComponent(missionId)}`;
  }

  private commandHeaders(version: number, idempotencyKey: string): Record<string, string> {
    return { 'If-Match': `"${version}"`, 'Idempotency-Key': idempotencyKey };
  }

  private headers(extra: Record<string, string> = {}): { headers: Record<string, string>; timeout: number } {
    return { headers: { Accept: 'application/json', ...extra }, timeout: 15_000 };
  }
}
