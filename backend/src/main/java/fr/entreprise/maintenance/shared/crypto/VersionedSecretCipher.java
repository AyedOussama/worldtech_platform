package fr.entreprise.maintenance.shared.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** JCA authenticated encryption, versioned external keyring and purpose-bound associated data. */
public final class VersionedSecretCipher {
    private final String active;
    private final Map<String, SecretKeySpec> keys;
    private final SecureRandom random = new SecureRandom();

    public VersionedSecretCipher(String active, Map<String, String> encodedKeys) {
        if (!active.matches("[A-Za-z0-9_-]{1,32}") || !encodedKeys.containsKey(active)) {
            throw new IllegalArgumentException("Missing active encryption key version.");
        }
        this.active = active;
        this.keys = encodedKeys.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {
            if (!entry.getKey().matches("[A-Za-z0-9_-]{1,32}")) { throw new IllegalArgumentException("Invalid encryption key version."); }
            byte[] key;
            try { key = Base64.getUrlDecoder().decode(entry.getValue().strip()); }
            catch (IllegalArgumentException failure) { throw new IllegalArgumentException("Invalid external encryption key."); }
            if (key.length != 32) { throw new IllegalArgumentException("External AES key must contain 32 bytes."); }
            return new SecretKeySpec(key, "AES");
        }));
    }

    public String encrypt(String plaintext, String context) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(active), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = Arrays.copyOf(nonce, nonce.length + ciphertext.length);
            System.arraycopy(ciphertext, 0, combined, nonce.length, ciphertext.length);
            return active + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("Secret encryption failed."); }
    }

    public String decrypt(String envelope, String context) {
        try {
            String[] parts = envelope.split("\\.", 2);
            if (parts.length != 2 || !keys.containsKey(parts[0]) || envelope.length() > 32768) {
                throw new GeneralSecurityException();
            }
            byte[] combined = Base64.getUrlDecoder().decode(parts[1]);
            if (combined.length < 28) { throw new GeneralSecurityException(); }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keys.get(parts[0]), new GCMParameterSpec(128, Arrays.copyOf(combined, 12)));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(combined, 12, combined.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException failure) { throw new IllegalStateException("Secret decryption refused."); }
    }
}
