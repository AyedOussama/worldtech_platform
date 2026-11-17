package fr.entreprise.maintenance.features.identity.application;

import java.time.Clock;
import java.util.UUID;

import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import fr.entreprise.maintenance.features.identity.domain.PasswordPolicy;
import fr.entreprise.maintenance.features.identity.publicapi.AccountProvisioning;
import org.springframework.stereotype.Service;

@Service
public class AccountCreationService implements AccountProvisioning, fr.entreprise.maintenance.features.identity.publicapi.RegistrationAccounts {
    private final IdentityStore store;
    private final PasswordHasher hasher;
    private final PasswordPolicy passwords;
    private final Clock clock;
    private final fr.entreprise.maintenance.features.identity.domain.ActionTokens actions;

    public AccountCreationService(IdentityStore store, PasswordHasher hasher, PasswordPolicy passwords, Clock clock,
            fr.entreprise.maintenance.features.identity.domain.ActionTokens actions) {
        this.store = store; this.hasher = hasher; this.passwords = passwords; this.clock = clock;
        this.actions = actions;
    }

    @Override
    public UUID createPendingClient(String email, String password) { return create(email, password, IdentityRole.CLIENT); }
    @Override
    public UUID createPendingTechnician(String email, String password) { return create(email, password, IdentityRole.TECHNICIAN); }

    private UUID create(String email, String password, IdentityRole role) {
        EmailAddress address = EmailAddress.parse(email);
        passwords.validate(password);
        return store.createPending(address.normalized(), address.display(), hasher.encode(password), role, clock.instant()).userId();
    }

    @Override
    public Prepared prepare(String email, String password) {
        EmailAddress address = EmailAddress.parse(email);
        passwords.validate(password);
        return new Prepared(address.normalized(), address.display(), hasher.encode(password));
    }
    @Override @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public UUID client(Prepared prepared) { return register(prepared, IdentityRole.CLIENT); }
    @Override @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public UUID technician(Prepared prepared) { return register(prepared, IdentityRole.TECHNICIAN); }
    private UUID register(Prepared prepared, IdentityRole role) {
        var now = clock.instant();
        UUID user;
        try { user = store.createPending(prepared.email(), prepared.displayEmail(), prepared.passwordHash(), role, now).userId(); }
        catch (fr.entreprise.maintenance.features.identity.domain.IdentityFailure failure) {
            if (failure.reason()==fr.entreprise.maintenance.features.identity.domain.IdentityFailure.Reason.CONFLICT) {
                throw new fr.entreprise.maintenance.features.identity.publicapi.RegistrationConflict();
            }
            throw failure;
        }
        String raw = fr.entreprise.maintenance.features.identity.domain.OpaqueTokens.generate();
        actions.issue(prepared.email(), fr.entreprise.maintenance.features.identity.domain.ActionTokens.Purpose.EMAIL_VERIFICATION,
                fr.entreprise.maintenance.features.identity.domain.OpaqueTokens.fingerprint(raw), raw, now, now.plusSeconds(86400));
        return user;
    }
}
