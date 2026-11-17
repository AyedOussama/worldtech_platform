package fr.entreprise.maintenance.features.tickets.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Ticket(UUID ticketId, UUID siteId, UUID equipmentId, String siteTimeZone, String title, String description,
        Priority requestedPriority, Priority retainedPriority, State state, UUID contractRevisionId, boolean qualified,
        long proposalNumber, long version, Instant createdAt) {
    public enum Priority { LOW, NORMAL, HIGH, CRITICAL }
    public enum State { OPEN, QUALIFIED, IN_PROGRESS, WAITING_CLIENT, RESOLVED, CLOSED, CANCELLED }
    public enum Action { QUALIFY, START, WAIT, RESUME, RESOLVE, CLOSE, REOPEN, CANCEL }
    public void expect(long expected) { if(expected!=version) { throw new TicketFailure(TicketFailure.Code.TICKET_VERSION_STALE); } }
    public State transition(Action action) {
        return switch(action) {
            case QUALIFY -> { require(State.OPEN); yield State.QUALIFIED; }
            case START -> { require(State.QUALIFIED); yield State.IN_PROGRESS; }
            case WAIT -> { require(State.QUALIFIED,State.IN_PROGRESS); yield State.WAITING_CLIENT; }
            case RESUME -> { require(State.WAITING_CLIENT); yield State.IN_PROGRESS; }
            case RESOLVE -> { require(State.QUALIFIED,State.IN_PROGRESS,State.WAITING_CLIENT); yield State.RESOLVED; }
            case CLOSE -> { require(State.RESOLVED); yield State.CLOSED; }
            case REOPEN -> { require(State.RESOLVED); yield State.IN_PROGRESS; }
            case CANCEL -> { require(State.OPEN,State.QUALIFIED,State.IN_PROGRESS,State.WAITING_CLIENT,State.RESOLVED); yield State.CANCELLED; }
        };
    }
    public void proposalEditable() { require(State.OPEN); }
    public void exchangeAllowed() { if(state==State.CLOSED || state==State.CANCELLED) { conflict(); } }
    private void require(State... states) { if(!Set.of(states).contains(state)) { conflict(); } }
    private static void conflict() { throw new TicketFailure(TicketFailure.Code.TICKET_STATE_CONFLICT); }
    public static String text(String value,int max) {
        if(value==null || value.isBlank() || value.length()>max || value.chars().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\t')) { throw TicketFailure.invalid(); }
        return value.strip();
    }
    @Override public String toString() { return "Ticket[redacted]"; }
}
