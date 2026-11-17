package fr.entreprise.maintenance.features.identity.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import fr.entreprise.maintenance.features.identity.domain.PasswordPolicy;
import org.springframework.stereotype.Service;

@Service
public class IdentityActions {
    private final ActionTokens tokens;
    private final LoginAttempts limits;
    private final PasswordPolicy passwords;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public IdentityActions(ActionTokens tokens, LoginAttempts limits, PasswordPolicy passwords, PasswordHasher hasher, Clock clock) {
        this.tokens = tokens; this.limits = limits; this.passwords = passwords; this.hasher = hasher; this.clock = clock;
    }

    public void request(String email, ActionTokens.Purpose purpose, String peer) {
        String normalized;
        try { normalized = EmailAddress.parse(email).normalized(); }
        catch (IdentityFailure ignored) { normalized = "invalid-email"; }
        limits.action(purpose.name(), normalized, peer);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var now = clock.instant();
        tokens.issue(normalized, purpose, LoginAttempts.digest(raw), raw, now,
                now.plusSeconds(purpose == ActionTokens.Purpose.EMAIL_VERIFICATION ? 86400 : 1800));
    }

    public void verify(String token, String peer) {
        limits.action("VERIFY_CONFIRM", "", peer);
        tokens.verifyEmail(fingerprint(token), clock.instant());
    }

    public void reset(String token, String password, String peer) {
        limits.action("RESET_CONFIRM", "", peer);
        String fingerprint = fingerprint(token);
        passwords.validate(password);
        if (!tokens.usable(fingerprint, ActionTokens.Purpose.PASSWORD_RESET, clock.instant())) {
            throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT);
        }
        String hash = hasher.encode(password); // Before acquiring the user/action transaction locks.
        tokens.resetPassword(fingerprint, hash, clock.instant());
    }

    private static String fingerprint(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) { throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT); }
        return LoginAttempts.digest(token);
    }
}
