import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** String formatting only: never convert API decimal strings to floating point. */
export function formatEuro(value: string | null): string {
  if (value === null) return 'Non renseigné';
  if (!/^(0|[1-9][0-9]*)(\.[0-9]{1,2})?$/.test(value)) return 'Montant invalide';
  const [integer = '', decimals = ''] = value.split('.');
  return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, '\u202f')},${decimals.padEnd(2, '0')}\u00a0€`;
}

@Component({
  selector: 'wt-money-display',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<span class="tabular">{{ formatted() }}</span>',
})
export class MoneyDisplay {
  readonly value = input.required<string | null>();
  protected readonly formatted = computed(() => formatEuro(this.value()));
}
