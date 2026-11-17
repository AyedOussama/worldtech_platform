package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.application.MobilePolicy;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityRole;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.MobileTokenStore;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcMobileTokenStore implements MobileTokenStore {
    private record Family(UUID user, UUID session, long version, Instant authenticatedAt, Instant expires, boolean consumed, boolean valid) { }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final IdentityStore identities;
    private final MobilePolicy policy;
    private final SecurityAudit audit;
    public JdbcMobileTokenStore(JdbcTemplate jdbc, PlatformTransactionManager manager, IdentityStore identities, MobilePolicy policy, SecurityAudit audit) {
        this.jdbc = jdbc; tx = new TransactionTemplate(manager); this.identities = identities; this.policy = policy; this.audit = audit;
    }

    @Override
    public Pair issue(IdentityStore.Credential expected, Instant now) {
        return tx.execute(status -> {
            var identity = identities.finishPasswordLogin(expected, now, now.plus(policy.familyLifetime()), "MOBILE");
            var account = identities.current(identity, "MOBILE", now).orElseThrow(JdbcMobileTokenStore::refused).account();
            if (!account.roles().contains(IdentityRole.TECHNICIAN) || account.roles().contains(IdentityRole.ADMIN)) { throw refused(); }
            return pair(identity.sessionId(), null, now, now.plus(policy.familyLifetime()));
        });
    }

    private Pair pair(UUID family, String parent, Instant now, Instant absolute) {
        String access = OpaqueTokens.generate(); String refresh = OpaqueTokens.generate();
        Instant accessEnd = min(now.plus(policy.accessTtl()), absolute);
        Instant refreshEnd = min(now.plus(policy.refreshIdle()), absolute);
        jdbc.update("INSERT INTO app.mobile_access_token(fingerprint,session_id,created_at,expires_at) VALUES (?,?,?,?)",
                OpaqueTokens.fingerprint(access), family, ts(now), ts(accessEnd));
        jdbc.update("INSERT INTO app.mobile_refresh_token(fingerprint,session_id,parent_fingerprint,created_at,expires_at) VALUES (?,?,?,?,?)",
                OpaqueTokens.fingerprint(refresh), family, parent, ts(now), ts(refreshEnd));
        return new Pair(access, refresh, accessEnd, refreshEnd, absolute);
    }

    @Override
    public Pair issueExternal(UUID user, long version, Instant now) {
        return tx.execute(status -> {
            var identity = identities.finishExternalLogin(user, version, now, now.plus(policy.familyLifetime()), "MOBILE");
            var account = identities.current(identity, "MOBILE", now).orElseThrow(JdbcMobileTokenStore::refused).account();
            if (!account.roles().contains(IdentityRole.TECHNICIAN) || account.roles().contains(IdentityRole.ADMIN)) { throw refused(); }
            return pair(identity.sessionId(), null, now, now.plus(policy.familyLifetime()));
        });
    }

    private Family lock(String fingerprint, Instant now) {
        var owners = jdbc.query("SELECT s.user_id FROM app.mobile_refresh_token t JOIN app.auth_session s USING(session_id) WHERE t.fingerprint=?",
                (row, index) -> row.getObject(1, UUID.class), fingerprint);
        if (owners.isEmpty()) { return null; }
        // Same order as reset/security changes: user -> family -> token. Never the reverse.
        jdbc.queryForObject("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", UUID.class, owners.getFirst());
        return jdbc.query("SELECT s.*,t.consumed_at, (s.revoked_at IS NULL AND t.revoked_at IS NULL AND t.expires_at>? "
                        + "AND s.expires_at>? AND s.channel='MOBILE' AND s.security_version=u.security_version AND u.status IN ('PENDING','ACTIVE') "
                        + "AND EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role='TECHNICIAN') "
                        + "AND NOT EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role='ADMIN')) AS valid "
                        + "FROM app.mobile_refresh_token t JOIN app.auth_session s USING(session_id) JOIN app.app_user u USING(user_id) "
                        + "WHERE t.fingerprint=? FOR UPDATE OF s,t", (row, index) -> new Family(row.getObject("user_id", UUID.class),
                        row.getObject("session_id", UUID.class), row.getLong("security_version"), row.getTimestamp("authenticated_at").toInstant(),
                        row.getTimestamp("expires_at").toInstant(), row.getTimestamp("consumed_at") != null, row.getBoolean("valid")),
                ts(now), ts(now), fingerprint).stream().findFirst().orElse(null);
    }

    @Override
    public Pair rotate(String fingerprint, Instant now) {
        Pair result = tx.execute(status -> {
            Family family = lock(fingerprint, now);
            if (family == null) { return null; }
            if (family.consumed()) {
                revoke(family, now);
                audit.record(SecurityAudit.Action.MOBILE_REFRESH_REUSED, family.user(), family.session());
                return null; // Commit the revocation before returning a generic authentication failure.
            }
            if (!family.valid()) { return null; }
            jdbc.update("UPDATE app.mobile_refresh_token SET consumed_at=? WHERE fingerprint=?", ts(now), fingerprint);
            Pair next = pair(family.session(), fingerprint, now, family.expires());
            audit.record(SecurityAudit.Action.MOBILE_REFRESH_ROTATED, family.user(), family.session());
            return next;
        });
        if (result == null) { throw refused(); }
        return result;
    }

    @Override
    public Optional<LoginIdentity> access(String fingerprint, Instant now) {
        return jdbc.query("SELECT s.* FROM app.mobile_access_token t JOIN app.auth_session s USING(session_id) "
                        + "WHERE t.fingerprint=? AND t.revoked_at IS NULL AND t.expires_at>? AND s.channel='MOBILE' "
                        + "AND s.revoked_at IS NULL AND s.expires_at>?", (row, index) -> new LoginIdentity(row.getObject("user_id", UUID.class),
                        row.getObject("session_id", UUID.class), row.getLong("security_version"), row.getTimestamp("authenticated_at").toInstant()),
                fingerprint, ts(now), ts(now)).stream().findFirst();
    }

    private void revoke(Family family, Instant now) {
        jdbc.update("UPDATE app.auth_session SET revoked_at=coalesce(revoked_at,?) WHERE session_id=?", ts(now), family.session());
        jdbc.update("UPDATE app.mobile_access_token SET revoked_at=coalesce(revoked_at,?) WHERE session_id=?", ts(now), family.session());
        jdbc.update("UPDATE app.mobile_refresh_token SET revoked_at=coalesce(revoked_at,?) WHERE session_id=?", ts(now), family.session());
    }

    @Override
    public void logout(String fingerprint, Instant now) {
        tx.executeWithoutResult(status -> {
            Family family = lock(fingerprint, now);
            if (family != null) { revoke(family, now); audit.record(SecurityAudit.Action.LOGOUT, family.user(), family.session()); }
        });
    }

    @Override
    public void logoutAll(LoginIdentity identity, Instant now) {
        tx.executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", UUID.class, identity.userId());
            if (identities.current(identity, "MOBILE", now).isEmpty()) { throw refused(); }
            jdbc.update("UPDATE app.app_user SET security_version=security_version+1,updated_at=? WHERE user_id=?", ts(now), identity.userId());
            jdbc.update("UPDATE app.auth_session SET revoked_at=? WHERE user_id=? AND revoked_at IS NULL", ts(now), identity.userId());
            jdbc.update("DELETE FROM app.spring_session WHERE principal_name=?", identity.userId().toString());
            audit.record(SecurityAudit.Action.LOGOUT_ALL, identity.userId(), identity.userId());
        });
    }

    private static Instant min(Instant first, Instant second) { return first.isBefore(second) ? first : second; }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private static IdentityFailure refused() { return new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
}
