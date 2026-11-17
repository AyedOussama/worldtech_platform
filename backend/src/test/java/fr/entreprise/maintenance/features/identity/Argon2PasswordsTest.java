package fr.entreprise.maintenance.features.identity;

import java.time.Duration;

import fr.entreprise.maintenance.features.identity.application.IdentityPolicy;
import fr.entreprise.maintenance.features.identity.infrastructure.security.Argon2Passwords;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2PasswordsTest {
    @Test
    void explicitVersionedArgon2UsesFreshSaltsAndPreservesUnicodeAndWhitespace() {
        var encoder = new Argon2Passwords(new IdentityPolicy(Duration.ofHours(8), Duration.ofMinutes(5), 2, 10, 60));
        String password = " synthetic long passphrase \uD83D\uDD12 ";
        long start = System.nanoTime();
        String first = encoder.encode(password);
        String second = encoder.encode(password);
        assertThat(first.startsWith("{argon2id-v1}$argon2id$v=19$m=19456,t=2,p=1$")).isTrue();
        assertThat(first.equals(second)).isFalse();
        assertThat(encoder.matches(password, first)).isTrue();
        assertThat(encoder.matches(password.strip(), first)).isFalse();
        assertThat(encoder.matches("a different passphrase", first)).isFalse();
        System.out.println("Argon2 local probe: 2 encodes + 3 verifications, elapsed_ms=" + (System.nanoTime() - start) / 1_000_000);
    }
}
