package fr.entreprise.maintenance.features.identity.publicapi;

import java.time.Instant;
import java.util.UUID;

/** Server-authenticated principal contract. Never bind this from a public request body. */
public interface IdentitySession {
    UUID userId();
    UUID sessionId();
    long securityVersion();
    Instant authenticatedAt();
}
