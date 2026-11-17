package fr.entreprise.maintenance.features.identity.domain;

import java.time.Instant;
import java.util.Optional;

public interface MobileTokenStore {
    record Pair(String accessToken, String refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt, Instant familyExpiresAt) {
        @Override public String toString() { return "Pair[redacted]"; }
    }
    Pair issue(IdentityStore.Credential expected, Instant now);
    Pair issueExternal(java.util.UUID user, long expectedVersion, Instant now);
    Pair rotate(String fingerprint, Instant now);
    Optional<LoginIdentity> access(String fingerprint, Instant now);
    void logout(String refreshFingerprint, Instant now);
    void logoutAll(LoginIdentity identity, Instant now);
}
