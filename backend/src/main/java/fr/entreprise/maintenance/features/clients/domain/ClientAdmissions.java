package fr.entreprise.maintenance.features.clients.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClientAdmissions {
    record Invitation(UUID invitationId, String email, Instant expiresAt, long version, boolean accepted) {
        @Override public String toString() { return "Invitation[redacted]"; }
    }
    record Link(UUID invitationId, String email) {
        @Override public String toString() { return "Link[redacted]"; }
    }
    record History(long revision, UUID actorUserId, String action, String reason, Instant occurredAt) {
        @Override public String toString() { return "History[redacted]"; }
    }
    Invitation invite(String email, String display, UUID actor, String fingerprint, Instant now, Instant expires);
    Invitation invitation(UUID id);
    Invitation resend(UUID id, UUID actor, long expected, String fingerprint, Instant now, Instant expires);
    Link lockLink(String fingerprint, String email, Instant now);
    void checkLink(String fingerprint, String email, Instant now);
    ClientDossier accept(Link link, String fingerprint, UUID user, Instant now);
    ClientDossier own(UUID user);
    UUID owner(UUID dossier);
    ClientDossier lock(UUID dossier);
    List<ClientDossier> review(long offset, int limit, ClientDossier.State state);
    List<History> history(UUID dossier, long after, int limit);
    ClientDossier save(ClientDossier current, ClientProfile profile, UUID actor, Instant now);
    ClientDossier transition(ClientDossier current, ClientDossier.State next, UUID actor, String reason, Instant now);
}
