package fr.entreprise.maintenance.features.identity.domain;

import java.time.Instant;

public interface ActionTokens {
    enum Purpose { EMAIL_VERIFICATION, PASSWORD_RESET }
    void issue(String normalizedEmail, Purpose purpose, String fingerprint, String rawToken, Instant now, Instant expires);
    boolean usable(String fingerprint, Purpose purpose, Instant now);
    void verifyEmail(String fingerprint, Instant now);
    void resetPassword(String fingerprint, String newHash, Instant now);
}
