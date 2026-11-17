import { ChangeDetectionStrategy, Component, Injector, afterNextRender, effect, inject, input, output, signal, viewChild } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ErrorSummary, FieldIssue } from '../ui/error-summary';

export interface IncidentFormValue {
  readonly site: string;
  readonly title: string;
  readonly description: string;
  readonly requestedPriority: string;
  readonly remuneration: string | null;
}

function trimmedRequired(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim().length ? null : { required: true };
}

/** Presentational typed form: the caller supplies sites and handles the submission. No HTTP. */
@Component({
  selector: 'wt-incident-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, ErrorSummary],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './incident-form.html',
  styleUrl: './incident-form.scss',
})
export class IncidentForm {
  readonly formId = input.required<string>();
  readonly sites = input.required<readonly string[]>();
  readonly busy = input(false);
  readonly validated = output<IncidentFormValue>();
  readonly edited = output<void>();
  readonly form = new FormGroup({
    site: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    title: new FormControl('', { nonNullable: true, validators: [trimmedRequired, Validators.maxLength(160)] }),
    description: new FormControl('', { nonNullable: true, validators: [trimmedRequired, Validators.maxLength(2000)] }),
    requestedPriority: new FormControl('NORMAL', { nonNullable: true, validators: [Validators.required] }),
    remuneration: new FormControl('', { nonNullable: true, validators: [Validators.pattern(/^(0|[1-9][0-9]*)([.,][0-9]{1,2})?$/), Validators.maxLength(40)] }),
  });
  protected readonly issues = signal<readonly FieldIssue[]>([]);
  private readonly summary = viewChild(ErrorSummary);
  private readonly injector = inject(Injector);

  constructor() {
    effect(() => {
      if (this.busy()) this.form.disable({ emitEvent: false });
      else this.form.enable({ emitEvent: false });
    });
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.edited.emit();
      if (this.issues().length) this.updateIssues();
    });
  }

  protected validate(): void {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    // Do not trust arbitrary strings written into a select programmatically.
    const value = this.form.getRawValue();
    if (!this.sites().includes(value.site)) this.form.controls.site.setErrors({ unavailable: true });
    if (!['HIGH', 'NORMAL', 'LOW'].includes(value.requestedPriority)) this.form.controls.requestedPriority.setErrors({ unavailable: true });
    this.updateIssues();
    if (this.form.invalid) {
      afterNextRender(() => this.summary()?.focus(), { injector: this.injector });
      return;
    }
    this.validated.emit({ ...value, title: value.title.trim(), description: value.description.trim(),
      remuneration: value.remuneration === '' ? null : value.remuneration.replace(',', '.'),
    });
  }

  reset(): void {
    this.form.reset({ site: '', title: '', description: '', requestedPriority: 'NORMAL', remuneration: '' });
    this.issues.set([]);
  }

  private updateIssues(): void {
    const fields = [
      ['site', 'Choisissez un site'], ['title', 'Renseignez un objet de 160 caractères maximum'],
      ['description', 'Renseignez une description de 2 000 caractères maximum'],
      ['requestedPriority', 'Choisissez une priorité'], ['remuneration', 'Indiquez un montant positif ou nul, avec deux décimales maximum'],
    ] as const;
    this.issues.set(fields.filter(([key]) => this.form.controls[key].invalid)
      .map(([key, message]) => ({ targetId: `${this.formId()}-${key}`, message })));
  }
}
