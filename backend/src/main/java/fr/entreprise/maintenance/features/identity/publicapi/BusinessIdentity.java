package fr.entreprise.maintenance.features.identity.publicapi;

import java.util.UUID;

/** Dedicated WEB business authorization. Implementations re-read current identity, never a DTO claim. */
public interface BusinessIdentity {
    record Actor(UUID userId, boolean administrator) { }
    void checkAdministrator(IdentitySession session);
    Actor authorize(IdentitySession session, boolean administratorOnly, UUID targetClient);
}
