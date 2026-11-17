package fr.entreprise.maintenance.features.identity.application;

import java.time.Clock;
import java.util.UUID;
import fr.entreprise.maintenance.features.identity.domain.AdministratorAccounts;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import fr.entreprise.maintenance.features.identity.domain.PasswordPolicy;
import org.springframework.stereotype.Service;

@Service
public class AdministratorBootstrapService {
    private final AdministratorAccounts accounts;
    private final PasswordHasher hasher;
    private final PasswordPolicy passwords;
    private final Clock clock;
    public AdministratorBootstrapService(AdministratorAccounts accounts, PasswordHasher hasher, PasswordPolicy passwords, Clock clock) {
        this.accounts = accounts; this.hasher = hasher; this.passwords = passwords; this.clock = clock;
    }

    public UUID create(String email, String password) {
        EmailAddress address = EmailAddress.parse(email);
        passwords.validate(password);
        return accounts.create(address.normalized(), address.display(), hasher.encode(password), clock.instant());
    }
}
