package fr.entreprise.maintenance.features.identity.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdentityStore {
    record Credential(IdentityAccount account, String hash) {
        @Override
        public String toString() { return "Credential[redacted]"; }
    }
    record CurrentSession(IdentityAccount account, Instant createdAt, Instant expiresAt, Instant mfaAt) { }

    Optional<Credential> credential(String normalizedEmail);
    IdentityAccount createPending(String normalizedEmail, String displayEmail, String hash, IdentityRole role, Instant now);
    LoginIdentity finishPasswordLogin(Credential expected, Instant now, Instant expiresAt, String channel);
    LoginIdentity finishExternalLogin(UUID user, long expectedVersion, Instant now, Instant expiresAt, String channel);
    Optional<CurrentSession> current(LoginIdentity identity, String channel, Instant now);
    void revoke(UUID sessionId, Instant now);
    boolean reserveAttempt(String key, Instant windowStart, Instant expiresAt, int limit);
}
