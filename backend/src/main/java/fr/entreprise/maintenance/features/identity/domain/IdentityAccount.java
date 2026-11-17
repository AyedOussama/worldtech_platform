package fr.entreprise.maintenance.features.identity.domain;

import java.util.Set;
import java.util.UUID;

public record IdentityAccount(UUID userId, AccountStatus status, Set<IdentityRole> roles,
        long securityVersion, String email, boolean emailVerified) {
    public IdentityAccount {
        roles = Set.copyOf(roles);
    }
}
