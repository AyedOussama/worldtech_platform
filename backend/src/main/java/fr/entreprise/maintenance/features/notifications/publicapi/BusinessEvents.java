package fr.entreprise.maintenance.features.notifications.publicapi;

import java.time.Instant;
import java.util.UUID;

/** Append metadata in the producer's transaction. No delivery or personal/free-form payload. */
public interface BusinessEvents {
    enum Kind { CONTRACT_REVISED, TICKET_CREATED, TICKET_CHANGED, TICKET_MESSAGE, TICKET_INTERNAL_NOTE,
        MISSION_CREATED, MISSION_PUBLISHED, MISSION_REVISED }
    void append(Kind kind,UUID account,UUID site,UUID resource,long version,UUID actor,Instant occurredAt);
}
