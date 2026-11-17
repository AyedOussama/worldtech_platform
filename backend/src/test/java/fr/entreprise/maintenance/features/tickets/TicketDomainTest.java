package fr.entreprise.maintenance.features.tickets;

import java.time.Instant;
import java.util.UUID;
import fr.entreprise.maintenance.features.tickets.domain.ClientProposal;
import fr.entreprise.maintenance.features.tickets.domain.Ticket;
import fr.entreprise.maintenance.features.tickets.domain.TicketFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketDomainTest {
    @ParameterizedTest @ValueSource(strings={"-1.00","-0.00","1.234","10000000000.00","1e2","1E+2","NaN","Infinity","1,50"," 2.00 ","01.00","",".50"})
    void invalidMoneyIsRejectedWithoutRoundingOrCoercion(String input) { assertThatThrownBy(()->ClientProposal.parse(input,null)).isInstanceOf(TicketFailure.class); }
    @Test void anAbsentProposalDiffersFromAnExplicitZeroAndTheMaximumIsExact() {
        var absent=ClientProposal.parse(null,null); var zero=ClientProposal.parse("0","0.00");
        assertThat(absent.remuneration()).isNull(); assertThat(zero.remuneration().toPlainString()).isEqualTo("0.00");
        assertThat(ClientProposal.parse("9999999999.99","20.1").expenses().toPlainString()).isEqualTo("20.10");
    }
    @Test void aRemoteResolutionDoesNotRequireAnInterventionAndAClientCanReopenBeforeClosure() {
        assertThat(ticket(Ticket.State.QUALIFIED).transition(Ticket.Action.RESOLVE)).isEqualTo(Ticket.State.RESOLVED);
        assertThat(ticket(Ticket.State.RESOLVED).transition(Ticket.Action.REOPEN)).isEqualTo(Ticket.State.IN_PROGRESS);
        assertThat(ticket(Ticket.State.RESOLVED).transition(Ticket.Action.CLOSE)).isEqualTo(Ticket.State.CLOSED);
        assertThatThrownBy(()->ticket(Ticket.State.OPEN).transition(Ticket.Action.RESOLVE)).isInstanceOf(TicketFailure.class);
    }
    @Test void closedAndCancelledTicketsCannotBeSilentlyReopenedAndProposalsStopAtQualification() {
        for(Ticket.State state:new Ticket.State[]{Ticket.State.CLOSED,Ticket.State.CANCELLED}) {
            for(Ticket.Action action:Ticket.Action.values()) { assertThatThrownBy(()->ticket(state).transition(action)).isInstanceOf(TicketFailure.class); }
            assertThatThrownBy(()->ticket(state).exchangeAllowed()).isInstanceOf(TicketFailure.class);
        }
        assertThatThrownBy(()->ticket(Ticket.State.QUALIFIED).proposalEditable()).isInstanceOf(TicketFailure.class);
        ticket(Ticket.State.OPEN).proposalEditable();
    }
    @Test void transitionsAndOptimisticVersionsDoNotFollowArbitraryStatusPatches() {
        assertThat(ticket(Ticket.State.QUALIFIED).transition(Ticket.Action.WAIT)).isEqualTo(Ticket.State.WAITING_CLIENT);
        assertThat(ticket(Ticket.State.WAITING_CLIENT).transition(Ticket.Action.RESUME)).isEqualTo(Ticket.State.IN_PROGRESS);
        assertThatThrownBy(()->ticket(Ticket.State.OPEN).transition(Ticket.Action.CLOSE)).isInstanceOf(TicketFailure.class);
        assertThatThrownBy(()->ticket(Ticket.State.OPEN).expect(1)).isInstanceOf(TicketFailure.class);
    }
    private static Ticket ticket(Ticket.State state) { return new Ticket(UUID.randomUUID(),UUID.randomUUID(),null,"Europe/Paris","Incident","Problème",Ticket.Priority.HIGH,state==Ticket.State.OPEN?null:Ticket.Priority.NORMAL,state,null,state!=Ticket.State.OPEN,0,0,Instant.EPOCH); }
}
