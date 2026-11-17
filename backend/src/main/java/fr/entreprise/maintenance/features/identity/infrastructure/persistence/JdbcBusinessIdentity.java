package fr.entreprise.maintenance.features.identity.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.publicapi.AdmissionIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.BusinessIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class JdbcBusinessIdentity implements BusinessIdentity {
    private final JdbcTemplate jdbc;
    private final AdmissionIdentity admission;
    private final Clock clock;
    public JdbcBusinessIdentity(JdbcTemplate jdbc, AdmissionIdentity admission, Clock clock) { this.jdbc=jdbc; this.admission=admission; this.clock=clock; }
    @Override public void checkAdministrator(IdentitySession session) { admission.checkAdministrator(session); }
    @Override public Actor authorize(IdentitySession session, boolean administratorOnly, UUID targetClient) {
        if(session==null) { throw fail(IdentityFailure.Reason.BAD_CREDENTIALS); }
        if(targetClient==null || targetClient.equals(session.userId())) {
            jdbc.queryForList("SELECT user_id FROM app.app_user WHERE user_id=? FOR UPDATE",session.userId());
        } else {
            jdbc.queryForList("SELECT user_id FROM app.app_user WHERE user_id IN (?,?) ORDER BY user_id FOR UPDATE",session.userId(),targetClient);
        }
        var roles=jdbc.query("SELECT r.role FROM app.app_user u JOIN app.local_credential c USING(user_id) JOIN app.auth_session s USING(user_id) "
                + "JOIN app.user_role r USING(user_id) WHERE u.user_id=? AND s.session_id=? AND s.channel='WEB' AND s.revoked_at IS NULL "
                + "AND s.expires_at>? AND s.security_version=u.security_version AND u.security_version=? AND u.status='ACTIVE' AND c.email_verified_at IS NOT NULL",
                (row,index)->row.getString(1),session.userId(),session.sessionId(),Timestamp.from(clock.instant()),session.securityVersion());
        if(roles.size()!=1 || !(roles.getFirst().equals("CLIENT") || roles.getFirst().equals("ADMIN"))) { throw fail(IdentityFailure.Reason.FORBIDDEN); }
        boolean admin=roles.getFirst().equals("ADMIN");
        if(administratorOnly && !admin) { throw fail(IdentityFailure.Reason.FORBIDDEN); }
        if(admin) { admission.checkAdministrator(session); }
        if(targetClient!=null) {
            Boolean eligible=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app.app_user u JOIN app.local_credential c USING(user_id) "
                    + "WHERE u.user_id=? AND u.status='ACTIVE' AND c.email_verified_at IS NOT NULL "
                    + "AND EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role='CLIENT') "
                    + "AND NOT EXISTS(SELECT 1 FROM app.user_role r WHERE r.user_id=u.user_id AND r.role<>'CLIENT'))",Boolean.class,targetClient);
            if(!Boolean.TRUE.equals(eligible)) { throw fail(IdentityFailure.Reason.CONFLICT); }
        }
        return new Actor(session.userId(),admin);
    }
    private static IdentityFailure fail(IdentityFailure.Reason reason) { return new IdentityFailure(reason); }
}
