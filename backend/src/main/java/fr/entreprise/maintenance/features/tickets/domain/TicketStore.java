package fr.entreprise.maintenance.features.tickets.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TicketStore {
    record Proposal(long number, ClientProposal amounts, UUID authorUserId, Instant createdAt) { }
    record Change(long version, String action, Ticket.State state, UUID actorUserId, String publicReason, Instant occurredAt) {
        @Override public String toString() { return "Change[redacted]"; }
    }
    record Message(long number, UUID messageId, UUID authorUserId, String text, Instant createdAt) {
        @Override public String toString() { return "Message[redacted]"; }
    }
    Ticket create(UUID account, UUID site, UUID equipment, String timeZone, String title, String description,
            Ticket.Priority requested, ClientProposal proposal, UUID actor, Instant now);
    Ticket lock(UUID account, UUID site, UUID ticket);
    List<Ticket> list(UUID account, UUID site, Ticket.State state, Ticket.Priority requested, UUID equipment, int offset, int size);
    long count(UUID account, UUID site, Ticket.State state, Ticket.Priority requested, UUID equipment);
    Ticket propose(UUID account, Ticket ticket, ClientProposal proposal, UUID actor, Instant now);
    Ticket transition(UUID account, Ticket ticket, Ticket.Action action, Ticket.State next, Ticket.Priority retained,
            UUID contractRevision, String reason, UUID actor, Instant now);
    List<Proposal> proposals(UUID account, UUID site, UUID ticket, long after, int size);
    List<Change> history(UUID account, UUID site, UUID ticket, long after, int size);
    Message message(UUID account, Ticket ticket, boolean internal, String text, UUID actor, Instant now);
    List<Message> messages(UUID account, UUID site, UUID ticket, boolean internal, long after, int size);
}
