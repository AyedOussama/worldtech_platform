package fr.entreprise.maintenance.features.identity.application;

import java.time.Clock;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.MobileTokenStore;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import org.springframework.stereotype.Service;

@Service
public class MobileIdentityService {
    public record Current(LoginIdentity identity, IdentityStore.CurrentSession session) { }
    private final LocalPasswordAuthentication passwords;
    private final MobileTokenStore tokens;
    private final IdentityStore identities;
    private final LoginAttempts limits;
    private final Clock clock;
    private final fr.entreprise.maintenance.features.identity.domain.MfaCommands proofs;
    public MobileIdentityService(LocalPasswordAuthentication passwords, MobileTokenStore tokens, IdentityStore identities, LoginAttempts limits, Clock clock,
            fr.entreprise.maintenance.features.identity.domain.MfaCommands proofs) {
        this.passwords = passwords; this.tokens = tokens; this.identities = identities; this.limits = limits; this.clock = clock;
        this.proofs = proofs;
    }
    public MobileTokenStore.Pair login(String email, String password, String peer) {
        return tokens.issue(passwords.verify(email, password, peer), clock.instant());
    }
    public MobileTokenStore.Pair refresh(String refresh, String peer) {
        String hash = OpaqueTokens.fingerprint(refresh);
        limits.mobile("refresh", hash, peer);
        return tokens.rotate(hash, clock.instant());
    }
    public Current authenticate(String access) {
        var identity = tokens.access(OpaqueTokens.fingerprint(access), clock.instant()).orElseThrow(MobileIdentityService::refused);
        return new Current(identity, current(identity));
    }
    public IdentityStore.CurrentSession current(LoginIdentity identity) {
        var current = identities.current(identity, "MOBILE", clock.instant()).orElseThrow(MobileIdentityService::refused);
        if (!current.account().roles().contains(IdentityRole.TECHNICIAN) || current.account().roles().contains(IdentityRole.ADMIN)) { throw refused(); }
        return current;
    }
    public void logout(String refresh, String peer) {
        String hash = OpaqueTokens.fingerprint(refresh);
        limits.mobile("logout", hash, peer);
        tokens.logout(hash, clock.instant());
    }
    public void logoutAll(LoginIdentity identity) { tokens.logoutAll(identity, clock.instant()); }
    public void reauthenticate(LoginIdentity identity, String password, String peer) {
        var account = current(identity).account();
        var credential = passwords.verify(account.email(), password, peer);
        proofs.reauthenticate(identity, credential.hash(), clock.instant());
    }
    private static IdentityFailure refused() { return new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
}
