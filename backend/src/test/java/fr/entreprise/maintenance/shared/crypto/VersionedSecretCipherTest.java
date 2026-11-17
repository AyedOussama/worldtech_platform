package fr.entreprise.maintenance.shared.crypto;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionedSecretCipherTest {
    @Test
    void encryptionUsesFreshNoncesBindsTheOwnerAndSupportsOldReadKeys() {
        String key1 = key(); String key2 = key();
        var first = new VersionedSecretCipher("v1", Map.of("v1", key1));
        String secret = key();
        String encrypted = first.encrypt(secret, "owner-a");
        assertThat(encrypted.equals(first.encrypt(secret, "owner-a"))).isFalse();
        assertThat(first.decrypt(encrypted, "owner-a").equals(secret)).isTrue();
        assertThatThrownBy(() -> first.decrypt(encrypted, "owner-b")).isInstanceOf(IllegalStateException.class);
        var rotated = new VersionedSecretCipher("v2", Map.of("v1", key1, "v2", key2));
        assertThat(rotated.decrypt(encrypted, "owner-a").equals(secret)).isTrue();
        assertThat(rotated.encrypt(secret, "owner-a").startsWith("v2.")).isTrue();
        assertThatThrownBy(() -> new VersionedSecretCipher("v2", Map.of("v2", key2)).decrypt(encrypted, "owner-a"))
                .isInstanceOf(IllegalStateException.class);
        char replacement = encrypted.charAt(encrypted.length() - 2) == 'A' ? 'B' : 'A';
        String altered = encrypted.substring(0, encrypted.length() - 2) + replacement + encrypted.charAt(encrypted.length() - 1);
        assertThatThrownBy(() -> first.decrypt(altered, "owner-a")).isInstanceOf(IllegalStateException.class);
    }

    private static String key() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
