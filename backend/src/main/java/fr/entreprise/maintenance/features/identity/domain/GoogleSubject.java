package fr.entreprise.maintenance.features.identity.domain;

/** Construct only after standard OIDC validation. Email intentionally cannot select the local account. */
public record GoogleSubject(String issuer, String subject) {
    public GoogleSubject {
        if (!"https://accounts.google.com".equals(issuer) || subject == null || subject.isBlank() || subject.length() > 255
                || subject.chars().anyMatch(Character::isISOControl)) {
            throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS);
        }
    }
    @Override public String toString() { return "GoogleSubject[redacted]"; }
}
