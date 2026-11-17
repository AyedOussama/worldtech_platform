package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.application.IdentityPolicy;
import fr.entreprise.maintenance.features.identity.application.LoginAttempts;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.MfaCommands;
import fr.entreprise.maintenance.features.identity.domain.TotpEngine;
import fr.entreprise.maintenance.features.notifications.publicapi.MailOutbox;
import fr.entreprise.maintenance.shared.crypto.VersionedSecretCipher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcMfaCommands implements MfaCommands {
    private record Owner(String email, Instant authenticatedAt, Instant mfaAt) { }
    private record Factor(String active, String pending, Instant expires, UUID enrollment, UUID session, boolean replacement, long lastStep) {
        @Override public String toString() { return "Factor[redacted]"; }
    }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final VersionedSecretCipher cipher;
    private final TotpEngine totp;
    private final IdentityPolicy policy;
    private final SecurityAudit audit;
    private final MailOutbox mail;
    private final java.security.SecureRandom random = new java.security.SecureRandom();

    public JdbcMfaCommands(JdbcTemplate jdbc, PlatformTransactionManager manager, VersionedSecretCipher cipher,
            TotpEngine totp, IdentityPolicy policy, SecurityAudit audit, MailOutbox mail) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.cipher = cipher; this.totp = totp;
        this.policy = policy; this.audit = audit; this.mail = mail;
    }

    private Owner lockOwner(LoginIdentity identity, Instant now, boolean mfaRecent) {
        var users = jdbc.query("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", (row, index) -> row.getObject(1), identity.userId());
        if (users.isEmpty()) { throw denied(); }
        var rows = jdbc.query("SELECT c.email_normalized,s.authenticated_at,s.mfa_at FROM app.auth_session s "
                        + "JOIN app.app_user u USING(user_id) JOIN app.local_credential c USING(user_id) "
                        + "WHERE s.session_id=? AND u.user_id=? AND u.status='ACTIVE' AND c.email_verified_at IS NOT NULL "
                        + "AND s.revoked_at IS NULL AND s.expires_at>? AND u.security_version=? AND s.security_version=u.security_version "
                        + "AND EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role='ADMIN') FOR UPDATE OF s,c",
                (row, index) -> new Owner(row.getString(1), row.getTimestamp(2).toInstant(),
                        row.getTimestamp(3) == null ? null : row.getTimestamp(3).toInstant()),
                identity.sessionId(), identity.userId(), ts(now), identity.securityVersion());
        if (rows.isEmpty()) { throw denied(); }
        Owner owner = rows.getFirst();
        if (!owner.authenticatedAt().isAfter(now.minus(policy.recentAuthentication()))
                || (mfaRecent && (owner.mfaAt() == null || !owner.mfaAt().isAfter(now.minus(policy.recentAuthentication()))))) { throw denied(); }
        return owner;
    }

    private Factor factor(UUID user) {
        return jdbc.query("SELECT * FROM app.mfa_credential WHERE user_id=? FOR UPDATE", (row, index) -> new Factor(
                row.getString("secret_encrypted"), row.getString("pending_secret"),
                row.getTimestamp("pending_expires_at") == null ? null : row.getTimestamp("pending_expires_at").toInstant(),
                row.getObject("enrollment_id", UUID.class), row.getObject("pending_session_id", UUID.class),
                row.getBoolean("pending_replacement"), row.getLong("last_step")), user).stream().findFirst().orElse(null);
    }

    @Override
    public void start(LoginIdentity identity, UUID enrollment, String secret, Instant now, Instant expires, boolean replacement) {
        tx.executeWithoutResult(status -> {
            lockOwner(identity, now, replacement);
            Factor factor = factor(identity.userId());
            if ((factor != null && factor.active() != null) != replacement) { throw denied(); }
            jdbc.update("INSERT INTO app.mfa_credential(user_id,pending_secret,pending_expires_at,enrollment_id,pending_session_id,pending_replacement) "
                            + "VALUES (?,?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET pending_secret=excluded.pending_secret, "
                            + "pending_expires_at=excluded.pending_expires_at,enrollment_id=excluded.enrollment_id,pending_session_id=excluded.pending_session_id, "
                            + "pending_replacement=excluded.pending_replacement", identity.userId(), cipher.encrypt(secret, context(identity.userId())),
                    ts(expires), enrollment, identity.sessionId(), replacement);
        });
    }

    @Override
    public Activation activate(LoginIdentity identity, UUID enrollment, String code, Instant now) {
        return tx.execute(status -> {
            Owner owner = lockOwner(identity, now, false);
            Factor factor = factor(identity.userId());
            if (factor == null || factor.pending() == null || !enrollment.equals(factor.enrollment())
                    || !identity.sessionId().equals(factor.session()) || !factor.expires().isAfter(now)) { throw denied(); }
            if (factor.replacement()) { lockOwner(identity, now, true); }
            String secret = cipher.decrypt(factor.pending(), context(identity.userId()));
            long step = totp.acceptedStep(secret, code, now, -1);
            if (step < 0) { throw denied(); }
            jdbc.update("UPDATE app.mfa_credential SET secret_encrypted=?,activated_at=?,last_step=?,pending_secret=NULL, "
                    + "pending_expires_at=NULL,enrollment_id=NULL,pending_session_id=NULL,pending_replacement=false WHERE user_id=?",
                    cipher.encrypt(secret, context(identity.userId())), ts(now), step, identity.userId());
            List<String> codes = freshCodes(identity.userId(), now);
            if (factor.replacement()) {
                jdbc.update("UPDATE app.app_user SET security_version=security_version+1,updated_at=? WHERE user_id=?", ts(now), identity.userId());
                jdbc.update("UPDATE app.auth_session SET revoked_at=? WHERE user_id=? AND revoked_at IS NULL", ts(now), identity.userId());
                jdbc.update("DELETE FROM app.spring_session WHERE principal_name=?", identity.userId().toString());
            } else { stamp(identity, now); }
            audit.record(SecurityAudit.Action.MFA_ENROLLED, identity.userId(), identity.userId());
            mail.enqueue(owner.email(), "Votre double authentification a été configurée", "Le facteur TOTP de votre compte a été configuré. "
                    + "Conservez vos codes de récupération hors ligne. Si cette action est inattendue, contactez l'opérateur.", now.plusSeconds(86400));
            return new Activation(codes, factor.replacement());
        });
    }

    @Override
    public void prove(LoginIdentity identity, String code, Instant now, boolean recovery) {
        tx.executeWithoutResult(status -> {
            lockOwner(identity, now, false);
            Factor factor = factor(identity.userId());
            if (factor == null || factor.active() == null) { throw denied(); }
            if (recovery) {
                if (code == null || !code.matches("[A-Za-z0-9_-]{43}")) { throw denied(); }
                int changed = jdbc.update("UPDATE app.recovery_code SET consumed_at=? WHERE user_id=? AND fingerprint=? AND consumed_at IS NULL",
                        ts(now), identity.userId(), LoginAttempts.digest(code));
                if (changed != 1) { throw denied(); }
            } else {
                long step = totp.acceptedStep(cipher.decrypt(factor.active(), context(identity.userId())), code, now, factor.lastStep());
                if (step < 0) { throw denied(); }
                jdbc.update("UPDATE app.mfa_credential SET last_step=? WHERE user_id=?", step, identity.userId());
            }
            stamp(identity, now);
            audit.record(recovery ? SecurityAudit.Action.MFA_RECOVERY_USED : SecurityAudit.Action.MFA_PROVED, identity.userId(), identity.sessionId());
        });
    }

    @Override
    public List<String> regenerateRecovery(LoginIdentity identity, Instant now) {
        return tx.execute(status -> {
            Owner owner = lockOwner(identity, now, true);
            List<String> codes = freshCodes(identity.userId(), now);
            audit.record(SecurityAudit.Action.MFA_RECOVERY_REPLACED, identity.userId(), identity.userId());
            mail.enqueue(owner.email(), "Vos codes de récupération ont été renouvelés", "Les anciens codes de récupération de votre compte ne sont plus valides.",
                    now.plusSeconds(86400));
            return codes;
        });
    }

    private List<String> freshCodes(UUID user, Instant now) {
        jdbc.update("UPDATE app.recovery_code SET consumed_at=? WHERE user_id=? AND consumed_at IS NULL", ts(now), user);
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[32]; random.nextBytes(bytes);
            String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            jdbc.update("INSERT INTO app.recovery_code(user_id,fingerprint,created_at) VALUES (?,?,?)", user, LoginAttempts.digest(code), ts(now));
            codes.add(code);
        }
        return List.copyOf(codes);
    }

    private void stamp(LoginIdentity identity, Instant now) {
        jdbc.update("UPDATE app.auth_session SET mfa_at=? WHERE session_id=?", ts(now), identity.sessionId());
    }

    @Override
    public LoginIdentity reauthenticate(LoginIdentity identity, String expectedHash, Instant now) {
        return tx.execute(status -> {
            jdbc.queryForObject("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", UUID.class, identity.userId());
            int updated = jdbc.update("UPDATE app.auth_session s SET authenticated_at=? FROM app.app_user u,app.local_credential c "
                            + "WHERE s.session_id=? AND s.user_id=? AND u.user_id=s.user_id AND c.user_id=u.user_id "
                            + "AND u.status IN ('ACTIVE','PENDING') AND u.security_version=? AND s.security_version=u.security_version "
                            + "AND s.revoked_at IS NULL AND s.expires_at>? AND c.password_hash=?", ts(now), identity.sessionId(), identity.userId(),
                    identity.securityVersion(), ts(now), expectedHash);
            if (updated != 1) { throw denied(); }
            audit.record(SecurityAudit.Action.REAUTHENTICATED, identity.userId(), identity.sessionId());
            return new LoginIdentity(identity.userId(), identity.sessionId(), identity.securityVersion(), now);
        });
    }

    private static String context(UUID id) { return "identity-totp/" + id; }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private static IdentityFailure denied() { return new IdentityFailure(IdentityFailure.Reason.FORBIDDEN); }
}
