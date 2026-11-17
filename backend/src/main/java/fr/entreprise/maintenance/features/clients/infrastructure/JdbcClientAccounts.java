package fr.entreprise.maintenance.features.clients.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.clients.domain.ClientAccount;
import fr.entreprise.maintenance.features.clients.domain.ClientAccounts;
import fr.entreprise.maintenance.features.clients.domain.AccountFailure;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClientAccounts implements ClientAccounts {
    private final JdbcTemplate jdbc;
    private final fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit audit;
    public JdbcClientAccounts(JdbcTemplate jdbc,fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit audit) { this.jdbc=jdbc; this.audit=audit; }
    @Override public Source approvedSource(UUID dossier) {
        return jdbc.query("SELECT user_id,account_type,first_name,last_name,organization_name FROM app.client_onboarding WHERE dossier_id=? AND state='APPROVED'",
                (r,i)->new Source(r.getObject(1,UUID.class),ClientAccount.Type.valueOf(r.getString(2)),r.getString(2).equals("BUSINESS") ? r.getString(5) : r.getString(3)+" "+r.getString(4)),dossier)
                .stream().findFirst().orElseThrow(()->new AccountFailure(AccountFailure.Code.CLIENT_NOT_APPROVED));
    }
    @Override public boolean approved(UUID user) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app.client_onboarding WHERE user_id=? AND state='APPROVED')",Boolean.class,user)); }
    @Override public Created create(UUID dossier,Source source,UUID actor,Instant now) {
        UUID id=UUID.randomUUID();
        int inserted=jdbc.update("INSERT INTO app.client_account(account_id,dossier_id,founder_user_id,account_type,display_name,created_at,updated_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT(dossier_id) DO NOTHING",
                id,dossier,source.userId(),source.accountType().name(),ClientAccount.name(source.name()),ts(now),ts(now));
        if(inserted==0) {
            return new Created(jdbc.query("SELECT * FROM app.client_account WHERE dossier_id=? FOR UPDATE",JdbcClientAccounts::account,dossier).getFirst(),false);
        }
        var account=lock(id); event(id,"ACCOUNT",id,0,actor,"CREATED",now); add(account,source.userId(),actor,now);
        return new Created(account,true);
    }
    @Override public ClientAccount lock(UUID id) { return jdbc.query("SELECT * FROM app.client_account WHERE account_id=? FOR UPDATE",JdbcClientAccounts::account,id).stream().findFirst().orElseThrow(JdbcClientAccounts::missing); }
    @Override public ClientAccount scoped(UUID id,UUID user,boolean admin) {
        return jdbc.query("SELECT a.* FROM app.client_account a WHERE a.account_id=? AND (? OR EXISTS(SELECT 1 FROM app.client_member m WHERE m.account_id=a.account_id AND m.user_id=? AND m.active)) FOR UPDATE OF a",
                JdbcClientAccounts::account,id,admin,user).stream().findFirst().orElseThrow(JdbcClientAccounts::missing);
    }
    @Override public List<ClientAccount> list(UUID user,boolean admin,int offset,int size) {
        return jdbc.query("SELECT a.* FROM app.client_account a WHERE (? OR EXISTS(SELECT 1 FROM app.client_member m WHERE m.account_id=a.account_id AND m.user_id=? AND m.active)) ORDER BY a.created_at,a.account_id LIMIT ? OFFSET ?",
                JdbcClientAccounts::account,admin,user,size,offset);
    }
    @Override public long count(UUID user,boolean admin) { return jdbc.queryForObject("SELECT count(*) FROM app.client_account a WHERE (? OR EXISTS(SELECT 1 FROM app.client_member m WHERE m.account_id=a.account_id AND m.user_id=? AND m.active))",Long.class,admin,user); }
    @Override public ClientAccount rename(ClientAccount a,String name,UUID actor,Instant now) {
        jdbc.update("UPDATE app.client_account SET display_name=?,version=version+1,updated_at=? WHERE account_id=?",name,ts(now),a.accountId());
        event(a.accountId(),"ACCOUNT",a.accountId(),a.version()+1,actor,"RENAMED",now); return lock(a.accountId());
    }
    @Override public Member currentMember(UUID account,UUID user) {
        return jdbc.query("SELECT * FROM app.client_member WHERE account_id=? AND user_id=? AND active",JdbcClientAccounts::member,account,user).stream().findFirst().orElseThrow(JdbcClientAccounts::missing);
    }
    @Override public Member member(UUID account,UUID id) { return jdbc.query("SELECT * FROM app.client_member WHERE account_id=? AND member_id=?",JdbcClientAccounts::member,account,id).stream().findFirst().orElseThrow(JdbcClientAccounts::missing); }
    @Override public List<Member> members(UUID account,int offset,int size) { return jdbc.query("SELECT * FROM app.client_member WHERE account_id=? AND active ORDER BY created_at,member_id LIMIT ? OFFSET ?",JdbcClientAccounts::member,account,size,offset); }
    @Override public long memberCount(UUID account) { return jdbc.queryForObject("SELECT count(*) FROM app.client_member WHERE account_id=? AND active",Long.class,account); }
    @Override public Member add(ClientAccount account,UUID user,UUID actor,Instant now) {
        UUID id=UUID.randomUUID();
        int inserted=jdbc.update("INSERT INTO app.client_member(member_id,account_id,account_type,founder_user_id,user_id,created_at,updated_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT(account_id,user_id) WHERE active DO NOTHING",
                id,account.accountId(),account.accountType().name(),account.founderUserId(),user,ts(now),ts(now));
        if(inserted!=1) { throw new AccountFailure(AccountFailure.Code.ACCOUNT_MEMBERSHIP_CONFLICT); }
        event(account.accountId(),"MEMBER",id,0,actor,"MEMBER_ADDED",now); return currentMember(account.accountId(),user);
    }
    @Override public Member remove(UUID account,UUID memberId,long expected,UUID actor,Instant now) {
        var rows=jdbc.query("SELECT * FROM app.client_member WHERE account_id=? AND member_id=? AND active",JdbcClientAccounts::member,account,memberId);
        if(rows.isEmpty()) { throw missing(); } var member=rows.getFirst();
        if(member.version()!=expected) { throw new AccountFailure(AccountFailure.Code.ACCOUNT_VERSION_STALE); }
        jdbc.update("UPDATE app.client_member SET active=false,version=version+1,updated_at=? WHERE account_id=? AND member_id=?",ts(now),account,memberId);
        event(account,"MEMBER",memberId,expected+1,actor,"MEMBER_REMOVED",now); return new Member(memberId,member.userId(),false,expected+1);
    }
    private void event(UUID account,String kind,UUID resource,long revision,UUID actor,String action,Instant now) {
        jdbc.update("INSERT INTO app.client_account_change(event_id,account_id,resource_kind,resource_id,revision,actor_user_id,action,occurred_at) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),account,kind,resource,revision,actor,action,ts(now));
        audit.record(fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit.Action.ACCOUNT_CHANGED,actor,resource);
    }
    private static ClientAccount account(ResultSet r,int i) throws SQLException { return new ClientAccount(r.getObject("account_id",UUID.class),ClientAccount.Type.valueOf(r.getString("account_type")),r.getString("display_name"),r.getObject("founder_user_id",UUID.class),r.getLong("version"),r.getTimestamp("created_at").toInstant()); }
    private static Member member(ResultSet r,int i) throws SQLException { return new Member(r.getObject("member_id",UUID.class),r.getObject("user_id",UUID.class),r.getBoolean("active"),r.getLong("version")); }
    private static AccountFailure missing() { return new AccountFailure(AccountFailure.Code.ACCOUNT_NOT_FOUND); }
    private static Timestamp ts(Instant now) { return Timestamp.from(now); }
}
