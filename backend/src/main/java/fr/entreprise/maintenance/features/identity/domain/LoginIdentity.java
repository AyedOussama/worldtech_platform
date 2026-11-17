package fr.entreprise.maintenance.features.identity.domain;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/** Only opaque IDs/version go in the web SecurityContext, never credentials or MFA secrets. */
public record LoginIdentity(UUID userId, UUID sessionId, long securityVersion, Instant authenticatedAt)
        implements Principal, Serializable, fr.entreprise.maintenance.features.identity.publicapi.IdentitySession {
    @Override
    public String getName() {
        return userId.toString();
    }
}
