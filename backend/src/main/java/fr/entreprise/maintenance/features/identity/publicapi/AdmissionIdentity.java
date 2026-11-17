package fr.entreprise.maintenance.features.identity.publicapi;

import java.util.UUID;

/** Dedicated single-role admission capabilities, not a generic user/table editing API.
 * Mixed-role applicants are refused: the global identity status cannot represent independent approvals. */
public interface AdmissionIdentity {
    enum Channel { WEB, MOBILE }
    enum Role { CLIENT, TECHNICIAN }
    record Applicant(UUID userId, String email, boolean emailVerified) {
        @Override public String toString() { return "Applicant[redacted]"; }
    }
    Applicant applicant(IdentitySession session, Channel channel, Role role, boolean requireVerifiedEmail);
    void administrator(IdentitySession session, UUID targetUser);
    void checkAdministrator(IdentitySession session);
    void approveClient(UUID user);
    void rejectApplicant(UUID user, Role role);
    String applicantEmail(UUID user, Role role);
    String normalizeEmail(String email);
    void limit(String operation, String identity, String peer);
}
