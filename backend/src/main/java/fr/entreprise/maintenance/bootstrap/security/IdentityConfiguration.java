package fr.entreprise.maintenance.bootstrap.security;

import java.time.Clock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

import fr.entreprise.maintenance.features.identity.application.IdentityPolicy;
import fr.entreprise.maintenance.features.identity.application.MobilePolicy;
import fr.entreprise.maintenance.features.identity.application.GooglePolicy;
import fr.entreprise.maintenance.features.identity.domain.PasswordPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({IdentityPolicy.class, MobilePolicy.class, GooglePolicy.class})
public class IdentityConfiguration {
    @Bean
    Clock identityClock() { return Clock.systemUTC(); }

    @Bean
    PasswordPolicy identityPasswordPolicy() throws IOException {
        try (var stream = IdentityConfiguration.class.getResourceAsStream("/identity/common-passwords.txt")) {
            if (stream == null) { throw new IllegalStateException("Controlled password blocklist missing."); }
            return new PasswordPolicy(new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank()).map(line -> line.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
        }
    }
}
