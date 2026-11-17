package fr.entreprise.maintenance.features.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Operator-only capability. No HTTP controller or public onboarding invokes this port. */
public interface AdministratorAccounts {
    UUID create(String email, String display, String passwordHash, Instant now);
}
