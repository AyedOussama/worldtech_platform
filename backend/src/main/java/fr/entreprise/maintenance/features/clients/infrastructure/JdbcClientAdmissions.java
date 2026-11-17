package fr.entreprise.maintenance.features.clients.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.clients.domain.ClientAdmissions;
import fr.entreprise.maintenance.features.clients.domain.ClientDossier;
import fr.entreprise.maintenance.features.clients.domain.ClientFailure;
import fr.entreprise.maintenance.features.clients.domain.ClientProfile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClientAdmissions implements ClientAdmissions {
    private final JdbcTemplate jdbc;
    private final java.time.Clock clock;
    public JdbcClientAdmissions(JdbcTemplate jdbc, java.time.Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    @Override public Invitation invite(String email, String display, UUID actor, String fingerprint, Instant now, Instant expires) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO app.client_invitation(invitation_id,email_normalized,email_display,invited_by,last_sent_by,created_at,expires_at) VALUES (?,?,?,?,?,?,?)",
                    id, email, display, actor, actor, ts(now), ts(expires));
        } catch (DuplicateKeyException failure) { throw new ClientFailure(ClientFailure.Code.CLIENT_STATE_CONFLICT); }
        link(id, fingerprint, now, expires);
        return new Invitation(id, email, expires, 0, false);
    }
    private void link(UUID id, String fingerprint, Instant now, Instant expires) {
        jdbc.update("INSERT INTO app.client_invitation_link(fingerprint,invitation_id,created_at,expires_at) VALUES (?,?,?,?)", fingerprint, id, ts(now), ts(expires));
    }
    @Override public Invitation invitation(UUID id) {
        return jdbc.query("SELECT * FROM app.client_invitation WHERE invitation_id=?", JdbcClientAdmissions::invitation, id)
                .stream().findFirst().orElseThrow(() -> new ClientFailure(ClientFailure.Code.CLIENT_DOSSIER_NOT_FOUND));
    }
    @Override public Invitation resend(UUID id, UUID actor, long expected, String fingerprint, Instant now, Instant expires) {
        var rows = jdbc.query("SELECT * FROM app.client_invitation WHERE invitation_id=? FOR UPDATE", JdbcClientAdmissions::invitation, id);
        if (rows.isEmpty()) { throw new ClientFailure(ClientFailure.Code.CLIENT_DOSSIER_NOT_FOUND); }
        var old = rows.getFirst();
        if (old.version()!=expected) { throw new ClientFailure(ClientFailure.Code.CLIENT_VERSION_STALE); }
        if (old.accepted()) { throw new ClientFailure(ClientFailure.Code.CLIENT_STATE_CONFLICT); }
        jdbc.update("UPDATE app.client_invitation_link SET revoked_at=? WHERE invitation_id=? AND revoked_at IS NULL AND consumed_at IS NULL", ts(now), id);
        jdbc.update("UPDATE app.client_invitation SET expires_at=?,last_sent_by=?,version=version+1 WHERE invitation_id=?", ts(expires), actor, id);
        link(id, fingerprint, now, expires);
        return new Invitation(id, old.email(), expires, old.version()+1, false);
    }
    @Override public Link lockLink(String fingerprint, String email, Instant now) {
        var ids = jdbc.query("SELECT invitation_id FROM app.client_invitation_link WHERE fingerprint=?", (row, index) -> row.getObject(1,UUID.class), fingerprint);
        if (ids.isEmpty()) { throw new ClientFailure(ClientFailure.Code.INVALID_INVITATION); }
        jdbc.queryForList("SELECT invitation_id FROM app.client_invitation WHERE invitation_id=? FOR UPDATE", ids.getFirst());
        now = clock.instant(); // Recheck the deadline after waiting for the authoritative invitation lock.
        var rows = jdbc.query("SELECT i.invitation_id,i.email_normalized FROM app.client_invitation i JOIN app.client_invitation_link l USING(invitation_id) "
                        + "WHERE l.fingerprint=? AND i.email_normalized=? AND i.accepted_at IS NULL AND i.expires_at>? "
                        + "AND l.expires_at>? AND l.consumed_at IS NULL AND l.revoked_at IS NULL FOR UPDATE OF l",
                (row, index) -> new Link(row.getObject(1, UUID.class), row.getString(2)), fingerprint, email, ts(now), ts(now));
        if (rows.isEmpty()) { throw new ClientFailure(ClientFailure.Code.INVALID_INVITATION); }
        return rows.getFirst();
    }
    @Override public void checkLink(String fingerprint, String email, Instant now) {
        Boolean valid = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app.client_invitation i JOIN app.client_invitation_link l USING(invitation_id) "
                + "WHERE l.fingerprint=? AND i.email_normalized=? AND i.accepted_at IS NULL AND i.expires_at>? AND l.expires_at>? "
                + "AND l.consumed_at IS NULL AND l.revoked_at IS NULL)",Boolean.class,fingerprint,email,ts(now),ts(now));
        if (!Boolean.TRUE.equals(valid)) { throw new ClientFailure(ClientFailure.Code.INVALID_INVITATION); }
    }
    @Override public ClientDossier accept(Link link, String fingerprint, UUID user, Instant now) {
        UUID dossier = UUID.randomUUID();
        jdbc.update("UPDATE app.client_invitation SET accepted_at=?,accepted_user_id=?,version=version+1 WHERE invitation_id=?", ts(now), user, link.invitationId());
        jdbc.update("UPDATE app.client_invitation_link SET consumed_at=? WHERE fingerprint=?", ts(now), fingerprint);
        try { jdbc.update("INSERT INTO app.client_onboarding(dossier_id,user_id,invitation_id,created_at,updated_at) VALUES (?,?,?,?,?)", dossier, user, link.invitationId(), ts(now), ts(now)); }
        catch (DuplicateKeyException failure) { throw new ClientFailure(ClientFailure.Code.CLIENT_STATE_CONFLICT); }
        var result = lock(dossier); history(result, user, "CREATED", null, now); return result;
    }
    @Override public ClientDossier own(UUID user) {
        return jdbc.query("SELECT * FROM app.client_onboarding WHERE user_id=? FOR UPDATE", JdbcClientAdmissions::dossier, user)
                .stream().findFirst().orElseThrow(() -> new ClientFailure(ClientFailure.Code.CLIENT_DOSSIER_NOT_FOUND));
    }
    @Override public UUID owner(UUID dossier) {
        return jdbc.query("SELECT user_id FROM app.client_onboarding WHERE dossier_id=?", (row, index) -> row.getObject(1,UUID.class), dossier)
                .stream().findFirst().orElseThrow(() -> new ClientFailure(ClientFailure.Code.CLIENT_DOSSIER_NOT_FOUND));
    }
    @Override public ClientDossier lock(UUID dossier) {
        return jdbc.query("SELECT * FROM app.client_onboarding WHERE dossier_id=? FOR UPDATE", JdbcClientAdmissions::dossier, dossier)
                .stream().findFirst().orElseThrow(() -> new ClientFailure(ClientFailure.Code.CLIENT_DOSSIER_NOT_FOUND));
    }
    @Override public List<ClientDossier> review(long offset, int limit, ClientDossier.State state) {
        return jdbc.query("SELECT * FROM app.client_onboarding WHERE state=? ORDER BY created_at,dossier_id LIMIT ? OFFSET ?",
                JdbcClientAdmissions::dossier, state.name(), limit, offset);
    }
    @Override public List<History> history(UUID dossier, long after, int limit) {
        return jdbc.query("SELECT revision,actor_user_id,action,reason,occurred_at FROM app.client_onboarding_history WHERE dossier_id=? AND revision>? ORDER BY revision LIMIT ?",
                (row,index) -> new History(row.getLong(1), row.getObject(2,UUID.class),row.getString(3),row.getString(4),row.getTimestamp(5).toInstant()), dossier,after,limit);
    }
    @Override public ClientDossier save(ClientDossier current, ClientProfile profile, UUID actor, Instant now) {
        jdbc.update("UPDATE app.client_onboarding SET account_type=?,first_name=?,last_name=?,phone=?,address=?,organization_name=?,version=version+1,updated_at=? WHERE dossier_id=?",
                profile.accountType()==null ? null : profile.accountType().name(),profile.firstName(),profile.lastName(),profile.phone(),profile.address(),profile.organizationName(),ts(now),current.dossierId());
        var next = lock(current.dossierId()); history(next, actor, "DRAFT_SAVED", null, now); return next;
    }
    @Override public ClientDossier transition(ClientDossier current, ClientDossier.State next, UUID actor, String reason, Instant now) {
        jdbc.update("UPDATE app.client_onboarding SET state=?,version=version+1,updated_at=? WHERE dossier_id=?",next.name(),ts(now),current.dossierId());
        var result = lock(current.dossierId()); history(result,actor,next.name(),reason,now); return result;
    }
    private void history(ClientDossier dossier, UUID actor, String action, String reason, Instant now) {
        ClientProfile p = dossier.profile();
        jdbc.update("INSERT INTO app.client_onboarding_history(event_id,dossier_id,revision,actor_user_id,action,reason,occurred_at,account_type,first_name,last_name,phone,address,organization_name) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),dossier.dossierId(),dossier.version(),actor,action,reason,ts(now),
                p.accountType()==null ? null : p.accountType().name(),p.firstName(),p.lastName(),p.phone(),p.address(),p.organizationName());
    }
    private static Invitation invitation(ResultSet row, int index) throws SQLException {
        return new Invitation(row.getObject("invitation_id",UUID.class),row.getString("email_normalized"),row.getTimestamp("expires_at").toInstant(),row.getLong("version"),row.getTimestamp("accepted_at")!=null);
    }
    private static ClientDossier dossier(ResultSet row, int index) throws SQLException {
        String type = row.getString("account_type");
        return new ClientDossier(row.getObject("dossier_id",UUID.class),row.getObject("user_id",UUID.class),ClientDossier.State.valueOf(row.getString("state")),
                new ClientProfile(type==null ? null : ClientProfile.Type.valueOf(type),row.getString("first_name"),row.getString("last_name"),row.getString("phone"),row.getString("address"),row.getString("organization_name")),
                row.getLong("version"),row.getTimestamp("created_at").toInstant());
    }
    private static Timestamp ts(Instant now) { return Timestamp.from(now); }
}
