package fr.entreprise.maintenance.features.identity.application;

import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import org.springframework.stereotype.Service;

@Service
public class LocalPasswordAuthentication {
    private final IdentityStore accounts;
    private final PasswordHasher passwords;
    private final LoginAttempts attempts;
    private final SecurityAudit audit;
    private final String dummy;
    public LocalPasswordAuthentication(IdentityStore accounts, PasswordHasher passwords, LoginAttempts attempts, SecurityAudit audit) {
        this.accounts = accounts; this.passwords = passwords; this.attempts = attempts; this.audit = audit;
        dummy = passwords.encode(OpaqueTokens.generate());
    }
    public IdentityStore.Credential verify(String email, String password, String peer) {
        String normalized;
        try { normalized = EmailAddress.parse(email).normalized(); }
        catch (IdentityFailure ignored) { normalized = "invalid-email"; }
        attempts.reserve(normalized, peer);
        if (password == null || password.length() > 256 || password.codePointCount(0, password.length()) > 128) {
            throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS);
        }
        var credential = accounts.credential(normalized);
        boolean matches = passwords.matches(password, credential.map(IdentityStore.Credential::hash).orElse(dummy));
        if (!matches || credential.isEmpty() || credential.get().hash() == null || !credential.get().account().status().canAuthenticate()) {
            audit.record(SecurityAudit.Action.LOGIN_FAILED, null, null);
            throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS);
        }
        return credential.get();
    }
}
