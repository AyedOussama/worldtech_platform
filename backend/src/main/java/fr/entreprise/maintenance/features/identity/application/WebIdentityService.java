package fr.entreprise.maintenance.features.identity.application;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import fr.entreprise.maintenance.features.identity.domain.IdentityAccount;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import org.springframework.stereotype.Service;

@Service
public class WebIdentityService {
    private final IdentityStore store;
    private final LocalPasswordAuthentication passwords;
    private final IdentityPolicy policy;
    private final Clock clock;

    public WebIdentityService(IdentityStore store, LocalPasswordAuthentication passwords, IdentityPolicy policy, Clock clock) {
        this.store = store;
        this.passwords = passwords;
        this.policy = policy;
        this.clock = clock;
    }

    public LoginIdentity login(String email, String password, String remoteAddress) {
        var credential = passwords.verify(email, password, remoteAddress);
        var now = clock.instant();
        return store.finishPasswordLogin(credential, now, now.plus(policy.webAbsolute()), "WEB");
    }

    public Optional<IdentityStore.CurrentSession> current(LoginIdentity identity) {
        return store.current(identity, "WEB", clock.instant());
    }

    public Set<String> authorities(IdentityStore.CurrentSession current) {
        IdentityAccount account = current.account();
        if (account.roles().contains(IdentityRole.ADMIN) && current.mfaAt() == null) {
            return Set.of("MFA_PENDING");
        }
        if (!account.emailVerified()) { return Set.of("IDENTITY_READ", "EMAIL_PENDING"); }
        if (account.status() != fr.entreprise.maintenance.features.identity.domain.AccountStatus.ACTIVE) {
            return Set.of("IDENTITY_READ");
        }
        var roles = account.roles().stream().map(role -> "ROLE_" + role.name()).collect(Collectors.toSet());
        roles.add("IDENTITY_READ");
        return Set.copyOf(roles);
    }

    public void logout(LoginIdentity identity) {
        store.revoke(identity.sessionId(), clock.instant());
    }
}
