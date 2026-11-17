package fr.entreprise.maintenance.features.clients.domain;

import java.time.Instant;
import java.util.UUID;

public record ClientDossier(UUID dossierId, UUID userId, State state, ClientProfile profile, long version, Instant createdAt) {
    public enum State { DRAFT, SUBMITTED, NEEDS_INFORMATION, APPROVED, REJECTED }
    public enum Decision { APPROVED, NEEDS_INFORMATION, REJECTED }
    public void expect(long expected) { if (version != expected) { throw new ClientFailure(ClientFailure.Code.CLIENT_VERSION_STALE); } }
    public void editable() {
        if (state != State.DRAFT && state != State.NEEDS_INFORMATION) { throw new ClientFailure(ClientFailure.Code.CLIENT_STATE_CONFLICT); }
    }
    public void submittable() { editable(); profile.requireComplete(); }
    public void decidable() {
        if (state != State.SUBMITTED) { throw new ClientFailure(ClientFailure.Code.CLIENT_STATE_CONFLICT); }
        profile.requireComplete();
    }
    public static String reason(String value) {
        if (value == null || value.isBlank() || value.length()>1000 || value.chars().anyMatch(Character::isISOControl)) {
            throw new ClientFailure(ClientFailure.Code.INVALID_CLIENT_INPUT);
        }
        return value.strip();
    }
    @Override public String toString() { return "ClientDossier[redacted]"; }
}
