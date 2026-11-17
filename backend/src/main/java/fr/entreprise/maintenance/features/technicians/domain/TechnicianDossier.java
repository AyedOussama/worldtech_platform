package fr.entreprise.maintenance.features.technicians.domain;

import java.time.Instant;
import java.util.UUID;

public record TechnicianDossier(UUID dossierId, UUID userId, State state, TechnicianProfile profile, long version, Instant createdAt) {
    public enum State { DRAFT, SUBMITTED, NEEDS_INFORMATION, APPROVED, REJECTED }
    public enum Decision { APPROVED, NEEDS_INFORMATION, REJECTED }
    public void expect(long expected) { if (version!=expected) { throw new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_VERSION_STALE); } }
    public void editable() {
        if (state!=State.DRAFT && state!=State.NEEDS_INFORMATION) { throw new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_STATE_CONFLICT); }
    }
    public void submittable() { editable(); profile.requireComplete(); }
    public void decidable(Decision decision) {
        if (state!=State.SUBMITTED) { throw new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_STATE_CONFLICT); }
        profile.requireComplete();
        // Product rule: professional evidence is mandatory. Lot 09 and evidence policy are not available yet.
        // No property/DTO bypass, fabricated CLEAN evidence, or activation without the required checks.
        if (decision==Decision.APPROVED) { throw new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_DOCUMENTS_REQUIRED); }
    }
    public static String reason(String value) {
        if (value==null || value.isBlank() || value.length()>1000 || value.chars().anyMatch(Character::isISOControl)) {
            throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT);
        }
        return value.strip();
    }
    @Override public String toString() { return "TechnicianDossier[redacted]"; }
}
