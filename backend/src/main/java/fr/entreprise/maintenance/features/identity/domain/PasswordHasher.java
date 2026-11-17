package fr.entreprise.maintenance.features.identity.domain;

public interface PasswordHasher {
    String encode(String password);
    boolean matches(String password, String hash);
}
