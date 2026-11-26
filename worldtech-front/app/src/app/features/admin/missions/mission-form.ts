import type { MissionCreateBody, MissionFinancialBody } from '../../../api/generated/types.gen';
import type { FieldIssue } from '../../../shared/ui/error-summary';

export interface MissionDraftInput {
  readonly title: string;
  readonly publicSummary: string;
  readonly skills: string;
  readonly windowStart: string;
  readonly windowEnd: string;
  readonly publicLatitude: string;
  readonly publicLongitude: string;
  readonly publicLabel: string;
}

export interface MissionFinancialInput {
  readonly remuneration: string;
  readonly expenses: string;
  readonly reason: string;
}

const money = /^(0|[1-9][0-9]*)(\.[0-9]{1,2})?$/;
const coordinate = /^-?(0|[1-9][0-9]*)(\.[0-9]{1,6})?$/;
const maxMoney = 9_999_999_999.99;

export function splitSkills(value: string): string[] {
  const skills = new Set<string>();
  for (const part of value.split(',')) {
    const skill = part.trim();
    if (skill) skills.add(skill);
  }
  return [...skills];
}

export function localDateTime(offsetMilliseconds: number): string {
  const date = new Date(Date.now() + offsetMilliseconds);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function parsedDate(value: string): Date | null {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function coordinateIssue(value: string, targetId: string, label: string, minimum: number, maximum: number): FieldIssue | null {
  const text = value.trim();
  if (!coordinate.test(text)) return { targetId, message: `${label} doit être un nombre décimal avec au plus six décimales.` };
  const number = Number(text);
  return number >= minimum && number <= maximum ? null : { targetId, message: `${label} doit être compris entre ${minimum} et ${maximum}.` };
}

function moneyIssue(value: string, targetId: string, label: string, required: boolean): FieldIssue | null {
  const text = value.trim();
  if (!text) return required ? { targetId, message: `${label} est obligatoire.` } : null;
  if (!money.test(text) || Number(text) > maxMoney) {
    return { targetId, message: `${label} doit être un montant HT positif avec au plus deux décimales.` };
  }
  return null;
}

export function validateMissionDraft(input: MissionDraftInput): FieldIssue[] {
  const issues: FieldIssue[] = [];
  if (!input.title.trim()) issues.push({ targetId: 'mission-title', message: 'Le titre est obligatoire.' });
  else if (input.title.trim().length > 160) issues.push({ targetId: 'mission-title', message: 'Le titre ne peut pas dépasser 160 caractères.' });
  if (!input.publicSummary.trim()) issues.push({ targetId: 'mission-summary', message: 'Le résumé public est obligatoire.' });
  else if (input.publicSummary.trim().length > 2000) issues.push({ targetId: 'mission-summary', message: 'Le résumé public ne peut pas dépasser 2 000 caractères.' });
  const skills = splitSkills(input.skills);
  if (skills.length < 1 || skills.length > 10) issues.push({ targetId: 'mission-skills', message: 'Ajoutez entre 1 et 10 compétences séparées par des virgules.' });
  else if (skills.some((skill) => skill.length > 80)) issues.push({ targetId: 'mission-skills', message: 'Chaque compétence est limitée à 80 caractères.' });
  const start = parsedDate(input.windowStart);
  const end = parsedDate(input.windowEnd);
  if (!start) issues.push({ targetId: 'mission-window-start', message: 'Renseignez le début de la fenêtre.' });
  if (!end) issues.push({ targetId: 'mission-window-end', message: 'Renseignez la fin de la fenêtre.' });
  if (start && end && end <= start) issues.push({ targetId: 'mission-window-end', message: 'La fin doit être postérieure au début.' });
  if (end && end.getTime() <= Date.now()) issues.push({ targetId: 'mission-window-end', message: 'La fin de la fenêtre doit être future.' });
  const latitude = coordinateIssue(input.publicLatitude, 'mission-latitude', 'La latitude', -90, 90);
  const longitude = coordinateIssue(input.publicLongitude, 'mission-longitude', 'La longitude', -180, 180);
  if (latitude) issues.push(latitude);
  if (longitude) issues.push(longitude);
  if (input.publicLabel.trim().length > 200) issues.push({ targetId: 'mission-label', message: 'Le libellé public ne peut pas dépasser 200 caractères.' });
  return issues;
}

export function validateMissionFinancial(input: MissionFinancialInput): FieldIssue[] {
  const issues: FieldIssue[] = [];
  const remuneration = moneyIssue(input.remuneration, 'mission-remuneration', 'La rémunération', true);
  const expenses = moneyIssue(input.expenses, 'mission-expenses', 'Les frais', false);
  if (remuneration) issues.push(remuneration);
  if (expenses) issues.push(expenses);
  if (input.reason.trim().length > 2000) issues.push({ targetId: 'mission-reason', message: 'Le motif ne peut pas dépasser 2 000 caractères.' });
  return issues;
}

export function buildMissionCreateBody(input: MissionDraftInput): MissionCreateBody {
  const start = parsedDate(input.windowStart);
  const end = parsedDate(input.windowEnd);
  return {
    title: input.title.trim(),
    publicSummary: input.publicSummary.trim(),
    skills: splitSkills(input.skills),
    windowStart: start?.toISOString() ?? '',
    windowEnd: end?.toISOString() ?? '',
    publicLatitude: Number(input.publicLatitude.trim()),
    publicLongitude: Number(input.publicLongitude.trim()),
    publicLabel: input.publicLabel.trim() || null,
  };
}

export function buildMissionFinancialBody(input: MissionFinancialInput): MissionFinancialBody {
  return {
    remuneration: input.remuneration.trim(),
    expenses: input.expenses.trim() || null,
    reason: input.reason.trim() || null,
  };
}

export function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}

export function formatWindow(startValue: string, endValue: string): string {
  const start = new Date(startValue);
  const end = new Date(endValue);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return `${startValue} → ${endValue}`;
  const day = new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short' });
  const time = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' });
  if (start.toDateString() === end.toDateString()) return `${day.format(start)} · ${time.format(start)} → ${time.format(end)}`;
  return `${day.format(start)} ${time.format(start)} → ${day.format(end)} ${time.format(end)}`;
}
