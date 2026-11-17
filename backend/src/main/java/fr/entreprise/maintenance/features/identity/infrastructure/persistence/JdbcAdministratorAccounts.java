package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.domain.AdministratorAccounts;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcAdministratorAccounts implements AdministratorAccounts {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final SecurityAudit audit;
    public JdbcAdministratorAccounts(JdbcTemplate jdbc, PlatformTransactionManager manager, SecurityAudit audit) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.audit = audit;
    }

    @Override
    public UUID create(String email, String display, String hash, Instant now) {
        try {
            return tx.execute(status -> {
                UUID id = UUID.randomUUID();
                jdbc.update("INSERT INTO app.app_user(user_id,status,created_at,updated_at) VALUES (?,'ACTIVE',?,?)", id, ts(now), ts(now));
                jdbc.update("INSERT INTO app.local_credential(user_id,email_normalized,email_display,password_hash,updated_at) VALUES (?,?,?,?,?)",
                        id, email, display, hash, ts(now));
                jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,'ADMIN')", id);
                audit.record(SecurityAudit.Action.ADMIN_BOOTSTRAPPED, null, id);
                return id;
            });
        } catch (DuplicateKeyException failure) { throw new IdentityFailure(IdentityFailure.Reason.CONFLICT); }
    }

    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
}
