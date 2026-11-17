package fr.entreprise.maintenance.features.identity.application;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.identity")
public record IdentityPolicy(@NotNull Duration webAbsolute, @NotNull Duration recentAuthentication,
        @Min(1) @Max(16) int passwordConcurrency, @Min(1) @Max(1000) int loginPerIdentity,
        @Min(1) @Max(10000) int loginPerIp) {
    public IdentityPolicy {
        if (webAbsolute == null || webAbsolute.isNegative() || webAbsolute.isZero()
                || recentAuthentication == null || recentAuthentication.isNegative() || recentAuthentication.isZero()) {
            throw new IllegalArgumentException("Invalid identity time policy.");
        }
    }
}
