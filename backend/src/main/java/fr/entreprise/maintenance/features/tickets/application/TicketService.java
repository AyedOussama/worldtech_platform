package fr.entreprise.maintenance.features.tickets.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.assets.publicapi.AssetAccess;
import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.contracts.publicapi.ContractCoverage;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.notifications.publicapi.BusinessEvents;
import fr.entreprise.maintenance.features.tickets.domain.ClientProposal;
import fr.entreprise.maintenance.features.tickets.domain.Ticket;
import fr.entreprise.maintenance.features.tickets.domain.TicketFailure;
import fr.entreprise.maintenance.features.tickets.domain.TicketStore;
import fr.entreprise.maintenance.shared.idempotency.CommandFingerprint;
import fr.entreprise.maintenance.shared.idempotency.CommandReceipts;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketService {
    public record Page(List<Ticket> items,long total,int page,int size) { }
    private final TicketStore store;
    private final AssetAccess assets;
    private final ContractCoverage contracts;
    private final CommandReceipts receipts;
    private final SecurityAudit audit;
    private final BusinessEvents events;
    private final Clock clock;
    public TicketService(TicketStore store,AssetAccess assets,ContractCoverage contracts,CommandReceipts receipts,SecurityAudit audit,BusinessEvents events,Clock clock) { this.store=store; this.assets=assets; this.contracts=contracts; this.receipts=receipts; this.audit=audit; this.events=events; this.clock=clock; }
    public CommandReceipts.ResourceResult create(IdentitySession actor,UUID account,UUID site,UUID equipment,String title,String description,Ticket.Priority requested,ClientProposal proposal,String key) {
        var scope=assets.authorizeSite(actor,account,site,false); clientOnly(scope); if(equipment!=null) { assets.requireEquipment(scope,equipment); }
        String checkedTitle=Ticket.text(title,160),checkedDescription=Ticket.text(description,4000);
        if(requested==null || proposal==null) { throw TicketFailure.invalid(); }
        String operation="ticket-create/"+site;
        String request=CommandFingerprint.of(account.toString(),string(equipment),checkedTitle,checkedDescription,requested.name(),money(proposal.remuneration()),money(proposal.expenses()));
        var replay=receipts.replayResource(actor.userId(),operation,key,request); if(replay.isPresent()) { return replay.get(); }
        var ticket=store.create(account,site,equipment,scope.timeZone(),checkedTitle,checkedDescription,requested,proposal,actor.userId(),clock.instant()); changed(account,ticket,actor,BusinessEvents.Kind.TICKET_CREATED);
        return result(actor,operation,key,request,ticket);
    }
    public Page list(IdentitySession actor,UUID account,UUID site,boolean admin,Ticket.State state,Ticket.Priority requested,UUID equipment,int page,int size) {
        bounds(page,size); var scope=assets.authorizeSite(actor,account,site,admin); if(equipment!=null) { assets.requireEquipment(scope,equipment); }
        return new Page(store.list(account,site,state,requested,equipment,page*size,size),store.count(account,site,state,requested,equipment),page,size);
    }
    public Ticket detail(IdentitySession actor,UUID account,UUID site,UUID ticket,boolean admin) { assets.authorizeSite(actor,account,site,admin); return store.lock(account,site,ticket); }
    public CommandReceipts.ResourceResult propose(IdentitySession actor,UUID account,UUID site,UUID ticketId,long expected,ClientProposal proposal,String key) {
        var scope=assets.authorizeSite(actor,account,site,false); clientOnly(scope); var ticket=store.lock(account,site,ticketId);
        String operation="ticket-proposal/"+ticketId,request=CommandFingerprint.of(Long.toString(expected),money(proposal.remuneration()),money(proposal.expenses()));
        var replay=receipts.replayResource(actor.userId(),operation,key,request); if(replay.isPresent()) { return replay.get(); }
        ticket.expect(expected); ticket.proposalEditable(); var next=store.propose(account,ticket,proposal,actor.userId(),clock.instant()); changed(account,next,actor,BusinessEvents.Kind.TICKET_CHANGED);
        return result(actor,operation,key,request,next);
    }
    public CommandReceipts.ResourceResult transition(IdentitySession actor,UUID account,UUID site,UUID ticketId,long expected,Ticket.Action action,Ticket.Priority retained,UUID contractRevision,String reason,String key) {
        boolean client=action==Ticket.Action.CLOSE || action==Ticket.Action.REOPEN;
        var scope=assets.authorizeSite(actor,account,site,!client); if(client) { clientOnly(scope); }
        var ticket=store.lock(account,site,ticketId); String checkedReason=Ticket.text(reason,2000);
        if(action==Ticket.Action.QUALIFY && retained==null) { throw TicketFailure.invalid(); }
        String operation="ticket-"+action.name().toLowerCase(java.util.Locale.ROOT)+"/"+ticketId;
        String request=CommandFingerprint.of(Long.toString(expected),retained==null?null:retained.name(),string(contractRevision),checkedReason);
        var replay=receipts.replayResource(actor.userId(),operation,key,request); if(replay.isPresent()) { return replay.get(); }
        ticket.expect(expected); Ticket.State state=ticket.transition(action);
        if(action==Ticket.Action.QUALIFY && contractRevision!=null) { contracts.requireApplicable(account,contractRevision,site,ticket.equipmentId(),ticket.createdAt()); }
        var next=store.transition(account,ticket,action,state,retained,contractRevision,checkedReason,actor.userId(),clock.instant()); changed(account,next,actor,BusinessEvents.Kind.TICKET_CHANGED);
        return result(actor,operation,key,request,next);
    }
    public List<TicketStore.Proposal> proposals(IdentitySession actor,UUID account,UUID site,UUID ticket,boolean admin,long after,int size) { historyBounds(after,size); detail(actor,account,site,ticket,admin); return store.proposals(account,site,ticket,after,size); }
    public List<TicketStore.Change> history(IdentitySession actor,UUID account,UUID site,UUID ticket,boolean admin,long after,int size) { historyBounds(after,size); detail(actor,account,site,ticket,admin); return store.history(account,site,ticket,after,size); }
    public List<TicketStore.Message> messages(IdentitySession actor,UUID account,UUID site,UUID ticket,boolean admin,boolean internal,long after,int size) { historyBounds(after,size); detail(actor,account,site,ticket,admin || internal); return store.messages(account,site,ticket,internal,after,size); }
    public CommandReceipts.ResourceResult message(IdentitySession actor,UUID account,UUID site,UUID ticketId,boolean admin,boolean internal,long expected,String text,String key) {
        assets.authorizeSite(actor,account,site,admin || internal); var ticket=store.lock(account,site,ticketId);
        String checked=Ticket.text(text,4000),operation=(internal?"ticket-note/":"ticket-message/")+ticketId,request=CommandFingerprint.of(Long.toString(expected),checked);
        var replay=receipts.replayResource(actor.userId(),operation,key,request); if(replay.isPresent()) { return replay.get(); }
        ticket.expect(expected); ticket.exchangeAllowed(); var message=store.message(account,ticket,internal,checked,actor.userId(),clock.instant());
        audit.record(internal?SecurityAudit.Action.TICKET_INTERNAL_NOTE:SecurityAudit.Action.TICKET_MESSAGE,actor.userId(),ticketId);
        events.append(internal?BusinessEvents.Kind.TICKET_INTERNAL_NOTE:BusinessEvents.Kind.TICKET_MESSAGE,account,site,ticketId,message.number(),actor.userId(),clock.instant());
        return receipts.recordResource(actor.userId(),operation,key,request,message.messageId(),message.number(),internal?"INTERNAL_NOTE":"PUBLIC_MESSAGE");
    }
    private void changed(UUID account,Ticket ticket,IdentitySession actor,BusinessEvents.Kind kind) { audit.record(SecurityAudit.Action.TICKET_CHANGED,actor.userId(),ticket.ticketId()); events.append(kind,account,ticket.siteId(),ticket.ticketId(),ticket.version(),actor.userId(),clock.instant()); }
    private CommandReceipts.ResourceResult result(IdentitySession actor,String operation,String key,String request,Ticket ticket) { return receipts.recordResource(actor.userId(),operation,key,request,ticket.ticketId(),ticket.version(),ticket.state().name()); }
    private static void clientOnly(AssetAccess.SiteContext context) { if(context.administrator()) { throw new AccessDeniedException("Client operation required."); } }
    private static String string(UUID value) { return value==null?null:value.toString(); }
    private static String money(java.math.BigDecimal value) { return value==null?null:value.toPlainString(); }
    private static void bounds(int page,int size) { if(page<0 || page>10000 || size<1 || size>100) { throw TicketFailure.invalid(); } }
    private static void historyBounds(long after,int size) { if(after< -1 || size<1 || size>100) { throw TicketFailure.invalid(); } }
}
