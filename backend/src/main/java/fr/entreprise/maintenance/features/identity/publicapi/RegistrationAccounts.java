package fr.entreprise.maintenance.features.identity.publicapi;

import java.util.UUID;

public interface RegistrationAccounts {
    record Prepared(String email, String displayEmail, String passwordHash) {
        @Override public String toString() { return "Prepared[redacted]"; }
    }
    Prepared prepare(String email, String password);
    UUID client(Prepared prepared);
    UUID technician(Prepared prepared);
}
