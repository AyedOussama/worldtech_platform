package fr.entreprise.maintenance.features.clients.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class InvitationTokens {
    private static final SecureRandom RANDOM = new SecureRandom();
    private InvitationTokens() { }
    public static String generate() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static String fingerprint(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) { throw new ClientFailure(ClientFailure.Code.INVALID_INVITATION); }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("Required digest unavailable."); }
    }
}
