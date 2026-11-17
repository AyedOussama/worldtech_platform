package fr.entreprise.maintenance.features.identity.domain;

public enum AccountStatus {
    PENDING, ACTIVE, SUSPENDED, REJECTED;

    public boolean canAuthenticate() {
        return this == PENDING || this == ACTIVE;
    }
}
