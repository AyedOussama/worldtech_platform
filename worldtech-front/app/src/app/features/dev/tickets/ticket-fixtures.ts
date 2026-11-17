import { StatusTone } from '../../../shared/ui/status-label';
import { TimelineEvent } from '../../../shared/ui/timeline';

// WT_DEV_TICKETS_ONLY: illustrative data, NEVER an API DTO or a server response.
// Reference, updatedAt, cross-account search and next action are UX proposals, not API 0.6.0 fields.
export type ReferenceState = 'OPEN' | 'QUALIFIED' | 'IN_PROGRESS' | 'WAITING_CLIENT' | 'RESOLVED' | 'CLOSED' | 'CANCELLED';
export type ReferencePriority = 'HIGH' | 'NORMAL' | 'LOW';
export interface ReferenceTicket {
  readonly reference: string;
  readonly title: string;
  readonly account: string;
  readonly site: string;
  readonly priority: ReferencePriority;
  readonly state: ReferenceState;
  readonly updatedAt: string;
  readonly nextAction: string;
  readonly owner: string;
}

export const REFERENCE_STATES: Readonly<Record<ReferenceState, { label: string; tone: StatusTone }>> = {
  OPEN: { label: 'À qualifier', tone: 'neutral' },
  QUALIFIED: { label: 'Qualifié', tone: 'neutral' },
  IN_PROGRESS: { label: 'En cours', tone: 'info' },
  WAITING_CLIENT: { label: 'En attente client', tone: 'warning' },
  RESOLVED: { label: 'Résolu', tone: 'success' },
  CLOSED: { label: 'Clos', tone: 'neutral' },
  CANCELLED: { label: 'Annulé', tone: 'neutral' },
};
export const REFERENCE_PRIORITIES: Readonly<Record<ReferencePriority, string>> = { HIGH: 'Haute', NORMAL: 'Normale', LOW: 'Basse' };
export const REFERENCE_SITES = ['Lyon · Part-Dieu', 'Paris · République', 'Nantes · Île de Nantes'] as const;

export const REFERENCE_TICKETS: readonly ReferenceTicket[] = [
  { reference: 'DEMO-2048', title: 'Connexion réseau indisponible à l’accueil', account: 'Atelier Boréal', site: REFERENCE_SITES[0], priority: 'HIGH', state: 'OPEN', updatedAt: '2026-11-10T09:42:00+01:00', nextAction: 'Qualifier la demande', owner: 'Administration' },
  { reference: 'DEMO-2047', title: 'Imprimante du deuxième étage hors ligne', account: 'Atelier Boréal', site: REFERENCE_SITES[1], priority: 'NORMAL', state: 'IN_PROGRESS', updatedAt: '2026-11-10T09:25:00+01:00', nextAction: 'Poursuivre le diagnostic', owner: 'Administration' },
  { reference: 'DEMO-2046', title: 'Écran de la salle de réunion sans signal', account: 'Comptoir Azur', site: REFERENCE_SITES[2], priority: 'NORMAL', state: 'WAITING_CLIENT', updatedAt: '2026-11-10T08:58:00+01:00', nextAction: 'Préciser les disponibilités sur site', owner: 'Client' },
  { reference: 'DEMO-2045', title: 'Poste de travail lent au démarrage', account: 'Atelier Boréal', site: REFERENCE_SITES[0], priority: 'LOW', state: 'QUALIFIED', updatedAt: '2026-11-10T08:30:00+01:00', nextAction: 'Préparer la suite de la prise en charge', owner: 'Administration' },
  { reference: 'DEMO-2044', title: 'Accès au dossier partagé interrompu', account: 'Atelier Boréal', site: REFERENCE_SITES[1], priority: 'HIGH', state: 'WAITING_CLIENT', updatedAt: '2026-11-09T17:15:00+01:00', nextAction: 'Fournir le message d’erreur', owner: 'Client' },
  { reference: 'DEMO-2043', title: 'Remplacement du clavier de l’accueil', account: 'Comptoir Azur', site: REFERENCE_SITES[2], priority: 'LOW', state: 'RESOLVED', updatedAt: '2026-11-09T16:40:00+01:00', nextAction: 'Confirmer la résolution', owner: 'Client' },
  { reference: 'DEMO-2042', title: 'Point d’accès Wi-Fi du premier étage instable', account: 'Atelier Boréal', site: REFERENCE_SITES[0], priority: 'HIGH', state: 'IN_PROGRESS', updatedAt: '2026-11-09T15:20:00+01:00', nextAction: 'Poursuivre le diagnostic', owner: 'Administration' },
  { reference: 'DEMO-2041', title: 'Mise à jour du poste de la comptabilité', account: 'Atelier Boréal', site: REFERENCE_SITES[1], priority: 'NORMAL', state: 'CLOSED', updatedAt: '2026-11-09T14:00:00+01:00', nextAction: 'Aucune action attendue', owner: 'Dossier clos' },
  { reference: 'DEMO-2040', title: 'Téléphone de bureau non enregistré', account: 'Comptoir Azur', site: REFERENCE_SITES[2], priority: 'NORMAL', state: 'OPEN', updatedAt: '2026-11-09T11:35:00+01:00', nextAction: 'Qualifier la demande', owner: 'Administration' },
  { reference: 'DEMO-2039', title: 'Installation d’un écran supplémentaire', account: 'Atelier Boréal', site: REFERENCE_SITES[0], priority: 'LOW', state: 'CANCELLED', updatedAt: '2026-11-08T15:10:00+01:00', nextAction: 'Aucune action attendue', owner: 'Demande annulée' },
];

export const REFERENCE_TIMELINE: readonly TimelineEvent[] = [
  { id: 'reading', at: '2026-11-10T09:42:00+01:00', label: 'Demande reçue, en attente de qualification', actor: 'Administration' },
  { id: 'opened', at: '2026-11-10T09:30:00+01:00', label: 'Incident déclaré sur le site', actor: 'Client' },
];
