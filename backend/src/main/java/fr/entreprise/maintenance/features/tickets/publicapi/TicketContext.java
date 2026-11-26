package fr.entreprise.maintenance.features.tickets.publicapi;

import java.util.Optional;
import java.util.UUID;

/** Minimal ticket facts needed by other domains without exposing ticket storage. */
public interface TicketContext {
    record LinkedTicket(UUID ticketId,UUID equipmentId,boolean qualified) { }
    Optional<LinkedTicket> findSiteTicket(UUID accountId,UUID siteId,UUID ticketId);
}
