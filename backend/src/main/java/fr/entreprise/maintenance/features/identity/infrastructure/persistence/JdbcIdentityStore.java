package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import fr.entreprise.maintenance.features.identity.domain.AccountStatus;
import fr.entreprise.maintenance.features.identity.domain.IdentityAccount;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcIdentityStore implements IdentityStore {
    private static final String ACCOUNT = "SELECT u.*, c.email_display, c.email_verified_at FROM app.app_user u "
            + "JOIN app.local_credential c USING(user_id) ";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final TransactionTemplate independent;
    private final SecurityAudit audit;

    public JdbcIdentityStore(JdbcTemplate jdbc, PlatformTransactionManager manager, SecurityAudit audit) {
        this.jdbc = jdbc;
        this.audit = audit;
        transactions = new TransactionTemplate(manager);
        independent = new TransactionTemplate(manager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private IdentityAccount account(ResultSet rows) throws SQLException {
        UUID id = rows.getObject("user_id", UUID.class);
        Set<IdentityRole> roles = Set.copyOf(jdbc.query("SELECT role FROM app.user_role WHERE user_id = ?",
                (row, index) -> IdentityRole.valueOf(row.getString(1)), id));
        return new IdentityAccount(id, AccountStatus.valueOf(rows.getString("status")), roles,
                rows.getLong("security_version"), rows.getString("email_display"), rows.getTimestamp("email_verified_at") != null);
    }

    @Override
    public Optional<Credential> credential(String email) {
        return jdbc.query("SELECT u.*, c.* FROM app.app_user u JOIN app.local_credential c USING(user_id) "
                        + "WHERE c.email_normalized = ?", (rows, index) -> new Credential(account(rows), rows.getString("password_hash")), email)
                .stream().findFirst();
    }

    @Override
    public IdentityAccount createPending(String email, String display, String hash, IdentityRole role, Instant now) {
        if (role == IdentityRole.ADMIN) { throw new IdentityFailure(IdentityFailure.Reason.FORBIDDEN); }
        try {
            return transactions.execute(status -> {
                UUID id = UUID.randomUUID();
                jdbc.update("INSERT INTO app.app_user(user_id,status,created_at,updated_at) VALUES (?,'PENDING',?,?)", id, ts(now), ts(now));
                jdbc.update("INSERT INTO app.local_credential(user_id,email_normalized,email_display,password_hash,updated_at) VALUES (?,?,?,?,?)",
                        id, email, display, hash, ts(now));
                jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,?)", id, role.name());
                return new IdentityAccount(id, AccountStatus.PENDING, Set.of(role), 0, display, false);
            });
        } catch (DuplicateKeyException exception) {
            throw new IdentityFailure(IdentityFailure.Reason.CONFLICT);
        }
    }

    @Override
    public LoginIdentity finishPasswordLogin(Credential expected, Instant now, Instant expires, String channel) {
        return transactions.execute(status -> {
            var rows = jdbc.query(ACCOUNT + "WHERE u.user_id = ? FOR UPDATE OF u, c",
                    (row, index) -> account(row), expected.account().userId());
            if (rows.isEmpty() || !rows.getFirst().status().canAuthenticate()
                    || rows.getFirst().securityVersion() != expected.account().securityVersion()
                    || !jdbc.queryForObject("SELECT password_hash = ? FROM app.local_credential WHERE user_id = ?",
                    Boolean.class, expected.hash(), expected.account().userId())) {
                throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS);
            }
            UUID session = UUID.randomUUID();
            jdbc.update("INSERT INTO app.auth_session(session_id,user_id,channel,security_version,created_at,authenticated_at,expires_at) VALUES (?,?,?,?,?,?,?)",
                    session, expected.account().userId(), channel, rows.getFirst().securityVersion(), ts(now), ts(now), ts(expires));
            audit.record(SecurityAudit.Action.LOGIN_SUCCEEDED, expected.account().userId(), session);
            return new LoginIdentity(expected.account().userId(), session, rows.getFirst().securityVersion(), now);
        });
    }

    @Override
    public LoginIdentity finishExternalLogin(UUID user, long version, Instant now, Instant expires, String channel) {
        return transactions.execute(status -> {
            var accounts = jdbc.query("SELECT security_version FROM app.app_user WHERE user_id=? AND status IN ('PENDING','ACTIVE') FOR UPDATE",
                    (row, index) -> row.getLong(1), user);
            if (accounts.isEmpty() || accounts.getFirst() != version) { throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
            UUID session = UUID.randomUUID();
            jdbc.update("INSERT INTO app.auth_session(session_id,user_id,channel,security_version,created_at,authenticated_at,expires_at) VALUES (?,?,?,?,?,?,?)",
                    session, user, channel, version, ts(now), ts(now), ts(expires));
            audit.record(SecurityAudit.Action.GOOGLE_LOGIN, user, session);
            return new LoginIdentity(user, session, version, now);
        });
    }

    @Override
    public Optional<CurrentSession> current(LoginIdentity identity, String channel, Instant now) {
        return jdbc.query("SELECT u.*, c.email_display, c.email_verified_at, s.created_at AS auth_created_at, s.expires_at, s.mfa_at "
                        + "FROM app.app_user u JOIN app.local_credential c USING(user_id) "
                        + "JOIN app.auth_session s USING(user_id) WHERE s.session_id = ? AND u.user_id = ? "
                        + "AND s.channel = ? AND s.revoked_at IS NULL AND s.expires_at > ? "
                        + "AND u.security_version = ? AND s.security_version = u.security_version "
                        + "AND u.status IN ('PENDING','ACTIVE')",
                (rows, index) -> new CurrentSession(account(rows), instant(rows, "auth_created_at"),
                        instant(rows, "expires_at"), instant(rows, "mfa_at")),
                identity.sessionId(), identity.userId(), channel, ts(now), identity.securityVersion()).stream().findFirst();
    }

    @Override
    public void revoke(UUID sessionId, Instant now) {
        transactions.executeWithoutResult(status -> {
            var users = jdbc.query("UPDATE app.auth_session SET revoked_at = ? WHERE session_id = ? AND revoked_at IS NULL RETURNING user_id",
                    (row, index) -> row.getObject(1, UUID.class), ts(now), sessionId);
            if (!users.isEmpty()) { audit.record(SecurityAudit.Action.LOGOUT, users.getFirst(), sessionId); }
        });
    }

    @Override
    public boolean reserveAttempt(String key, Instant start, Instant expires, int limit) {
        return Boolean.TRUE.equals(independent.execute(status -> !jdbc.query("""
                INSERT INTO app.auth_rate_bucket(bucket_key,window_start,hits,expires_at) VALUES (?,?,1,?)
                ON CONFLICT(bucket_key,window_start) DO UPDATE SET hits = app.auth_rate_bucket.hits + 1
                WHERE app.auth_rate_bucket.hits < ? RETURNING hits
                """, (row, index) -> row.getInt(1), key, ts(start), ts(expires), limit).isEmpty()));
    }

    private static Timestamp ts(Instant time) { return Timestamp.from(time); }
    private static Instant instant(ResultSet rows, String column) throws SQLException {
        Timestamp value = rows.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
