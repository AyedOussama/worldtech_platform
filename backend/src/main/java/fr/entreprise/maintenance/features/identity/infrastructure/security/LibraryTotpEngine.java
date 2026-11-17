package fr.entreprise.maintenance.features.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import fr.entreprise.maintenance.features.identity.domain.TotpEngine;
import org.bouncycastle.util.encoders.Base32;
import org.springframework.stereotype.Component;

@Component
public class LibraryTotpEngine implements TotpEngine {
    private final TimeBasedOneTimePasswordGenerator generator;
    private final SecureRandom random = new SecureRandom();

    public LibraryTotpEngine() {
        try { generator = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), 6, "HmacSHA1"); }
        catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException("Required TOTP algorithm unavailable."); }
    }

    @Override
    public String newSecret() {
        byte[] bytes = new byte[20]; random.nextBytes(bytes);
        return Base32.toBase32String(bytes);
    }

    @Override
    public long acceptedStep(String secret, String code, Instant now, long previousStep) {
        if (code == null || !code.matches("[0-9]{6}")) { return -1; }
        try {
            var key = new SecretKeySpec(Base32.decode(secret), "HmacSHA1");
            long step = Math.floorDiv(now.getEpochSecond(), 30);
            long accepted = -1;
            for (long candidate = step - 1; candidate <= step + 1; candidate++) {
                if (candidate < 0) { continue; }
                String expected = generator.generateOneTimePasswordString(key, Instant.ofEpochSecond(candidate * 30), java.util.Locale.ROOT);
                boolean same = MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), code.getBytes(StandardCharsets.US_ASCII));
                if (same && candidate > previousStep) { accepted = Math.max(accepted, candidate); }
            }
            return accepted;
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            throw new IllegalStateException("MFA verification unavailable.");
        }
    }
}
