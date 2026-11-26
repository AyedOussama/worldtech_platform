package fr.entreprise.maintenance.features.missions.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import fr.entreprise.maintenance.features.missions.domain.Mission;
import fr.entreprise.maintenance.features.missions.domain.MissionFailure;
import fr.entreprise.maintenance.features.missions.domain.MissionFinancialRevision;
import fr.entreprise.maintenance.features.missions.domain.MissionStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMissionStore implements MissionStore {
    private static final String SELECT="SELECT m.mission_id,m.account_id,m.site_id,m.ticket_id,m.contract_revision_id,m.title,m.public_summary,"
            +"m.window_start,m.window_end,m.public_latitude,m.public_longitude,m.public_label,m.state,m.financial_revision,m.version,"
            +"m.created_at,m.published_at,coalesce((SELECT array_agg(s.skill ORDER BY s.skill_order) FROM app.mission_skill s "
            +"WHERE s.mission_id=m.mission_id),'{}'::text[]) AS skills FROM app.mission m";
    private static final String FILTER=" WHERE m.account_id=? AND m.site_id=? AND (?::varchar IS NULL OR m.state=?)";
    private final JdbcTemplate jdbc;
    public JdbcMissionStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Mission create(UUID account,UUID site,Mission draft,UUID actor,Instant now) {
        jdbc.update("INSERT INTO app.mission(mission_id,account_id,site_id,ticket_id,contract_revision_id,title,public_summary,window_start,window_end,"
                        +"public_latitude,public_longitude,public_label,public_location,state,financial_revision,version,created_by,created_at,updated_at,published_at) "
                        +"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,ST_SetSRID(ST_MakePoint(?,?),4326)::geography,'DRAFT',-1,0,?,?,?,NULL)",
                draft.missionId(),account,site,draft.ticketId(),draft.contractRevisionId(),draft.title(),draft.publicSummary(),
                ts(draft.windowStart()),ts(draft.windowEnd()),draft.publicLatitude(),draft.publicLongitude(),draft.publicLabel(),
                draft.publicLongitude(),draft.publicLatitude(),actor,ts(now),ts(now));
        for(int i=0;i<draft.skills().size();i++) {
            jdbc.update("INSERT INTO app.mission_skill(account_id,mission_id,skill_order,skill) VALUES (?,?,?,?)",account,draft.missionId(),i,draft.skills().get(i));
        }
        return lock(account,site,draft.missionId());
    }
    @Override public Mission lock(UUID account,UUID site,UUID mission) {
        return jdbc.query(SELECT+" WHERE m.account_id=? AND m.site_id=? AND m.mission_id=? FOR UPDATE OF m",JdbcMissionStore::mission,account,site,mission)
                .stream().findFirst().orElseThrow(()->new MissionFailure(MissionFailure.Code.MISSION_NOT_FOUND));
    }
    @Override public List<Mission> list(UUID account,UUID site,Mission.State state,int offset,int size) {
        return jdbc.query(SELECT+FILTER+" ORDER BY m.created_at,m.mission_id LIMIT ? OFFSET ?",JdbcMissionStore::mission,
                account,site,name(state),name(state),size,offset);
    }
    @Override public long count(UUID account,UUID site,Mission.State state) {
        return jdbc.queryForObject("SELECT count(*) FROM app.mission m"+FILTER,Long.class,account,site,name(state),name(state));
    }
    @Override public Mission publish(UUID account,Mission current,MissionFinancialRevision revision,Instant now) {
        financial(account,current,revision);
        updated(jdbc.update("UPDATE app.mission SET state='PUBLISHED',financial_revision=?,version=version+1,updated_at=?,published_at=? "
                        +"WHERE account_id=? AND site_id=? AND mission_id=? AND version=?",
                revision.number(),ts(now),ts(now),account,current.siteId(),current.missionId(),current.version()));
        return lock(account,current.siteId(),current.missionId());
    }
    @Override public Mission revise(UUID account,Mission current,MissionFinancialRevision revision,Instant now) {
        financial(account,current,revision);
        updated(jdbc.update("UPDATE app.mission SET financial_revision=?,version=version+1,updated_at=? "
                        +"WHERE account_id=? AND site_id=? AND mission_id=? AND version=?",
                revision.number(),ts(now),account,current.siteId(),current.missionId(),current.version()));
        return lock(account,current.siteId(),current.missionId());
    }
    private void financial(UUID account,Mission mission,MissionFinancialRevision revision) {
        jdbc.update("INSERT INTO app.mission_financial_revision(account_id,mission_id,revision_number,remuneration,expenses,reason,author_user_id,created_at) "
                        +"VALUES (?,?,?,?,?,?,?,?)",
                account,mission.missionId(),revision.number(),revision.remuneration(),revision.expenses(),revision.reason(),
                revision.authorUserId(),ts(revision.createdAt()));
    }
    @Override public List<MissionFinancialRevision> financialRevisions(UUID account,UUID site,UUID mission,long after,int size) {
        return jdbc.query("SELECT f.revision_number,f.remuneration,f.expenses,f.reason,f.author_user_id,f.created_at "
                        +"FROM app.mission_financial_revision f JOIN app.mission m ON m.account_id=f.account_id AND m.mission_id=f.mission_id "
                        +"WHERE m.account_id=? AND m.site_id=? AND m.mission_id=? AND f.revision_number>? ORDER BY f.revision_number LIMIT ?",
                JdbcMissionStore::revision,account,site,mission,after,size);
    }
    @Override public Optional<MissionFinancialRevision> currentFinancial(UUID account,UUID site,UUID mission) {
        return jdbc.query("SELECT f.revision_number,f.remuneration,f.expenses,f.reason,f.author_user_id,f.created_at "
                        +"FROM app.mission_financial_revision f JOIN app.mission m ON m.account_id=f.account_id AND m.mission_id=f.mission_id "
                        +"WHERE m.account_id=? AND m.site_id=? AND m.mission_id=? ORDER BY f.revision_number DESC LIMIT 1",
                JdbcMissionStore::revision,account,site,mission).stream().findFirst();
    }
    private static void updated(int count) { if(count!=1) { throw new MissionFailure(MissionFailure.Code.MISSION_VERSION_STALE); } }
    private static String name(Enum<?> value) { return value==null?null:value.name(); }
    private static Timestamp ts(Instant instant) { return Timestamp.from(instant); }
    private static Mission mission(ResultSet r,int i) throws SQLException {
        var skills=r.getArray("skills");
        Timestamp published=r.getTimestamp("published_at");
        return new Mission(r.getObject("mission_id",UUID.class),r.getObject("account_id",UUID.class),r.getObject("site_id",UUID.class),
                r.getObject("ticket_id",UUID.class),r.getObject("contract_revision_id",UUID.class),r.getString("title"),
                r.getString("public_summary"),skills==null?List.of():Arrays.stream((String[])skills.getArray()).toList(),
                r.getTimestamp("window_start").toInstant(),r.getTimestamp("window_end").toInstant(),
                r.getBigDecimal("public_latitude"),r.getBigDecimal("public_longitude"),r.getString("public_label"),
                Mission.State.valueOf(r.getString("state")),r.getLong("financial_revision"),r.getLong("version"),
                r.getTimestamp("created_at").toInstant(),published==null?null:published.toInstant());
    }
    private static MissionFinancialRevision revision(ResultSet r,int i) throws SQLException {
        return new MissionFinancialRevision(r.getLong("revision_number"),r.getBigDecimal("remuneration"),r.getBigDecimal("expenses"),
                r.getString("reason"),r.getObject("author_user_id",UUID.class),r.getTimestamp("created_at").toInstant());
    }
}
