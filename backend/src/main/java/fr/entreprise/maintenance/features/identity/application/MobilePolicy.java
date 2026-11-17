package fr.entreprise.maintenance.features.identity.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.identity.mobile")
public record MobilePolicy(Duration accessTtl, Duration refreshIdle, Duration familyLifetime) {
    public MobilePolicy {
        if (accessTtl == null || refreshIdle == null || familyLifetime == null || accessTtl.isZero() || accessTtl.isNegative()
                || refreshIdle.isZero() || refreshIdle.isNegative() || familyLifetime.compareTo(refreshIdle) < 0
                || refreshIdle.compareTo(accessTtl) < 0 || accessTtl.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalArgumentException("Invalid mobile session policy.");
        }
    }
}
