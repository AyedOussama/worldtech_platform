package fr.entreprise.maintenance.features.tickets.domain;

public final class TicketFailure extends RuntimeException {
    public enum Code { INVALID_TICKET_INPUT, TICKET_NOT_FOUND, TICKET_VERSION_STALE, TICKET_STATE_CONFLICT }
    private final Code code;
    public TicketFailure(Code code) { super("Ticket command refused."); this.code=code; }
    public Code code() { return code; }
    public static TicketFailure invalid() { return new TicketFailure(Code.INVALID_TICKET_INPUT); }
}
