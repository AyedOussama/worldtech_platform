package fr.entreprise.maintenance.features.technicians.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TechnicianAdmissions {
    record History(long revision, UUID actorUserId, String action, String reason, Instant occurredAt) {
        @Override public String toString() { return "History[redacted]"; }
    }
    TechnicianDossier open(UUID user, Instant now);
    TechnicianDossier own(UUID user);
    UUID owner(UUID dossier);
    TechnicianDossier lock(UUID dossier);
    TechnicianDossier save(TechnicianDossier dossier, TechnicianProfile profile, UUID actor, Instant now);
    TechnicianDossier transition(TechnicianDossier dossier, TechnicianDossier.State next, UUID actor, String reason, Instant now);
    List<TechnicianDossier> review(long offset, int limit, TechnicianDossier.State state);
    List<History> history(UUID dossier, long after, int size);
}
