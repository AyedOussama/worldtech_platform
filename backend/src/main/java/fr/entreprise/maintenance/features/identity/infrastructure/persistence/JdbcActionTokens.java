package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.domain.ActionTokens;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.notifications.publicapi.MailOutbox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcActionTokens implements ActionTokens {
    private record Owner(UUID id, String email) { }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final MailOutbox outbox;
    private final SecurityAudit audit;
    private final URI origin;

    public JdbcActionTokens(JdbcTemplate jdbc, PlatformTransactionManager manager, MailOutbox outbox, SecurityAudit audit,
            @Value("${app.identity.frontend-origin}") URI origin) {
        if (origin.getHost() == null || origin.getUserInfo() != null || origin.getQuery() != null || origin.getFragment() != null
                || !("https".equals(origin.getScheme()) || ("http".equals(origin.getScheme())
                && java.util.Set.of("localhost", "127.0.0.1", "[::1]").contains(origin.getHost())))) {
            throw new IllegalArgumentException("A controlled frontend origin is required.");
        }
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.outbox = outbox; this.audit = audit; this.origin = origin;
    }

    @Override
    public void issue(String email, Purpose purpose, String fingerprint, String raw, Instant now, Instant expires) {
        tx.executeWithoutResult(status -> {
            var owners = jdbc.query("SELECT u.user_id,c.email_normalized,c.email_verified_at,c.password_hash FROM app.app_user u "
                            + "JOIN app.local_credential c USING(user_id) WHERE c.email_normalized=? FOR UPDATE OF u,c",
                    (row, index) -> new Object[]{new Owner(row.getObject("user_id", UUID.class), row.getString("email_normalized")),
                            row.getTimestamp("email_verified_at") != null,row.getString("password_hash")}, email);
            if (owners.isEmpty() || (purpose == Purpose.EMAIL_VERIFICATION && (Boolean) owners.getFirst()[1])) { return; }
            if (purpose==Purpose.PASSWORD_RESET && ((String)owners.getFirst()[2]).isEmpty()) { return; }
            Owner owner = (Owner) owners.getFirst()[0];
            jdbc.update("UPDATE app.action_token SET consumed_at=? WHERE user_id=? AND purpose=? AND consumed_at IS NULL",
                    ts(now), owner.id(), purpose.name());
            jdbc.update("INSERT INTO app.action_token(token_id,user_id,fingerprint,purpose,email_normalized,created_at,expires_at) VALUES (?,?,?,?,?,?,?)",
                    UUID.randomUUID(), owner.id(), fingerprint, purpose.name(), owner.email(), ts(now), ts(expires));
            String path = purpose == Purpose.EMAIL_VERIFICATION ? "/auth/email-verification" : "/auth/password-reset";
            String link = origin.resolve(path) + "#token=" + raw;
            outbox.enqueue(owner.email(), purpose == Purpose.EMAIL_VERIFICATION ? "Vérifiez votre adresse email" : "Réinitialisez votre mot de passe",
                    "Confirmez volontairement cette demande dans l'application :\n" + link
                            + "\nNe partagez pas ce lien. Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.", expires);
        });
    }

    @Override
    public boolean usable(String fingerprint, Purpose purpose, Instant now) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM app.action_token t JOIN app.local_credential c USING(user_id) "
                + "WHERE fingerprint=? AND purpose=? AND consumed_at IS NULL AND expires_at>? AND t.email_normalized=c.email_normalized "
                + "AND (t.purpose<>'PASSWORD_RESET' OR length(c.password_hash)>0))",
                Boolean.class, fingerprint, purpose.name(), ts(now)));
    }

    private Owner lock(String fingerprint, Purpose purpose, Instant now) {
        var users = jdbc.query("SELECT user_id FROM app.action_token WHERE fingerprint=? AND purpose=?",
                (row, index) -> row.getObject(1, UUID.class), fingerprint, purpose.name());
        if (users.isEmpty()) { throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT); }
        UUID user = users.getFirst();
        jdbc.queryForObject("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", UUID.class, user);
        var tokens = jdbc.query("SELECT t.user_id,t.email_normalized FROM app.action_token t JOIN app.local_credential c USING(user_id) "
                        + "WHERE fingerprint=? AND purpose=? AND consumed_at IS NULL AND expires_at>? "
                        + "AND t.email_normalized=c.email_normalized FOR UPDATE OF t,c",
                (row, index) -> new Owner(row.getObject(1, UUID.class), row.getString(2)), fingerprint, purpose.name(), ts(now));
        if (tokens.isEmpty()) { throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT); }
        return tokens.getFirst();
    }

    private void consume(Owner owner, Purpose purpose, Instant now) {
        jdbc.update("UPDATE app.action_token SET consumed_at=? WHERE user_id=? AND purpose=? AND consumed_at IS NULL",
                ts(now), owner.id(), purpose.name());
    }

    @Override
    public void verifyEmail(String fingerprint, Instant now) {
        tx.executeWithoutResult(status -> {
            Owner owner = lock(fingerprint, Purpose.EMAIL_VERIFICATION, now);
            jdbc.update("UPDATE app.local_credential SET email_verified_at=?,updated_at=? WHERE user_id=?", ts(now), ts(now), owner.id());
            consume(owner, Purpose.EMAIL_VERIFICATION, now);
            audit.record(SecurityAudit.Action.EMAIL_VERIFIED, owner.id(), owner.id());
        });
    }

    @Override
    public void resetPassword(String fingerprint, String newHash, Instant now) {
        tx.executeWithoutResult(status -> {
            Owner owner = lock(fingerprint, Purpose.PASSWORD_RESET, now);
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT length(password_hash)=0 FROM app.local_credential WHERE user_id=?",Boolean.class,owner.id()))) {
                throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT);
            }
            jdbc.update("UPDATE app.local_credential SET password_hash=?,updated_at=? WHERE user_id=?", newHash, ts(now), owner.id());
            jdbc.update("UPDATE app.app_user SET security_version=security_version+1,updated_at=? WHERE user_id=?", ts(now), owner.id());
            jdbc.update("UPDATE app.auth_session SET revoked_at=? WHERE user_id=? AND revoked_at IS NULL", ts(now), owner.id());
            jdbc.update("DELETE FROM app.spring_session WHERE principal_name=?", owner.id().toString());
            consume(owner, Purpose.PASSWORD_RESET, now);
            audit.record(SecurityAudit.Action.PASSWORD_RESET, owner.id(), owner.id());
            outbox.enqueue(owner.email(), "Votre mot de passe a été modifié", "Le mot de passe de votre compte a été modifié. "
                    + "Si vous n'êtes pas à l'origine de cette action, contactez l'opérateur. Le MFA et le statut du compte restent inchangés.", now.plusSeconds(86400));
        });
    }

    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
}
