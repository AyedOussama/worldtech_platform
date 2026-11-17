package fr.entreprise.maintenance.features.identity.domain;

import java.time.Instant;
import java.util.UUID;

public interface GoogleFlows {
    enum Channel { WEB, MOBILE }
    enum Intention { LOGIN, LINK }
    enum Registration { CLIENT, TECHNICIAN }
    record Transaction(UUID id, Channel channel, Intention intention, UUID userId, UUID sourceSession, Long securityVersion,
            String challenge, String destinationId, Instant expiresAt,Registration registration,String registrationEmail,String invitationFingerprint) {
        @Override public String toString() { return "Transaction[redacted]"; }
    }
    record Authorization(Transaction transaction, String encrypted) {
        @Override public String toString() { return "Authorization[redacted]"; }
    }
    record Completion(LoginIdentity webIdentity, String handoffCode, String destinationId, UUID transactionId) {
        @Override public String toString() { return "Completion[redacted]"; }
    }
    record Exchange(MobileTokenStore.Pair tokens, boolean linked) {
        @Override public String toString() { return "Exchange[redacted]"; }
    }
    void create(Transaction transaction, String launchFingerprint, String browserFingerprint, Instant now);
    Transaction launch(String launchFingerprint, String browserFingerprint, Instant now);
    void saveAuthorization(UUID transaction, String stateFingerprint, String encrypted, Instant now);
    Authorization authorization(String stateFingerprint, String browserFingerprint, boolean consume, Instant now);
    Completion complete(UUID transaction, GoogleSubject subject, String verifiedEmail, LoginIdentity currentBrowserIdentity, Instant now);
    Exchange exchange(UUID transaction, String fingerprint, String challenge, LoginIdentity currentMobileIdentity, Instant now);
    void unlink(LoginIdentity identity, Channel channel, Instant now);
}
