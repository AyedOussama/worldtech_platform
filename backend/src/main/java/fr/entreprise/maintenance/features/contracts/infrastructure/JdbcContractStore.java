package fr.entreprise.maintenance.features.contracts.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.contracts.domain.ContractDefinition;
import fr.entreprise.maintenance.features.contracts.domain.ContractFailure;
import fr.entreprise.maintenance.features.contracts.domain.ContractRevision;
import fr.entreprise.maintenance.features.contracts.domain.ContractStore;
import fr.entreprise.maintenance.features.contracts.domain.ServiceCalendar;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcContractStore implements ContractStore {
    private static final String SUMMARY="r.contract_id,r.revision_id,r.revision_number,r.public_name,r.valid_from,r.valid_until";
    private static final String SITE=" FROM app.contract_revision r JOIN app.contract_site_coverage c ON c.account_id=r.account_id AND c.revision_id=r.revision_id WHERE r.account_id=? AND c.site_id=?";
    private final JdbcTemplate jdbc;
    public JdbcContractStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public ContractRevision create(UUID account,ContractDefinition definition,UUID actor,Instant now) {
        UUID contract=UUID.randomUUID();
        jdbc.update("INSERT INTO app.maintenance_contract(contract_id,account_id,created_at,updated_at) VALUES (?,?,?,?)",contract,account,ts(now),ts(now));
        return insert(account,contract,0,definition,actor,now);
    }
    @Override public ContractRevision current(UUID account,UUID contract) {
        var ids=jdbc.query("SELECT r.revision_id FROM app.maintenance_contract c JOIN app.contract_revision r ON r.contract_id=c.contract_id AND r.revision_number=c.version WHERE c.account_id=? AND c.contract_id=? FOR UPDATE OF c",(r,i)->r.getObject(1,UUID.class),account,contract);
        if(ids.isEmpty()) { throw missing(); } return revision(account,contract,ids.getFirst());
    }
    @Override public ContractRevision revise(UUID account,ContractRevision current,ContractDefinition definition,UUID actor,Instant now) {
        if(jdbc.update("UPDATE app.maintenance_contract SET version=version+1,updated_at=? WHERE account_id=? AND contract_id=? AND version=?",ts(now),account,current.contractId(),current.version())!=1) {
            throw new ContractFailure(ContractFailure.Code.CONTRACT_VERSION_STALE);
        }
        return insert(account,current.contractId(),current.version()+1,definition,actor,now);
    }
    private ContractRevision insert(UUID account,UUID contract,long number,ContractDefinition d,UUID actor,Instant now) {
        UUID revision=UUID.randomUUID();
        jdbc.update("INSERT INTO app.contract_revision(revision_id,account_id,contract_id,revision_number,public_name,internal_notes,valid_from,valid_until,calendar_mode,reason,author_user_id,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                revision,account,contract,number,d.publicName(),d.internalNotes(),ts(d.validFrom()),ts(d.validUntil()),d.calendar().mode().name(),d.reason(),actor,ts(now));
        for(var c:d.coverage()) {
            jdbc.update("INSERT INTO app.contract_site_coverage(account_id,revision_id,site_id,coverage_mode) VALUES (?,?,?,?)",account,revision,c.siteId(),c.wholeSite()?"SITE":"EQUIPMENT");
            for(UUID equipment:c.equipmentIds()) { jdbc.update("INSERT INTO app.contract_equipment_coverage(account_id,revision_id,site_id,equipment_id) VALUES (?,?,?,?)",account,revision,c.siteId(),equipment); }
        }
        for(var w:d.calendar().weekly()) { jdbc.update("INSERT INTO app.contract_weekly_window(account_id,revision_id,day_of_week,start_minute,end_minute) VALUES (?,?,?,?,?)",account,revision,w.dayOfWeek(),w.startMinute(),w.endMinute()); }
        for(var day:d.calendar().overrides()) {
            jdbc.update("INSERT INTO app.contract_day_override(account_id,revision_id,service_date) VALUES (?,?,?)",account,revision,day.date());
            for(var w:day.windows()) { jdbc.update("INSERT INTO app.contract_override_window(account_id,revision_id,service_date,start_minute,end_minute) VALUES (?,?,?,?,?)",account,revision,day.date(),w.startMinute(),w.endMinute()); }
        }
        for(var rule:d.slaRules()) { jdbc.update("INSERT INTO app.contract_sla_rule(account_id,revision_id,priority,metric,target_minutes,pause_while_waiting_client) VALUES (?,?,?,?,?,?)",account,revision,rule.priority().name(),rule.metric().name(),rule.targetMinutes(),rule.pauseWhileWaitingClient()); }
        return new ContractRevision(contract,revision,number,d,actor,now);
    }
    @Override public ContractRevision revision(UUID account,UUID contract,UUID revision) {
        var rows=jdbc.query("SELECT * FROM app.contract_revision WHERE account_id=? AND contract_id=? AND revision_id=?",(r,i)->new RevisionRow(r.getObject("contract_id",UUID.class),r.getObject("revision_id",UUID.class),r.getLong("revision_number"),r.getString("public_name"),r.getString("internal_notes"),r.getTimestamp("valid_from").toInstant(),r.getTimestamp("valid_until").toInstant(),r.getString("calendar_mode"),r.getString("reason"),r.getObject("author_user_id",UUID.class),r.getTimestamp("created_at").toInstant()),account,contract,revision);
        if(rows.isEmpty()) { throw missing(); } var row=rows.getFirst();
        var equipment=jdbc.query("SELECT site_id,equipment_id FROM app.contract_equipment_coverage WHERE account_id=? AND revision_id=? ORDER BY equipment_id",(r,i)->new EquipmentLink(r.getObject(1,UUID.class),r.getObject(2,UUID.class)),account,revision);
        var coverage=jdbc.query("SELECT site_id,coverage_mode FROM app.contract_site_coverage WHERE account_id=? AND revision_id=? ORDER BY site_id",(r,i)-> {
            UUID site=r.getObject(1,UUID.class);
            return new ContractDefinition.Coverage(site,r.getString(2).equals("SITE"),equipment.stream().filter(e->e.site().equals(site)).map(EquipmentLink::equipment).toList());
        },account,revision);
        var definition=new ContractDefinition(row.name(),row.notes(),row.from(),row.until(),coverage,calendar(account,revision,row.mode()),rules(account,revision),row.reason());
        return new ContractRevision(row.contract(),revision,row.number(),definition,row.author(),row.created());
    }
    @Override public void requireRevision(UUID account,UUID revision) {
        if(jdbc.queryForObject("SELECT count(*) FROM app.contract_revision WHERE account_id=? AND revision_id=?",Long.class,account,revision)==0) { throw missing(); }
    }
    @Override public ContractRevision applicable(UUID account,UUID revision,UUID site,UUID equipment,Instant at) {
        var ids=jdbc.query("SELECT r.contract_id"+SITE+" AND r.revision_id=? AND r.valid_from<=? AND r.valid_until>? AND (c.coverage_mode='SITE' OR EXISTS(SELECT 1 FROM app.contract_equipment_coverage e WHERE e.account_id=r.account_id AND e.revision_id=r.revision_id AND e.site_id=c.site_id AND e.equipment_id=?))",
                (r,i)->r.getObject(1,UUID.class),account,site,revision,ts(at),ts(at),equipment);
        if(ids.isEmpty()) { throw new ContractFailure(ContractFailure.Code.CONTRACT_NOT_APPLICABLE); }
        return revision(account,ids.getFirst(),revision);
    }
    @Override public List<Summary> list(UUID account,int offset,int size) { return jdbc.query("SELECT "+SUMMARY+" FROM app.maintenance_contract c JOIN app.contract_revision r ON r.contract_id=c.contract_id AND r.revision_number=c.version WHERE c.account_id=? ORDER BY c.created_at,c.contract_id LIMIT ? OFFSET ?",JdbcContractStore::summary,account,size,offset); }
    @Override public long count(UUID account) { return jdbc.queryForObject("SELECT count(*) FROM app.maintenance_contract WHERE account_id=?",Long.class,account); }
    @Override public List<Summary> history(UUID account,UUID contract,long after,int size) { return jdbc.query("SELECT "+SUMMARY+" FROM app.contract_revision r WHERE r.account_id=? AND r.contract_id=? AND r.revision_number>? ORDER BY r.revision_number LIMIT ?",JdbcContractStore::summary,account,contract,after,size); }
    @Override public List<SiteSummary> siteList(UUID account,UUID site,Instant at,int offset,int size) {
        return jdbc.query("SELECT "+SUMMARY+",c.coverage_mode"+SITE+" AND r.valid_from<=? AND r.valid_until>? ORDER BY r.created_at,r.revision_id LIMIT ? OFFSET ?",
                (r,i)->new SiteSummary(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getString(4),r.getTimestamp(5).toInstant(),r.getTimestamp(6).toInstant(),r.getString(7).equals("SITE")),account,site,ts(at),ts(at),size,offset);
    }
    @Override public long siteCount(UUID account,UUID site,Instant at) { return jdbc.queryForObject("SELECT count(*)"+SITE+" AND r.valid_from<=? AND r.valid_until>?",Long.class,account,site,ts(at),ts(at)); }
    @Override public SiteRevision siteRevision(UUID account,UUID site,UUID revision) {
        // This projection never selects internal notes/reasons or another site's coverage.
        var rows=jdbc.query("SELECT "+SUMMARY+",c.coverage_mode,r.calendar_mode"+SITE+" AND r.revision_id=?",(r,i)->new SiteRow(summary(r,i),r.getString(7).equals("SITE"),r.getString(8)),account,site,revision);
        if(rows.isEmpty()) { throw missing(); } var row=rows.getFirst(); var s=row.summary();
        var equipment=jdbc.query("SELECT equipment_id FROM app.contract_equipment_coverage WHERE account_id=? AND site_id=? AND revision_id=? ORDER BY equipment_id",(r,i)->r.getObject(1,UUID.class),account,site,revision);
        return new SiteRevision(s.contractId(),revision,s.version(),s.publicName(),s.validFrom(),s.validUntil(),row.whole(),equipment,calendar(account,revision,row.mode()),rules(account,revision));
    }
    private ServiceCalendar calendar(UUID account,UUID revision,String mode) {
        var weekly=jdbc.query("SELECT day_of_week,start_minute,end_minute FROM app.contract_weekly_window WHERE account_id=? AND revision_id=? ORDER BY day_of_week,start_minute",(r,i)->new ServiceCalendar.WeeklyWindow(r.getInt(1),r.getInt(2),r.getInt(3)),account,revision);
        var windows=jdbc.query("SELECT service_date,start_minute,end_minute FROM app.contract_override_window WHERE account_id=? AND revision_id=? ORDER BY service_date,start_minute",(r,i)->new DatedWindow(r.getObject(1,LocalDate.class),new ServiceCalendar.Window(r.getInt(2),r.getInt(3))),account,revision);
        var days=jdbc.query("SELECT service_date FROM app.contract_day_override WHERE account_id=? AND revision_id=? ORDER BY service_date",(r,i)->r.getObject(1,LocalDate.class),account,revision);
        var overrides=days.stream().map(day->new ServiceCalendar.DayOverride(day,windows.stream().filter(w->w.date().equals(day)).map(DatedWindow::window).toList())).toList();
        return new ServiceCalendar(ServiceCalendar.Mode.valueOf(mode),weekly,overrides);
    }
    private List<ContractDefinition.SlaRule> rules(UUID account,UUID revision) { return jdbc.query("SELECT priority,metric,target_minutes,pause_while_waiting_client FROM app.contract_sla_rule WHERE account_id=? AND revision_id=? ORDER BY priority,metric",(r,i)->new ContractDefinition.SlaRule(ContractDefinition.Priority.valueOf(r.getString(1)),ContractDefinition.Metric.valueOf(r.getString(2)),r.getInt(3),r.getBoolean(4)),account,revision); }
    private static Summary summary(ResultSet r,int i) throws SQLException { return new Summary(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getString(4),r.getTimestamp(5).toInstant(),r.getTimestamp(6).toInstant()); }
    private record RevisionRow(UUID contract,UUID revision,long number,String name,String notes,Instant from,Instant until,String mode,String reason,UUID author,Instant created) { @Override public String toString() { return "RevisionRow[redacted]"; } }
    private record EquipmentLink(UUID site,UUID equipment) { }
    private record DatedWindow(LocalDate date,ServiceCalendar.Window window) { }
    private record SiteRow(Summary summary,boolean whole,String mode) { }
    private static Timestamp ts(Instant instant) { return Timestamp.from(instant); }
    private static ContractFailure missing() { return new ContractFailure(ContractFailure.Code.CONTRACT_NOT_FOUND); }
}
