import { describe, expect, it } from 'vitest';
import { isNetworkFailure, missionFailureMessage } from './mission-feedback';
import {
  buildMissionCreateBody,
  buildMissionFinancialBody,
  localDateTime,
  splitSkills,
  validateMissionDraft,
  validateMissionFinancial,
} from './mission-form';

function validDraft() {
  return {
    title: ' Maintenance réseau ',
    publicSummary: ' Intervention préventive. ',
    skills: 'Réseau, Windows Server, Réseau',
    windowStart: localDateTime(60 * 60 * 1000),
    windowEnd: localDateTime(2 * 60 * 60 * 1000),
    publicLatitude: '48.856600',
    publicLongitude: '2.352200',
    publicLabel: ' Paris centre ',
  };
}

describe('mission draft form boundary', () => {
  it('trims text, deduplicates skills, keeps decimals as numbers and clears a blank label', () => {
    const input = validDraft();
    expect(validateMissionDraft(input)).toEqual([]);
    const body = buildMissionCreateBody({ ...input, publicLabel: '   ' });
    expect(body.title).toBe('Maintenance réseau');
    expect(body.publicSummary).toBe('Intervention préventive.');
    expect(body.skills).toEqual(['Réseau', 'Windows Server']);
    expect(body.publicLatitude).toBe(48.8566);
    expect(body.publicLongitude).toBe(2.3522);
    expect(body.publicLabel).toBeNull();
    expect(new Date(body.windowStart).getTime()).toBeLessThan(new Date(body.windowEnd).getTime());
  });

  it('refuses an expired window, invalid coordinates and too many skills', () => {
    const issues = validateMissionDraft({
      ...validDraft(),
      windowStart: '2020-01-01T10:00',
      windowEnd: '2020-01-01T11:00',
      publicLatitude: '48.1234567',
      publicLongitude: '181',
      skills: 'a,b,c,d,e,f,g,h,i,j,k',
    });
    expect(issues.some((issue) => issue.targetId === 'mission-window-end')).toBe(true);
    expect(issues.some((issue) => issue.targetId === 'mission-latitude')).toBe(true);
    expect(issues.some((issue) => issue.targetId === 'mission-longitude')).toBe(true);
    expect(issues.some((issue) => issue.targetId === 'mission-skills')).toBe(true);
  });

  it('keeps the API decimal string exact and distinguishes absent expenses from zero', () => {
    expect(validateMissionFinancial({ remuneration: '9007199254740993,01', expenses: '', reason: '' }).length).toBe(1);
    expect(validateMissionFinancial({ remuneration: '180.00', expenses: '0', reason: '' })).toEqual([]);
    expect(buildMissionFinancialBody({ remuneration: '180.00', expenses: ' 0 ', reason: ' Ajustement ' }))
      .toEqual({ remuneration: '180.00', expenses: '0', reason: 'Ajustement' });
    expect(buildMissionFinancialBody({ remuneration: '180.00', expenses: '', reason: '' }))
      .toEqual({ remuneration: '180.00', expenses: null, reason: null });
  });

  it('rejects malformed or excessive money before any command', () => {
    for (const value of ['12.345', '-1', '10000000000', '01.00', 'abc']) {
      expect(validateMissionFinancial({ remuneration: value, expenses: '', reason: '' }).length, value).toBe(1);
    }
    expect(validateMissionFinancial({ remuneration: '9999999999.99', expenses: '0.00', reason: '' })).toEqual([]);
  });

  it('splits and trims comma-separated skills without inventing entries', () => {
    expect(splitSkills(' Réseau ,, Windows , ')).toEqual(['Réseau', 'Windows']);
    expect(splitSkills('')).toEqual([]);
  });
});

describe('mission feedback', () => {
  it('turns a stale version into an explicit retry instruction', () => {
    expect(missionFailureMessage({ kind: 'http', status: 412 })).toContain('changé depuis sa lecture');
    expect(missionFailureMessage({ kind: 'http', status: 403 })).toContain('ADMIN');
  });

  it('does not claim success after a network failure and keeps the idempotency key', () => {
    const error = { kind: 'network', status: 0 };
    expect(isNetworkFailure(error)).toBe(true);
    expect(missionFailureMessage(error)).toContain('n’est pas confirmé');
    expect(isNetworkFailure({ kind: 'http', status: 409 })).toBe(false);
  });
});
