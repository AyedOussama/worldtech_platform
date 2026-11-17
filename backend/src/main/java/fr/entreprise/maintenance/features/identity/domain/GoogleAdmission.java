package fr.entreprise.maintenance.features.identity.domain;

import java.util.UUID;

/** Bound to admission modules only by bootstrap adapters; no identity -> business implementation dependency. */
public interface GoogleAdmission {
    void checkClientInvitation(String fingerprint, String email);
    void lockClientInvitation(String fingerprint, String email);
    void client(UUID newUser, String fingerprint, String verifiedEmail);
    void technician(UUID newUser);
}
