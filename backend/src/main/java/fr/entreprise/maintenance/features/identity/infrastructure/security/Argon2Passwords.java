package fr.entreprise.maintenance.features.identity.infrastructure.security;

import java.util.Map;
import java.util.concurrent.Semaphore;

import fr.entreprise.maintenance.features.identity.application.IdentityPolicy;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class Argon2Passwords implements PasswordHasher {
    private final PasswordEncoder encoder = new DelegatingPasswordEncoder("argon2id-v1",
            Map.of("argon2id-v1", new Argon2PasswordEncoder(16, 32, 1, 19456, 2)));
    private final Semaphore permits;

    public Argon2Passwords(IdentityPolicy policy) {
        permits = new Semaphore(policy.passwordConcurrency());
    }

    @Override
    public String encode(String password) {
        acquire();
        try { return encoder.encode(password); }
        finally { permits.release(); }
    }

    @Override
    public boolean matches(String password, String hash) {
        acquire();
        try { return encoder.matches(password, hash); }
        finally { permits.release(); }
    }

    private void acquire() {
        if (!permits.tryAcquire()) { throw new IdentityFailure(IdentityFailure.Reason.RATE_LIMITED); }
    }
}
