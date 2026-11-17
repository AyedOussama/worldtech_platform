package fr.entreprise.maintenance.features.clients.publicapi;

import java.util.UUID;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;

/** Transaction-bound authority for assets, not a cached credential or public request value. */
public interface ClientAccountAccess {
    record Scope(UUID accountId, UUID actorUserId, UUID memberId, boolean administrator) { }
    Scope authorize(IdentitySession actor, UUID accountId, boolean administratorOnly);
    UUID currentMember(Scope authorizedAdministrator, UUID userId);
}
