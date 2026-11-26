package fr.entreprise.maintenance.features.identity.publicapi;

/** Dedicated mobile business authorization for an approved, active technician. */
public interface ActiveTechnician {
    void require(IdentitySession session);
}
