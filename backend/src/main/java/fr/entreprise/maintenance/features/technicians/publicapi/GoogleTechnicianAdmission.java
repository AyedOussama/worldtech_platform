package fr.entreprise.maintenance.features.technicians.publicapi;

import java.util.UUID;

/** Trusted identity orchestration only; no caller-supplied role or approval. */
public interface GoogleTechnicianAdmission {
    void enroll(UUID newPendingUser);
}
