package fr.entreprise.maintenance.features.identity.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MfaCommands {
    record Activation(List<String> recoveryCodes, boolean requiresLogin) {
        @Override public String toString() { return "Activation[redacted]"; }
    }
    void start(LoginIdentity identity, UUID enrollmentId, String secret, Instant now, Instant expires, boolean replacement);
    Activation activate(LoginIdentity identity, UUID enrollmentId, String code, Instant now);
    void prove(LoginIdentity identity, String code, Instant now, boolean recovery);
    List<String> regenerateRecovery(LoginIdentity identity, Instant now);
    LoginIdentity reauthenticate(LoginIdentity identity, String expectedHash, Instant now);
}
