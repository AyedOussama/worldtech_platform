package fr.entreprise.maintenance.features.identity.domain;

import java.time.Instant;

public interface TotpEngine {
    String newSecret();
    long acceptedStep(String secret, String code, Instant now, long previousStep);
}
