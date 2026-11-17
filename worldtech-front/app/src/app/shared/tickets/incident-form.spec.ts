import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { IncidentForm } from './incident-form';

async function setup() {
  TestBed.configureTestingModule({ imports: [IncidentForm] });
  const fixture = TestBed.createComponent(IncidentForm);
  fixture.componentRef.setInput('formId', 'test-incident');
  fixture.componentRef.setInput('sites', ['Site fictif']);
  await fixture.whenStable();
  const emitted = vi.fn();
  fixture.componentInstance.validated.subscribe(emitted);
  const submit = () => (fixture.nativeElement as HTMLElement).querySelector('form')?.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  return { fixture, form: fixture.componentInstance.form, submit, emitted };
}

describe('incident form presentation boundary', () => {
  it('refuses empty and whitespace-only fields, focuses useful error links, never emits a command', async () => {
    const { fixture, form, submit, emitted } = await setup();
    form.controls.title.setValue('   ');
    submit();
    await fixture.whenStable();
    expect(emitted).not.toHaveBeenCalled();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('wt-error-summary')?.textContent).toContain('Choisissez un site');
    expect(root.querySelector('a[href="#test-incident-title"]')).not.toBeNull();
  });

  it('emits trimmed values and exact decimal strings, distinguishing zero from no proposal', async () => {
    const { form, submit, emitted } = await setup();
    form.setValue({ site: 'Site fictif', title: ' Objet français ', description: ' Diagnostic à préciser ', requestedPriority: 'HIGH', remuneration: '9007199254740993,01' });
    submit();
    expect(emitted).toHaveBeenLastCalledWith({ site: 'Site fictif', title: 'Objet français', description: 'Diagnostic à préciser', requestedPriority: 'HIGH', remuneration: '9007199254740993.01' });
    form.controls.remuneration.setValue('0'); submit();
    expect(emitted.mock.lastCall?.[0].remuneration).toBe('0');
    form.controls.remuneration.setValue(''); submit();
    expect(emitted.mock.lastCall?.[0].remuneration).toBeNull();
  });

  it('rejects a site outside the supplied list and malformed money', async () => {
    const { form, submit, emitted } = await setup();
    form.setValue({ site: 'Autre site', title: 'Objet', description: 'Texte', requestedPriority: 'NORMAL', remuneration: '12.345' });
    submit();
    expect(emitted).not.toHaveBeenCalled();
    expect(form.controls.site.invalid).toBe(true);
    expect(form.controls.remuneration.invalid).toBe(true);
  });

  it('keeps all values while pending and suppresses another submit until the caller releases busy', async () => {
    const { fixture, form, submit, emitted } = await setup();
    form.setValue({ site: 'Site fictif', title: 'Objet', description: 'Texte', requestedPriority: 'NORMAL', remuneration: '120.50' });
    const before = form.getRawValue();
    fixture.componentRef.setInput('busy', true);
    await fixture.whenStable(); submit();
    expect(emitted).not.toHaveBeenCalled(); expect(form.disabled).toBe(true);
    expect(form.getRawValue()).toEqual(before);
    fixture.componentRef.setInput('busy', false);
    await fixture.whenStable(); submit();
    expect(form.enabled).toBe(true); expect(emitted).toHaveBeenCalledOnce();
  });
});
