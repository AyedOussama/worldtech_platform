package fr.entreprise.maintenance.features.assets.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import fr.entreprise.maintenance.features.assets.domain.AssetFailure;
import fr.entreprise.maintenance.features.assets.domain.AssetStore;
import fr.entreprise.maintenance.features.assets.domain.Equipment;
import fr.entreprise.maintenance.features.assets.domain.EquipmentProfile;
import fr.entreprise.maintenance.features.assets.domain.Site;
import fr.entreprise.maintenance.features.assets.domain.SiteAccess;
import fr.entreprise.maintenance.features.assets.domain.SiteProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcAssetStore implements AssetStore {
    // Membership was authorized and the account locked by clients.publicapi for this transaction.
    // Site visibility is still applied in SQL before detail, pagination, count or private projection.
    private static final String SCOPE="s.account_id=? AND NOT s.archived AND (? OR EXISTS(SELECT 1 FROM app.site_access_policy p WHERE p.account_id=s.account_id AND p.member_id=? AND p.all_sites) OR EXISTS(SELECT 1 FROM app.site_access_grant g WHERE g.account_id=s.account_id AND g.member_id=? AND g.site_id=s.site_id))";
    private static final String EQUIPMENT=" FROM app.equipment e JOIN app.site s ON s.account_id=e.account_id AND s.site_id=e.site_id WHERE NOT e.archived AND "+SCOPE;
    private static final String FILTER=" AND (?::uuid IS NULL OR e.site_id=?) AND (?::varchar IS NULL OR e.category=?)";
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit audit;
    public JdbcAssetStore(JdbcTemplate jdbc,JsonMapper json,fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit audit) { this.jdbc=jdbc; this.json=json; this.audit=audit; }
    @Override public Site site(UUID account,UUID member,boolean admin,UUID id) { return jdbc.query("SELECT s.* FROM app.site s WHERE "+SCOPE+" AND s.site_id=?",this::site,account,admin,member,member,id).stream().findFirst().orElseThrow(JdbcAssetStore::missing); }
    @Override public List<Site> sites(UUID account,UUID member,boolean admin,int offset,int size) { return jdbc.query("SELECT s.* FROM app.site s WHERE "+SCOPE+" ORDER BY s.created_at,s.site_id LIMIT ? OFFSET ?",this::site,account,admin,member,member,size,offset); }
    @Override public long siteCount(UUID account,UUID member,boolean admin) { return jdbc.queryForObject("SELECT count(*) FROM app.site s WHERE "+SCOPE,Long.class,account,admin,member,member); }
    @Override public Site createSite(UUID account,SiteProfile p,UUID actor,Instant now) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO app.site(site_id,account_id,name,address,time_zone,contacts,access_instructions,created_at,updated_at) VALUES (?,?,?,?,?,?::jsonb,?,?,?)",id,account,p.name(),p.address(),p.timeZone(),json.writeValueAsString(p.contacts()),p.accessInstructions(),ts(now),ts(now));
        event(account,"SITE",id,0,actor,"CREATED",null,null,now); return site(account,null,true,id);
    }
    @Override public Site updateSite(UUID account,Site current,SiteProfile p,boolean archive,UUID actor,Instant now) {
        jdbc.update("UPDATE app.site SET name=?,address=?,time_zone=?,contacts=?::jsonb,access_instructions=?,archived=?,version=version+1,updated_at=? WHERE account_id=? AND site_id=?",p.name(),p.address(),p.timeZone(),json.writeValueAsString(p.contacts()),p.accessInstructions(),archive,ts(now),account,current.siteId());
        event(account,"SITE",current.siteId(),current.version()+1,actor,archive?"ARCHIVED":"UPDATED",null,null,now);
        return new Site(current.siteId(),p,archive,current.version()+1);
    }
    @Override public Equipment equipment(UUID account,UUID member,boolean admin,UUID id) { return jdbc.query("SELECT e.*"+EQUIPMENT+" AND e.equipment_id=?",this::equipment,account,admin,member,member,id).stream().findFirst().orElseThrow(JdbcAssetStore::missing); }
    @Override public boolean equipmentBelongsToActiveSite(UUID account,UUID site,UUID equipment) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app.equipment e JOIN app.site s ON s.account_id=e.account_id AND s.site_id=e.site_id WHERE e.account_id=? AND e.site_id=? AND e.equipment_id=? AND NOT e.archived AND NOT s.archived)",Boolean.class,account,site,equipment));
    }
    @Override public List<Equipment> equipmentList(UUID account,UUID member,boolean admin,UUID site,String category,int offset,int size) { return jdbc.query("SELECT e.*"+EQUIPMENT+FILTER+" ORDER BY e.created_at,e.equipment_id LIMIT ? OFFSET ?",this::equipment,account,admin,member,member,site,site,category,category,size,offset); }
    @Override public long equipmentCount(UUID account,UUID member,boolean admin,UUID site,String category) { return jdbc.queryForObject("SELECT count(*)"+EQUIPMENT+FILTER,Long.class,account,admin,member,member,site,site,category,category); }
    @Override public Equipment createEquipment(UUID account,UUID site,EquipmentProfile p,UUID actor,Instant now) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO app.equipment(equipment_id,account_id,site_id,label,category,internal_reference,brand,model,serial_number,location,condition_description,attributes,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?)",id,account,site,p.label(),p.category(),p.internalReference(),p.brand(),p.model(),p.serialNumber(),p.location(),p.conditionDescription(),json.writeValueAsString(p.attributes()),ts(now),ts(now));
        event(account,"EQUIPMENT",id,0,actor,"CREATED",null,null,now); return equipment(account,null,true,id);
    }
    @Override public Equipment updateEquipment(UUID account,Equipment c,EquipmentProfile p,boolean archive,UUID actor,Instant now) {
        jdbc.update("UPDATE app.equipment SET label=?,category=?,internal_reference=?,brand=?,model=?,serial_number=?,location=?,condition_description=?,attributes=?::jsonb,archived=?,version=version+1,updated_at=? WHERE account_id=? AND equipment_id=?",p.label(),p.category(),p.internalReference(),p.brand(),p.model(),p.serialNumber(),p.location(),p.conditionDescription(),json.writeValueAsString(p.attributes()),archive,ts(now),account,c.equipmentId());
        event(account,"EQUIPMENT",c.equipmentId(),c.version()+1,actor,archive?"ARCHIVED":"UPDATED",null,null,now);
        return new Equipment(c.equipmentId(),c.siteId(),p,archive,c.version()+1);
    }
    @Override public SiteAccess access(UUID account,UUID member) {
        var rows=jdbc.query("SELECT all_sites,version FROM app.site_access_policy WHERE account_id=? AND member_id=?",(r,i)->new SiteAccess(member,r.getBoolean(1),List.of(),r.getLong(2)),account,member);
        var sites=jdbc.query("SELECT site_id FROM app.site_access_grant WHERE account_id=? AND member_id=? ORDER BY site_id",(r,i)->r.getObject(1,UUID.class),account,member);
        return rows.isEmpty()?new SiteAccess(member,false,List.of(),0):new SiteAccess(member,rows.getFirst().allSites(),sites,rows.getFirst().version());
    }
    @Override public SiteAccess replaceAccess(UUID account,SiteAccess c,boolean all,List<UUID> sites,UUID actor,Instant now) {
        var checked=new SiteAccess(c.memberId(),all,sites,c.version()+1);
        jdbc.update("INSERT INTO app.site_access_policy(member_id,account_id,all_sites,version,updated_at) VALUES (?,?,?,?,?) ON CONFLICT(member_id) DO UPDATE SET all_sites=excluded.all_sites,version=excluded.version,updated_at=excluded.updated_at",c.memberId(),account,all,checked.version(),ts(now));
        jdbc.update("DELETE FROM app.site_access_grant WHERE account_id=? AND member_id=?",account,c.memberId());
        for(UUID site:checked.siteIds()) { jdbc.update("INSERT INTO app.site_access_grant(account_id,member_id,site_id) VALUES (?,?,?)",account,c.memberId(),site); }
        event(account,"SITE_ACCESS",c.memberId(),checked.version(),actor,"ACCESS_REPLACED",all,checked.siteIds(),now); return checked;
    }
    private void event(UUID account,String kind,UUID id,long version,UUID actor,String action,Boolean all,List<UUID> sites,Instant now) {
        jdbc.update("INSERT INTO app.asset_change(event_id,account_id,resource_kind,resource_id,revision,actor_user_id,action,all_sites,site_ids,occurred_at) VALUES (?,?,?,?,?,?,?,?,?::uuid[],?)",UUID.randomUUID(),account,kind,id,version,actor,action,all,sites==null?null:sites.toArray(UUID[]::new),ts(now));
        audit.record(fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit.Action.ASSET_CHANGED,actor,id);
    }
    private Site site(ResultSet r,int i) throws SQLException { return new Site(r.getObject("site_id",UUID.class),new SiteProfile(r.getString("name"),r.getString("address"),r.getString("time_zone"),Arrays.asList(json.readValue(r.getString("contacts"),SiteProfile.Contact[].class)),r.getString("access_instructions")),r.getBoolean("archived"),r.getLong("version")); }
    @SuppressWarnings("unchecked") private Equipment equipment(ResultSet r,int i) throws SQLException { return new Equipment(r.getObject("equipment_id",UUID.class),r.getObject("site_id",UUID.class),new EquipmentProfile(r.getString("label"),r.getString("category"),r.getString("internal_reference"),r.getString("brand"),r.getString("model"),r.getString("serial_number"),r.getString("location"),r.getString("condition_description"),(Map<String,String>)json.readValue(r.getString("attributes"),Map.class)),r.getBoolean("archived"),r.getLong("version")); }
    private static Timestamp ts(Instant now) { return Timestamp.from(now); }
    private static AssetFailure missing() { return new AssetFailure(AssetFailure.Code.ASSET_NOT_FOUND); }
}
