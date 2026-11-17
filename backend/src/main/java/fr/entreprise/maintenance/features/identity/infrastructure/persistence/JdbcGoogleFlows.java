package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.application.IdentityPolicy;
import fr.entreprise.maintenance.features.identity.domain.GoogleFlows;
import fr.entreprise.maintenance.features.identity.domain.GoogleSubject;
import fr.entreprise.maintenance.features.identity.domain.GoogleAdmission;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.IdentityStore;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.MobileTokenStore;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import fr.entreprise.maintenance.features.notifications.publicapi.MailOutbox;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcGoogleFlows implements GoogleFlows {
    private record Account(UUID id, long version) { }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final IdentityPolicy policy;
    private final IdentityStore identities;
    private final MobileTokenStore mobile;
    private final SecurityAudit audit;
    private final MailOutbox mail;
    private final GoogleAdmission admission;
    public JdbcGoogleFlows(JdbcTemplate jdbc, PlatformTransactionManager manager, IdentityPolicy policy,
            IdentityStore identities, MobileTokenStore mobile, SecurityAudit audit, MailOutbox mail,GoogleAdmission admission) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.policy = policy;
        this.identities = identities; this.mobile = mobile; this.audit = audit; this.mail = mail;
        this.admission=admission;
    }

    private String recent(LoginIdentity identity, Channel channel, Instant now) {
        if (identity == null) { throw refused(); }
        jdbc.queryForObject("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", UUID.class, identity.userId());
        var emails = jdbc.query("SELECT c.email_normalized FROM app.auth_session s JOIN app.app_user u USING(user_id) "
                        + "JOIN app.local_credential c USING(user_id) WHERE s.session_id=? AND u.user_id=? AND s.channel=? "
                        + "AND s.revoked_at IS NULL AND s.expires_at>? AND s.authenticated_at>? AND u.status IN ('PENDING','ACTIVE') "
                        + "AND u.security_version=? AND s.security_version=u.security_version AND c.email_verified_at IS NOT NULL "
                        + "AND (NOT EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role='ADMIN') OR s.mfa_at>?)",
                (row, index) -> row.getString(1), identity.sessionId(), identity.userId(), channel.name(), ts(now),
                ts(now.minus(policy.recentAuthentication())), identity.securityVersion(), ts(now.minus(policy.recentAuthentication())));
        if (emails.isEmpty()) { throw refused(); }
        return emails.getFirst();
    }

    @Override
    public void create(Transaction transaction, String launch, String browser, Instant now) {
        tx.executeWithoutResult(status -> {
            if (transaction.intention() == Intention.LINK) { recent(source(transaction, now), transaction.channel(), now); }
            jdbc.update("INSERT INTO app.google_oauth_transaction(transaction_id,channel,intention,user_id,source_session_id,security_version, "
                            + "browser_fingerprint,launch_fingerprint,pkce_challenge,destination_id,created_at,expires_at,registration_kind,registration_email,invitation_fingerprint) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    transaction.id(), transaction.channel().name(), transaction.intention().name(), transaction.userId(), transaction.sourceSession(),
                    transaction.securityVersion(), browser, launch, transaction.challenge(), transaction.destinationId(), ts(now), ts(transaction.expiresAt()),
                    transaction.registration()==null ? null : transaction.registration().name(),transaction.registrationEmail(),transaction.invitationFingerprint());
        });
    }

    @Override
    public Transaction launch(String launch, String browser, Instant now) {
        var rows = jdbc.query("UPDATE app.google_oauth_transaction SET launched_at=?,browser_fingerprint=? WHERE launch_fingerprint=? "
                        + "AND (browser_fingerprint IS NULL OR browser_fingerprint=?) AND launched_at IS NULL AND expires_at>? RETURNING *",
                JdbcGoogleFlows::transaction, ts(now), browser, launch, browser, ts(now));
        if (rows.isEmpty()) { throw refused(); }
        return rows.getFirst();
    }

    @Override
    public void saveAuthorization(UUID transaction, String state, String encrypted, Instant now) {
        if (jdbc.update("UPDATE app.google_oauth_transaction SET state_fingerprint=?,authorization_encrypted=? WHERE transaction_id=? "
                + "AND state_fingerprint IS NULL AND launched_at IS NOT NULL AND consumed_at IS NULL AND expires_at>?",
                state, encrypted, transaction, ts(now)) != 1) { throw refused(); }
    }

    @Override
    public Authorization authorization(String state, String browser, boolean consume, Instant now) {
        return tx.execute(status -> {
            var rows = jdbc.query("SELECT * FROM app.google_oauth_transaction WHERE state_fingerprint=? AND browser_fingerprint=? "
                            + "AND consumed_at IS NULL AND expires_at>? AND authorization_encrypted IS NOT NULL FOR UPDATE",
                    (row, index) -> new Authorization(transaction(row, index), row.getString("authorization_encrypted")), state, browser, ts(now));
            if (rows.isEmpty()) { return null; }
            var selected = rows.getFirst();
            if (consume) { jdbc.update("UPDATE app.google_oauth_transaction SET consumed_at=?,authorization_encrypted=NULL WHERE transaction_id=?",
                    ts(now), selected.transaction().id()); }
            return selected;
        });
    }

    private Transaction completedTransaction(UUID id, Instant now) {
        var rows = jdbc.query("UPDATE app.google_oauth_transaction SET completed_at=? WHERE transaction_id=? "
                        + "AND consumed_at IS NOT NULL AND completed_at IS NULL AND expires_at>? RETURNING *",
                JdbcGoogleFlows::transaction, ts(now), id, ts(now));
        if (rows.isEmpty()) { throw refused(); }
        return rows.getFirst();
    }

    @Override
    public Completion complete(UUID id, GoogleSubject subject, String verifiedEmail, LoginIdentity browserIdentity, Instant now) {
        try {
            return tx.execute(status -> {
                Transaction transaction = completedTransaction(id, now);
                Account registered=transaction.registration()==null ? null : register(transaction,subject,verifiedEmail,now);
                if (transaction.channel() == Channel.WEB) {
                    if (transaction.intention() == Intention.LINK) {
                        sameSource(transaction, browserIdentity);
                        link(browserIdentity, Channel.WEB, subject, now);
                        return new Completion(browserIdentity, null, null, id);
                    }
                    Account account = registered==null ? account(subject) : registered;
                    return new Completion(identities.finishExternalLogin(account.id(), account.version(), now, now.plus(policy.webAbsolute()), "WEB"),
                            null, null, id);
                }
                Account account = transaction.intention() == Intention.LOGIN ? (registered==null ? account(subject) : registered)
                        : new Account(transaction.userId(), transaction.securityVersion());
                if (transaction.intention() == Intention.LINK) { recent(source(transaction, now), Channel.MOBILE, now); }
                String raw = OpaqueTokens.generate();
                jdbc.update("INSERT INTO app.mobile_handoff(fingerprint,transaction_id,issuer,subject,user_id,security_version,created_at,expires_at) "
                        + "VALUES (?,?,?,?,?,?,?,?)", OpaqueTokens.fingerprint(raw), id, subject.issuer(), subject.subject(), account.id(), account.version(),
                        ts(now), ts(now.plusSeconds(60)));
                return new Completion(null, raw, transaction.destinationId(), id);
            });
        } catch (DuplicateKeyException failure) { throw new IdentityFailure(IdentityFailure.Reason.CONFLICT); }
    }

    private Account account(GoogleSubject subject) {
        var owners = jdbc.query("SELECT user_id FROM app.external_identity WHERE provider='google' AND issuer=? AND subject=?",
                (row, index) -> row.getObject(1, UUID.class), subject.issuer(), subject.subject());
        if (owners.isEmpty()) { throw refused(); } // No lookup/merge by email, no implicit registration.
        UUID id = owners.getFirst();
        jdbc.queryForObject("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", UUID.class, id);
        var rows = jdbc.query("SELECT u.security_version FROM app.app_user u JOIN app.external_identity e USING(user_id) "
                        + "WHERE u.user_id=? AND u.status IN ('PENDING','ACTIVE') AND e.provider='google' AND e.issuer=? AND e.subject=?",
                (row, index) -> new Account(id, row.getLong(1)), id, subject.issuer(), subject.subject());
        if (rows.isEmpty()) { throw refused(); }
        return rows.getFirst();
    }

    private Account register(Transaction transaction,GoogleSubject subject,String verifiedEmail,Instant now) {
        var email=EmailAddress.parse(verifiedEmail);
        if (transaction.registration()==Registration.CLIENT && !email.normalized().equals(transaction.registrationEmail())) { throw refused(); }
        // Match the local registration order: invitation -> new user/credential. Avoid a credential/invitation deadlock.
        if (transaction.registration()==Registration.CLIENT) { admission.lockClientInvitation(transaction.invitationFingerprint(),email.normalized()); }
        UUID user=UUID.randomUUID();
        // Uniqueness rejects both a preexisting local mailbox and a preexisting external triplet, never merging either.
        jdbc.update("INSERT INTO app.app_user(user_id,status,created_at,updated_at) VALUES (?,'PENDING',?,?)",user,ts(now),ts(now));
        jdbc.update("INSERT INTO app.local_credential(user_id,email_normalized,email_display,password_hash,email_verified_at,updated_at) VALUES (?,?,?,'',?,?)",
                user,email.normalized(),email.display(),ts(now),ts(now));
        jdbc.update("INSERT INTO app.user_role(user_id,role) VALUES (?,?)",user,transaction.registration().name());
        jdbc.update("INSERT INTO app.external_identity(identity_id,user_id,provider,issuer,subject,created_at) VALUES (?,?,'google',?,?,?)",UUID.randomUUID(),user,subject.issuer(),subject.subject(),ts(now));
        if (transaction.registration()==Registration.CLIENT) { admission.client(user,transaction.invitationFingerprint(),email.normalized()); }
        else { admission.technician(user); }
        audit.record(SecurityAudit.Action.GOOGLE_REGISTERED,user,user);
        return new Account(user,0);
    }

    private void link(LoginIdentity identity, Channel channel, GoogleSubject subject, Instant now) {
        String email = recent(identity, channel, now);
        jdbc.update("INSERT INTO app.external_identity(identity_id,user_id,provider,issuer,subject,created_at) VALUES (?,?,'google',?,?,?) "
                + "ON CONFLICT DO NOTHING", UUID.randomUUID(), identity.userId(), subject.issuer(), subject.subject(), ts(now));
        Boolean owns = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app.external_identity WHERE provider='google' AND issuer=? AND subject=? AND user_id=?)",
                Boolean.class, subject.issuer(), subject.subject(), identity.userId());
        if (!Boolean.TRUE.equals(owns)) { throw new IdentityFailure(IdentityFailure.Reason.CONFLICT); }
        audit.record(SecurityAudit.Action.GOOGLE_LINKED, identity.userId(), identity.userId());
        mail.enqueue(email, "Une identité Google a été associée", "Une identité Google a été associée à votre compte après confirmation. "
                + "Si cette action est inattendue, contactez l'opérateur.", now.plusSeconds(86400));
    }

    @Override
    public Exchange exchange(UUID id, String fingerprint, String challenge, LoginIdentity mobileIdentity, Instant now) {
        return tx.execute(status -> {
            var rows = jdbc.query("SELECT t.*,h.issuer,h.subject,h.user_id AS handoff_user,h.security_version AS handoff_version "
                            + "FROM app.mobile_handoff h JOIN app.google_oauth_transaction t USING(transaction_id) "
                            + "WHERE h.transaction_id=? AND h.fingerprint=? AND h.consumed_at IS NULL AND h.expires_at>? "
                            + "AND t.expires_at>? AND t.channel='MOBILE' AND t.completed_at IS NOT NULL FOR UPDATE OF h,t",
                    (row, index) -> new Object[]{transaction(row, index), new GoogleSubject(row.getString("issuer"), row.getString("subject")),
                            new Account(row.getObject("handoff_user", UUID.class), row.getLong("handoff_version"))}, id, fingerprint, ts(now), ts(now));
            if (rows.isEmpty()) { throw refused(); }
            Transaction transaction = (Transaction) rows.getFirst()[0];
            if (!java.security.MessageDigest.isEqual(transaction.challenge().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    challenge.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) { throw refused(); }
            GoogleSubject subject = (GoogleSubject) rows.getFirst()[1];
            Account expected = (Account) rows.getFirst()[2];
            MobileTokenStore.Pair pair = null;
            if (transaction.intention() == Intention.LINK) {
                sameSource(transaction, mobileIdentity); link(mobileIdentity, Channel.MOBILE, subject, now);
            } else {
                Account current = account(subject);
                if (!current.equals(expected)) { throw refused(); }
                pair = mobile.issueExternal(current.id(), current.version(), now);
            }
            jdbc.update("UPDATE app.mobile_handoff SET consumed_at=? WHERE fingerprint=?", ts(now), fingerprint);
            audit.record(SecurityAudit.Action.GOOGLE_HANDOFF, expected.id(), id);
            return new Exchange(pair, transaction.intention() == Intention.LINK);
        });
    }

    @Override
    public void unlink(LoginIdentity identity, Channel channel, Instant now) {
        tx.executeWithoutResult(status -> {
            String email = recent(identity, channel, now);
            Boolean alternative = jdbc.queryForObject("SELECT password_hash IS NOT NULL AND length(password_hash)>0 AND email_verified_at IS NOT NULL "
                    + "FROM app.local_credential WHERE user_id=?", Boolean.class, identity.userId());
            if (!Boolean.TRUE.equals(alternative)) { throw new IdentityFailure(IdentityFailure.Reason.CONFLICT); }
            jdbc.update("DELETE FROM app.external_identity WHERE user_id=? AND provider='google'", identity.userId());
            jdbc.update("UPDATE app.app_user SET security_version=security_version+1,updated_at=? WHERE user_id=?", ts(now), identity.userId());
            jdbc.update("UPDATE app.auth_session SET revoked_at=? WHERE user_id=? AND revoked_at IS NULL", ts(now), identity.userId());
            jdbc.update("DELETE FROM app.spring_session WHERE principal_name=?", identity.userId().toString());
            audit.record(SecurityAudit.Action.GOOGLE_UNLINKED, identity.userId(), identity.userId());
            mail.enqueue(email, "L'identité Google a été dissociée", "L'identité Google a été dissociée de votre compte. Reconnectez-vous avec votre mot de passe.",
                    now.plusSeconds(86400));
        });
    }

    private static Transaction transaction(ResultSet row, int index) throws SQLException {
        return new Transaction(row.getObject("transaction_id", UUID.class), Channel.valueOf(row.getString("channel")),
                Intention.valueOf(row.getString("intention")), row.getObject("user_id", UUID.class), row.getObject("source_session_id", UUID.class),
                row.getObject("security_version", Long.class), row.getString("pkce_challenge"), row.getString("destination_id"), row.getTimestamp("expires_at").toInstant(),
                row.getString("registration_kind")==null ? null : Registration.valueOf(row.getString("registration_kind")),row.getString("registration_email"),row.getString("invitation_fingerprint"));
    }
    private static LoginIdentity source(Transaction transaction, Instant now) {
        return new LoginIdentity(transaction.userId(), transaction.sourceSession(), transaction.securityVersion(), now);
    }
    private static void sameSource(Transaction transaction, LoginIdentity current) {
        if (current == null || !current.userId().equals(transaction.userId()) || !current.sessionId().equals(transaction.sourceSession())
                || current.securityVersion() != transaction.securityVersion()) { throw refused(); }
    }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private static IdentityFailure refused() { return new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
}
