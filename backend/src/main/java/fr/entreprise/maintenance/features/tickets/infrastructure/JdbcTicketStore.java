package fr.entreprise.maintenance.features.tickets.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.tickets.domain.ClientProposal;
import fr.entreprise.maintenance.features.tickets.domain.Ticket;
import fr.entreprise.maintenance.features.tickets.domain.TicketFailure;
import fr.entreprise.maintenance.features.tickets.domain.TicketStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTicketStore implements TicketStore {
    private static final String FILTER=" WHERE account_id=? AND site_id=? AND (?::varchar IS NULL OR state=?) AND (?::varchar IS NULL OR requested_priority=?) AND (?::uuid IS NULL OR equipment_id=?)";
    private final JdbcTemplate jdbc;
    public JdbcTicketStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Ticket create(UUID account,UUID site,UUID equipment,String timeZone,String title,String description,Ticket.Priority requested,ClientProposal proposal,UUID actor,Instant now) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO app.ticket(ticket_id,account_id,site_id,equipment_id,site_time_zone,title,description,requested_priority,state,created_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,'OPEN',?,?,?)",id,account,site,equipment,timeZone,title,description,requested.name(),actor,ts(now),ts(now));
        proposal(account,id,0,proposal,actor,now); var ticket=lock(account,site,id); change(account,ticket,"CREATED",null,actor,now); return ticket;
    }
    @Override public Ticket lock(UUID account,UUID site,UUID ticket) {
        return jdbc.query("SELECT * FROM app.ticket WHERE account_id=? AND site_id=? AND ticket_id=? FOR UPDATE",JdbcTicketStore::ticket,account,site,ticket).stream().findFirst().orElseThrow(()->new TicketFailure(TicketFailure.Code.TICKET_NOT_FOUND));
    }
    @Override public List<Ticket> list(UUID account,UUID site,Ticket.State state,Ticket.Priority requested,UUID equipment,int offset,int size) {
        return jdbc.query("SELECT * FROM app.ticket"+FILTER+" ORDER BY created_at,ticket_id LIMIT ? OFFSET ?",JdbcTicketStore::ticket,account,site,name(state),name(state),name(requested),name(requested),equipment,equipment,size,offset);
    }
    @Override public long count(UUID account,UUID site,Ticket.State state,Ticket.Priority requested,UUID equipment) { return jdbc.queryForObject("SELECT count(*) FROM app.ticket"+FILTER,Long.class,account,site,name(state),name(state),name(requested),name(requested),equipment,equipment); }
    @Override public Ticket propose(UUID account,Ticket ticket,ClientProposal proposal,UUID actor,Instant now) {
        proposal(account,ticket.ticketId(),ticket.proposalNumber()+1,proposal,actor,now);
        updated(jdbc.update("UPDATE app.ticket SET proposal_number=proposal_number+1,version=version+1,updated_at=? WHERE account_id=? AND site_id=? AND ticket_id=? AND version=?",ts(now),account,ticket.siteId(),ticket.ticketId(),ticket.version()));
        var result=lock(account,ticket.siteId(),ticket.ticketId()); change(account,result,"PROPOSAL_REVISED",null,actor,now); return result;
    }
    @Override public Ticket transition(UUID account,Ticket ticket,Ticket.Action action,Ticket.State next,Ticket.Priority retained,UUID contractRevision,String reason,UUID actor,Instant now) {
        boolean qualifying=action==Ticket.Action.QUALIFY;
        String kind=qualifying ? (contractRevision==null?"OUT_OF_CONTRACT":"COVERED") : ticket.qualified()?(ticket.contractRevisionId()==null?"OUT_OF_CONTRACT":"COVERED"):null;
        updated(jdbc.update("UPDATE app.ticket SET state=?,retained_priority=?,qualification_kind=?,contract_revision_id=?,version=version+1,updated_at=?, "
                +"qualified_at=CASE WHEN ? THEN ? ELSE qualified_at END, started_at=CASE WHEN ? THEN coalesce(started_at,?) ELSE started_at END, "
                +"resolved_at=CASE WHEN ? THEN ? WHEN ? THEN NULL ELSE resolved_at END, closed_at=CASE WHEN ? THEN ? ELSE closed_at END "
                +"WHERE account_id=? AND site_id=? AND ticket_id=? AND version=?",
                next.name(),name(qualifying?retained:ticket.retainedPriority()),kind,qualifying?contractRevision:ticket.contractRevisionId(),ts(now),
                qualifying,ts(now),action==Ticket.Action.START || action==Ticket.Action.RESUME || action==Ticket.Action.REOPEN,ts(now),
                action==Ticket.Action.RESOLVE,ts(now),action==Ticket.Action.REOPEN,action==Ticket.Action.CLOSE,ts(now),account,ticket.siteId(),ticket.ticketId(),ticket.version()));
        var result=lock(account,ticket.siteId(),ticket.ticketId()); change(account,result,action.name(),reason,actor,now); return result;
    }
    private void proposal(UUID account,UUID ticket,long number,ClientProposal proposal,UUID actor,Instant now) {
        jdbc.update("INSERT INTO app.client_financial_proposal(account_id,ticket_id,proposal_number,remuneration,expenses,author_user_id,created_at) VALUES (?,?,?,?,?,?,?)",account,ticket,number,proposal.remuneration(),proposal.expenses(),actor,ts(now));
    }
    private void change(UUID account,Ticket ticket,String action,String reason,UUID actor,Instant now) {
        jdbc.update("INSERT INTO app.ticket_change(account_id,site_id,ticket_id,revision,action,state,actor_user_id,public_reason,occurred_at) VALUES (?,?,?,?,?,?,?,?,?)",account,ticket.siteId(),ticket.ticketId(),ticket.version(),action,ticket.state().name(),actor,reason,ts(now));
    }
    @Override public List<Proposal> proposals(UUID account,UUID site,UUID ticket,long after,int size) {
        return jdbc.query("SELECT p.proposal_number,p.remuneration,p.expenses,p.author_user_id,p.created_at FROM app.client_financial_proposal p JOIN app.ticket t ON t.account_id=p.account_id AND t.ticket_id=p.ticket_id WHERE t.account_id=? AND t.site_id=? AND t.ticket_id=? AND p.proposal_number>? ORDER BY p.proposal_number LIMIT ?",
                (r,i)->new Proposal(r.getLong(1),new ClientProposal(r.getBigDecimal(2),r.getBigDecimal(3)),r.getObject(4,UUID.class),r.getTimestamp(5).toInstant()),account,site,ticket,after,size);
    }
    @Override public List<Change> history(UUID account,UUID site,UUID ticket,long after,int size) {
        return jdbc.query("SELECT revision,action,state,actor_user_id,public_reason,occurred_at FROM app.ticket_change WHERE account_id=? AND site_id=? AND ticket_id=? AND revision>? ORDER BY revision LIMIT ?",(r,i)->new Change(r.getLong(1),r.getString(2),Ticket.State.valueOf(r.getString(3)),r.getObject(4,UUID.class),r.getString(5),r.getTimestamp(6).toInstant()),account,site,ticket,after,size);
    }
    @Override public Message message(UUID account,Ticket ticket,boolean internal,String text,UUID actor,Instant now) {
        String table=table(internal); UUID id=UUID.randomUUID();
        long number=jdbc.queryForObject("SELECT coalesce(max(message_number),-1)+1 FROM "+table+" WHERE account_id=? AND site_id=? AND ticket_id=?",Long.class,account,ticket.siteId(),ticket.ticketId());
        jdbc.update("INSERT INTO "+table+"(account_id,site_id,ticket_id,message_number,message_id,author_user_id,message_text,created_at) VALUES (?,?,?,?,?,?,?,?)",account,ticket.siteId(),ticket.ticketId(),number,id,actor,text,ts(now));
        return new Message(number,id,actor,text,now);
    }
    @Override public List<Message> messages(UUID account,UUID site,UUID ticket,boolean internal,long after,int size) {
        return jdbc.query("SELECT message_number,message_id,author_user_id,message_text,created_at FROM "+table(internal)+" WHERE account_id=? AND site_id=? AND ticket_id=? AND message_number>? ORDER BY message_number LIMIT ?",(r,i)->new Message(r.getLong(1),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getString(4),r.getTimestamp(5).toInstant()),account,site,ticket,after,size);
    }
    private static String table(boolean internal) { return internal?"app.ticket_internal_note":"app.ticket_public_message"; }
    private static void updated(int count) { if(count!=1) { throw new TicketFailure(TicketFailure.Code.TICKET_VERSION_STALE); } }
    private static String name(Enum<?> value) { return value==null?null:value.name(); }
    private static Timestamp ts(Instant instant) { return Timestamp.from(instant); }
    private static Ticket ticket(ResultSet r,int i) throws SQLException {
        String priority=r.getString("retained_priority");
        return new Ticket(r.getObject("ticket_id",UUID.class),r.getObject("site_id",UUID.class),r.getObject("equipment_id",UUID.class),r.getString("site_time_zone"),r.getString("title"),r.getString("description"),Ticket.Priority.valueOf(r.getString("requested_priority")),priority==null?null:Ticket.Priority.valueOf(priority),Ticket.State.valueOf(r.getString("state")),r.getObject("contract_revision_id",UUID.class),r.getTimestamp("qualified_at")!=null,r.getLong("proposal_number"),r.getLong("version"),r.getTimestamp("created_at").toInstant());
    }
}
