package fr.entreprise.maintenance.features.clients.publicapi;

import java.util.UUID;

/** Trusted identity orchestration only, after standard OIDC validation and creation of a new pending CLIENT. */
public interface GoogleClientAdmission {
    void checkInvitation(String fingerprint,String email);
    void lockInvitation(String fingerprint,String email);
    void enroll(UUID newUser,String fingerprint,String verifiedEmail);
}
