package fr.entreprise.maintenance.features.identity.publicapi;

import java.util.UUID;

/** Trusted in-process capability for lot 04 after its invitation/onboarding checks; not an HTTP API. */
public interface AccountProvisioning {
    UUID createPendingClient(String email, String password);
    UUID createPendingTechnician(String email, String password);
}
