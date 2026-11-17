package fr.entreprise.maintenance.features.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import org.springframework.stereotype.Service;

@Service
public class LoginAttempts {
    private final IdentityStore store;
    private final IdentityPolicy policy;
    private final Clock clock;

    public LoginAttempts(IdentityStore store, IdentityPolicy policy, Clock clock) {
        this.store = store;
        this.policy = policy;
        this.clock = clock;
    }

    public void reserve(String email, String remoteAddress) {
        // remoteAddress is the servlet peer address, not an untrusted forwarding header.
        reserve("login/ip/" + remoteAddress, 60, policy.loginPerIp());
        reserve("login/identity/" + email, 900, policy.loginPerIdentity());
    }

    public void action(String purpose, String identity, String peer) {
        reserve("action/ip/" + purpose + "/" + peer, 900, 20);
        if (!identity.isEmpty()) { reserve("action/identity/" + purpose + "/" + identity, 900, 3); }
    }

    public void mfa(java.util.UUID user, String peer) {
        reserve("mfa/ip/" + peer, 300, 20);
        reserve("mfa/user/" + user, 300, 5);
    }

    public void mobile(String action, String fingerprint, String peer) {
        reserve("mobile/ip/" + action + "/" + peer, 60, 60);
        reserve("mobile/token/" + action + "/" + fingerprint, 60, 10);
    }

    public void onboarding(String operation, String identity, String peer) {
        reserve("onboarding/ip/" + operation + "/" + peer, 900, 20);
        if (identity != null && !identity.isEmpty()) { reserve("onboarding/identity/" + operation + "/" + identity, 900, 5); }
    }

    private void reserve(String subject, long seconds, int limit) {
        Instant now = clock.instant();
        Instant start = Instant.ofEpochSecond(Math.floorDiv(now.getEpochSecond(), seconds) * seconds);
        if (!store.reserveAttempt(digest(subject), start, start.plusSeconds(seconds), limit)) {
            throw new IdentityFailure(IdentityFailure.Reason.RATE_LIMITED,
                    (int) Math.max(1, start.plusSeconds(seconds).getEpochSecond() - now.getEpochSecond()));
        }
    }

    public static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required digest unavailable.");
        }
    }
}
