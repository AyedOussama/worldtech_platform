package fr.entreprise.maintenance.features.technicians.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianAdmissions;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianDossier;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianFailure;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTechnicianAdmissions implements TechnicianAdmissions {
    private final JdbcTemplate jdbc;
    public JdbcTechnicianAdmissions(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public TechnicianDossier open(UUID user, Instant now) {
        UUID id=UUID.randomUUID();
        int inserted=jdbc.update("INSERT INTO app.technician_onboarding(dossier_id,user_id,created_at,updated_at) VALUES (?,?,?,?) ON CONFLICT(user_id) DO NOTHING",id,user,ts(now),ts(now));
        var result=own(user);
        if (inserted==1) { history(result,user,"CREATED",null,now); }
        return result;
    }
    @Override public TechnicianDossier own(UUID user) {
        return jdbc.query("SELECT * FROM app.technician_onboarding WHERE user_id=? FOR UPDATE",JdbcTechnicianAdmissions::dossier,user)
                .stream().findFirst().orElseThrow(() -> new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_DOSSIER_NOT_FOUND));
    }
    @Override public UUID owner(UUID dossier) {
        return jdbc.query("SELECT user_id FROM app.technician_onboarding WHERE dossier_id=?",(row,index) -> row.getObject(1,UUID.class),dossier)
                .stream().findFirst().orElseThrow(() -> new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_DOSSIER_NOT_FOUND));
    }
    @Override public TechnicianDossier lock(UUID dossier) {
        return jdbc.query("SELECT * FROM app.technician_onboarding WHERE dossier_id=? FOR UPDATE",JdbcTechnicianAdmissions::dossier,dossier)
                .stream().findFirst().orElseThrow(() -> new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_DOSSIER_NOT_FOUND));
    }
    @Override public TechnicianDossier save(TechnicianDossier dossier, TechnicianProfile p, UUID actor, Instant now) {
        jdbc.update("UPDATE app.technician_onboarding SET first_name=?,last_name=?,phone=?,base_city=?,radius_km=?,declared_skills=?::varchar[],version=version+1,updated_at=? WHERE dossier_id=?",
                p.firstName(),p.lastName(),p.phone(),p.baseCity(),p.radiusKm(),p.declaredSkills().toArray(String[]::new),ts(now),dossier.dossierId());
        var result=lock(dossier.dossierId()); history(result,actor,"DRAFT_SAVED",null,now); return result;
    }
    @Override public TechnicianDossier transition(TechnicianDossier dossier, TechnicianDossier.State next, UUID actor, String reason, Instant now) {
        jdbc.update("UPDATE app.technician_onboarding SET state=?,version=version+1,updated_at=? WHERE dossier_id=?",next.name(),ts(now),dossier.dossierId());
        var result=lock(dossier.dossierId()); history(result,actor,next.name(),reason,now); return result;
    }
    @Override public List<TechnicianDossier> review(long offset, int limit, TechnicianDossier.State state) {
        return jdbc.query("SELECT * FROM app.technician_onboarding WHERE state=? ORDER BY created_at,dossier_id LIMIT ? OFFSET ?",JdbcTechnicianAdmissions::dossier,state.name(),limit,offset);
    }
    @Override public List<History> history(UUID dossier,long after,int size) {
        return jdbc.query("SELECT revision,actor_user_id,action,reason,occurred_at FROM app.technician_onboarding_history WHERE dossier_id=? AND revision>? ORDER BY revision LIMIT ?",
                (row,index) -> new History(row.getLong(1),row.getObject(2,UUID.class),row.getString(3),row.getString(4),row.getTimestamp(5).toInstant()),dossier,after,size);
    }
    private void history(TechnicianDossier dossier,UUID actor,String action,String reason,Instant now) {
        var p=dossier.profile();
        jdbc.update("INSERT INTO app.technician_onboarding_history(event_id,dossier_id,revision,actor_user_id,action,reason,occurred_at,first_name,last_name,phone,base_city,radius_km,declared_skills) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?::varchar[])",UUID.randomUUID(),dossier.dossierId(),dossier.version(),actor,action,reason,ts(now),
                p.firstName(),p.lastName(),p.phone(),p.baseCity(),p.radiusKm(),p.declaredSkills().toArray(String[]::new));
    }
    private static TechnicianDossier dossier(ResultSet row,int index) throws SQLException {
        return new TechnicianDossier(row.getObject("dossier_id",UUID.class),row.getObject("user_id",UUID.class),TechnicianDossier.State.valueOf(row.getString("state")),
                new TechnicianProfile(row.getString("first_name"),row.getString("last_name"),row.getString("phone"),row.getString("base_city"),row.getObject("radius_km",Integer.class),
                        Arrays.asList((String[])row.getArray("declared_skills").getArray())),row.getLong("version"),row.getTimestamp("created_at").toInstant());
    }
    private static Timestamp ts(Instant now) { return Timestamp.from(now); }
}
