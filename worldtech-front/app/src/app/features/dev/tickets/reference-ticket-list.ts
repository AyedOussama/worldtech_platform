import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { LucideArrowUp } from '@lucide/angular';
import { StatusLabel } from '../../../shared/ui/status-label';
import { formatParisDate } from '../../../shared/ui/timeline';
import { REFERENCE_PRIORITIES, REFERENCE_STATES, ReferenceTicket } from './ticket-fixtures';

/** Read-only reference list; contains no navigation, API calls or authorization. */
@Component({
  selector: 'wt-reference-ticket-list',
  imports: [LowerCasePipe, LucideArrowUp, StatusLabel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reference-ticket-list.html',
  styleUrl: './reference-ticket-list.scss',
})
export class ReferenceTicketList {
  readonly tickets = input.required<readonly ReferenceTicket[]>();
  readonly showAccount = input(false);
  readonly lastOpened = input<string | null>(null);
  readonly ticketOpened = output<ReferenceTicket>();
  protected readonly states = REFERENCE_STATES;
  protected readonly priorities = REFERENCE_PRIORITIES;
  protected readonly formatDate = formatParisDate;
}
