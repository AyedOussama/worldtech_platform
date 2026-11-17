package fr.entreprise.maintenance.features.identity.publicapi;

/** No existing account details are carried across the module boundary. */
public final class RegistrationConflict extends RuntimeException {
    public RegistrationConflict() { super("Registration request refused."); }
}
