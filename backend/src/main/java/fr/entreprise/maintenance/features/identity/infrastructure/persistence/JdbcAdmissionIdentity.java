package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.application.IdentityPolicy;
import fr.entreprise.maintenance.features.identity.application.LoginAttempts;
import fr.entreprise.maintenance.features.identity.domain.EmailAddress;
import fr.entreprise.maintenance.features.identity.publicapi.AdmissionIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcAdmissionIdentity implements AdmissionIdentity {
    private record Candidate(Applicant applicant,boolean allowedRole) { }
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final IdentityPolicy policy;
    private final LoginAttempts limits;
    public JdbcAdmissionIdentity(JdbcTemplate jdbc, Clock clock, IdentityPolicy policy, LoginAttempts limits) {
        this.jdbc = jdbc; this.clock = clock; this.policy = policy; this.limits = limits;
    }
    private void lock(UUID first, UUID second) {
        // All admission commands: sorted users -> dossier/invitation -> child rows/receipt/outbox.
        if (second == null || first.equals(second)) { jdbc.queryForList("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE", first); }
        else { jdbc.queryForList("SELECT user_id FROM app.app_user WHERE user_id IN (?,?) ORDER BY user_id FOR UPDATE", first, second); }
    }
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public Applicant applicant(IdentitySession session, Channel channel, Role role, boolean verified) {
        if (session == null) { throw refused(); }
        lock(session.userId(), null);
        var result = jdbc.query("SELECT c.email_normalized,c.email_verified_at, "
                        + "(EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role=?) "
                        + "AND NOT EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role<>?)) AS allowed_role "
                        + "FROM app.app_user u JOIN app.local_credential c USING(user_id) "
                        + "JOIN app.auth_session s USING(user_id) WHERE u.user_id=? AND s.session_id=? AND s.channel=? AND s.revoked_at IS NULL "
                        + "AND s.expires_at>? AND s.security_version=u.security_version AND u.security_version=? AND u.status IN ('PENDING','ACTIVE')",
                (row, index) -> new Candidate(new Applicant(session.userId(), row.getString(1), row.getTimestamp(2) != null),row.getBoolean(3)),role.name(),role.name(),session.userId(),
                session.sessionId(), channel.name(), Timestamp.from(clock.instant()), session.securityVersion());
        if (result.isEmpty()) { throw refused(); }
        if (!result.getFirst().allowedRole() || (verified && !result.getFirst().applicant().emailVerified())) { throw new IdentityFailure(IdentityFailure.Reason.FORBIDDEN); }
        return result.getFirst().applicant();
    }
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void administrator(IdentitySession session, UUID targetUser) {
        if (session == null) { throw refused(); }
        lock(session.userId(), targetUser);
        checkAdministrator(session);
    }
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void checkAdministrator(IdentitySession session) {
        if (session == null) { throw refused(); }
        Boolean allowed = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app.app_user u JOIN app.local_credential c USING(user_id) "
                + "JOIN app.auth_session s USING(user_id) WHERE u.user_id=? AND s.session_id=? AND s.channel='WEB' AND s.revoked_at IS NULL "
                + "AND s.expires_at>? AND s.authenticated_at>? AND s.mfa_at>? AND c.email_verified_at IS NOT NULL "
                + "AND u.status='ACTIVE' AND s.security_version=u.security_version AND u.security_version=? "
                + "AND EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role='ADMIN'))", Boolean.class,
                session.userId(), session.sessionId(), Timestamp.from(clock.instant()), Timestamp.from(clock.instant().minus(policy.recentAuthentication())),
                Timestamp.from(clock.instant().minus(policy.recentAuthentication())), session.securityVersion());
        if (!Boolean.TRUE.equals(allowed)) { throw new IdentityFailure(IdentityFailure.Reason.FORBIDDEN); }
    }
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public String applicantEmail(UUID user, Role role) {
        var rows = jdbc.query("SELECT c.email_normalized FROM app.app_user u JOIN app.local_credential c USING(user_id) "
                        + "WHERE u.user_id=? AND u.status='PENDING' AND c.email_verified_at IS NOT NULL "
                        + "AND EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role=?) "
                        + "AND NOT EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role<>?) FOR UPDATE OF u",
                (row, index) -> row.getString(1), user, role.name(),role.name());
        if (rows.isEmpty()) { throw new IdentityFailure(IdentityFailure.Reason.CONFLICT); }
        return rows.getFirst();
    }
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void approveClient(UUID user) {
        // ACTIVE is global: never activate an additional role through a client decision.
        applicantEmail(user, Role.CLIENT);
        jdbc.update("UPDATE app.app_user SET status='ACTIVE',updated_at=? WHERE user_id=?", Timestamp.from(clock.instant()), user);
        // No version bump: current sessions load the new status on their next request.
    }
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void rejectApplicant(UUID user, Role role) {
        applicantEmail(user, role);
        jdbc.update("UPDATE app.app_user SET status='REJECTED',security_version=security_version+1,updated_at=? WHERE user_id=?", Timestamp.from(clock.instant()), user);
        jdbc.update("UPDATE app.auth_session SET revoked_at=? WHERE user_id=? AND revoked_at IS NULL", Timestamp.from(clock.instant()), user);
        jdbc.update("DELETE FROM app.spring_session WHERE principal_name=?", user.toString());
    }
    @Override public String normalizeEmail(String email) { return EmailAddress.parse(email).normalized(); }
    @Override public void limit(String operation, String identity, String peer) { limits.onboarding(operation, identity, peer); }
    private static IdentityFailure refused() { return new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
}
