import type { ClientProfileBody, DossierState, TechnicianProfileBody } from '../../../api/generated/types.gen';
import type { ApiFailure } from '../../../core/http/api-failure';
import type { StatusTone } from '../../../shared/ui/status-label';

export const PENDING_STATES: readonly DossierState[] = ['SUBMITTED', 'NEEDS_INFORMATION'];

const DATE_FORMAT = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });

const ACTION_LABELS: Record<string, string> = {
  CREATED: 'Dossier créé',
  DRAFT_SAVED: 'Brouillon enregistré',
  SUBMITTED: 'Dossier soumis',
  NEEDS_INFORMATION: 'Complément demandé',
  APPROVED: 'Dossier validé',
  REJECTED: 'Dossier refusé',
};

export function admissionStateLabel(state: DossierState): string {
  switch (state) {
    case 'DRAFT': return 'Brouillon';
    case 'SUBMITTED': return 'En attente';
    case 'NEEDS_INFORMATION': return 'Complément demandé';
    case 'APPROVED': return 'Validé';
    case 'REJECTED': return 'Refusé';
  }
}

export function admissionStateTone(state: DossierState): StatusTone {
  switch (state) {
    case 'SUBMITTED': return 'info';
    case 'NEEDS_INFORMATION': return 'warning';
    case 'APPROVED': return 'success';
    case 'REJECTED': return 'error';
    case 'DRAFT': return 'neutral';
  }
}

export function admissionActionLabel(action: string): string {
  return ACTION_LABELS[action] ?? action;
}

export function formatDateTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : DATE_FORMAT.format(date);
}

export function dossierReference(id: string): string {
  return `Dossier ${id.slice(0, 8)}…`;
}

export function sortAdmissions<T extends { readonly createdAt: string; readonly dossierId: string }>(items: readonly T[]): T[] {
  return [...items].sort((left, right) =>
    left.createdAt.localeCompare(right.createdAt) || left.dossierId.localeCompare(right.dossierId));
}

export function technicianDisplayName(profile: TechnicianProfileBody): string {
  return [profile.firstName, profile.lastName].filter((part): part is string => Boolean(part?.trim())).join(' ') || 'Technicien sans nom';
}

export function clientDisplayName(profile: ClientProfileBody): string {
  return profile.organizationName?.trim()
    || [profile.firstName, profile.lastName].filter((part): part is string => Boolean(part?.trim())).join(' ')
    || 'Client sans nom';
}

export function clientAccountTypeLabel(type: 'BUSINESS' | 'INDIVIDUAL'): string {
  return type === 'BUSINESS' ? 'Entreprise' : 'Particulier';
}

export function clientProfileTypeLabel(type: 'BUSINESS' | 'INDIVIDUAL' | null | undefined): string {
  return type ? clientAccountTypeLabel(type) : 'Non renseigné';
}

export function radiusLabel(radius: number | null | undefined): string {
  return radius == null ? 'Non renseigné' : `${radius} km`;
}

export function documentsLabel(status: string): string {
  return status === 'REQUIRED_NOT_AVAILABLE' ? 'Non disponibles dans ce lot' : status;
}

function failureOf(error: unknown): Partial<ApiFailure> | null {
  return typeof error === 'object' && error !== null ? error as Partial<ApiFailure> : null;
}

export function directoryFailureMessage(error: unknown): string {
  switch (failureOf(error)?.status) {
    case 0: return 'Le service est injoignable. Vérifiez la connexion puis réessayez.';
    case 401: return 'La session a expiré. Reconnectez-vous.';
    case 403: return 'Accès refusé. La session ADMIN ou sa preuve MFA n’est plus valide.';
    case 404: return 'Le dossier demandé est introuvable.';
    case 429: return 'Trop de requêtes. Attendez avant de réessayer.';
    default: return 'Les données sont temporairement indisponibles. Réessayez.';
  }
}
