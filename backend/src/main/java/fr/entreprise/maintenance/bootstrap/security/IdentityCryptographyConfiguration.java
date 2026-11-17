package fr.entreprise.maintenance.bootstrap.security;

import java.util.Map;

import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityCryptographyConfiguration.Keyring.class)
public class IdentityCryptographyConfiguration {
    @ConfigurationProperties("app.identity.crypto")
    public record Keyring(String activeKey, Map<String, String> keys) {
        @Override public String toString() { return "Keyring[redacted]"; }
    }

    @Bean
    VersionedSecretCipher identitySecretCipher(Keyring keyring) {
        return new VersionedSecretCipher(keyring.activeKey(), keyring.keys());
    }
}
