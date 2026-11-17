package fr.entreprise.maintenance.features.identity.application;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.MfaCommands;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import fr.entreprise.maintenance.features.identity.domain.TotpEngine;
import org.springframework.stereotype.Service;

@Service
public class MfaService {
    public record Enrollment(UUID enrollmentId, String secret, String uri) {
        @Override public String toString() { return "Enrollment[redacted]"; }
    }
    private final MfaCommands commands;
    private final IdentityStore accounts;
    private final TotpEngine totp;
    private final PasswordHasher passwords;
    private final LoginAttempts limits;
    private final Clock clock;
    private final SecurityAudit audit;
    public MfaService(MfaCommands commands, IdentityStore accounts, TotpEngine totp, PasswordHasher passwords,
            LoginAttempts limits, Clock clock, SecurityAudit audit) {
        this.commands = commands; this.accounts = accounts; this.totp = totp; this.passwords = passwords;
        this.limits = limits; this.clock = clock; this.audit = audit;
    }

    public Enrollment start(LoginIdentity identity, String peer, boolean replacement) {
        limits.mfa(identity.userId(), peer);
        String secret = totp.newSecret(); UUID enrollment = UUID.randomUUID();
        var now = clock.instant();
        commands.start(identity, enrollment, secret, now, now.plusSeconds(300), replacement);
        String email = accounts.current(identity, "WEB", now).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS)).account().email();
        String label = URLEncoder.encode("Maintenance:" + email, StandardCharsets.UTF_8).replace("+", "%20");
        return new Enrollment(enrollment, secret, "otpauth://totp/" + label + "?secret=" + secret + "&issuer=Maintenance&algorithm=SHA1&digits=6&period=30");
    }

    public MfaCommands.Activation activate(LoginIdentity identity, UUID enrollment, String code, String peer) {
        limits.mfa(identity.userId(), peer);
        try { return commands.activate(identity, enrollment, code, clock.instant()); }
        catch (IdentityFailure failure) { audit.record(SecurityAudit.Action.MFA_FAILED, identity.userId(), identity.sessionId()); throw failure; }
    }

    public void prove(LoginIdentity identity, String code, String peer, boolean recovery) {
        limits.mfa(identity.userId(), peer);
        try { commands.prove(identity, code, clock.instant(), recovery); }
        catch (IdentityFailure failure) { audit.record(SecurityAudit.Action.MFA_FAILED, identity.userId(), identity.sessionId()); throw failure; }
    }

    public List<String> recoveryCodes(LoginIdentity identity, String peer) {
        limits.mfa(identity.userId(), peer);
        return commands.regenerateRecovery(identity, clock.instant());
    }

    public LoginIdentity reauthenticate(LoginIdentity identity, String password, String peer) {
        var current = accounts.current(identity, "WEB", clock.instant()).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS));
        String email = EmailAddress.parse(current.account().email()).normalized();
        limits.reserve(email, peer);
        var credential = accounts.credential(email).orElseThrow(() -> new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS));
        if (password == null || password.length() > 256 || password.codePointCount(0, password.length()) > 128
                || !passwords.matches(password, credential.hash())) { throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
        return commands.reauthenticate(identity, credential.hash(), clock.instant());
    }
}
