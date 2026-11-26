package fr.entreprise.maintenance.features.missions.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.assets.publicapi.AssetAccess;
import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.contracts.publicapi.ContractCoverage;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.missions.domain.Mission;
import fr.entreprise.maintenance.features.missions.domain.MissionFailure;
import fr.entreprise.maintenance.features.missions.domain.MissionFinancialRevision;
import fr.entreprise.maintenance.features.missions.domain.MissionStore;
import fr.entreprise.maintenance.features.notifications.publicapi.BusinessEvents;
import fr.entreprise.maintenance.features.tickets.publicapi.TicketContext;
import fr.entreprise.maintenance.shared.idempotency.CommandFingerprint;
import fr.entreprise.maintenance.shared.idempotency.CommandReceipts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MissionService {
    public record Page(List<Mission> items,long total,int page,int size) { }
    public record Detail(Mission mission,MissionFinancialRevision financial) { }
    private final MissionStore store;
    private final AssetAccess assets;
    private final TicketContext tickets;
    private final ContractCoverage contracts;
    private final CommandReceipts receipts;
    private final SecurityAudit audit;
    private final BusinessEvents events;
    private final Clock clock;
    public MissionService(MissionStore store,AssetAccess assets,TicketContext tickets,ContractCoverage contracts,CommandReceipts receipts,
            SecurityAudit audit,BusinessEvents events,Clock clock) {
        this.store=store; this.assets=assets; this.tickets=tickets; this.contracts=contracts; this.receipts=receipts;
        this.audit=audit; this.events=events; this.clock=clock;
    }
    public CommandReceipts.ResourceResult create(IdentitySession actor,UUID account,UUID site,UUID ticketId,UUID contractRevisionId,
            String title,String publicSummary,List<String> skills,Instant windowStart,Instant windowEnd,BigDecimal publicLatitude,
            BigDecimal publicLongitude,String publicLabel,String key) {
        assets.authorizeSite(actor,account,site,true);
        if(ticketId!=null && tickets.findSiteTicket(account,site,ticketId).isEmpty()) { throw MissionFailure.invalid(); }
        if(contractRevisionId!=null) { contracts.requireAccountRevision(account,contractRevisionId); }
        Instant now=clock.instant();
        var draft=Mission.draft(UUID.randomUUID(),account,site,ticketId,contractRevisionId,title,publicSummary,skills,windowStart,windowEnd,
                publicLatitude,publicLongitude,publicLabel,now);
        String operation="mission-create/"+site;
        String request=CommandFingerprint.of(account.toString(),string(ticketId),string(contractRevisionId),draft.title(),draft.publicSummary(),
                CommandFingerprint.of(draft.skills().toArray(String[]::new)),draft.windowStart().toString(),draft.windowEnd().toString(),
                draft.publicLatitude().toPlainString(),draft.publicLongitude().toPlainString(),string(draft.publicLabel()));
        var replay=receipts.replayResource(actor.userId(),operation,key,request); if(replay.isPresent()) { return replay.get(); }
        var mission=store.create(account,site,draft,actor.userId(),now);
        changed(account,mission,actor,BusinessEvents.Kind.MISSION_CREATED);
        return result(actor,operation,key,request,mission);
    }
    public Page list(IdentitySession actor,UUID account,UUID site,Mission.State state,int page,int size) {
        bounds(page,size); assets.authorizeSite(actor,account,site,true);
        return new Page(store.list(account,site,state,page*size,size),store.count(account,site,state),page,size);
    }
    public Detail detail(IdentitySession actor,UUID account,UUID site,UUID missionId) {
        assets.authorizeSite(actor,account,site,true); var mission=store.lock(account,site,missionId);
        return new Detail(mission,store.currentFinancial(account,site,missionId).orElse(null));
    }
    public CommandReceipts.ResourceResult publish(IdentitySession actor,UUID account,UUID site,UUID missionId,long expected,
            String remuneration,String expenses,String reason,String key) {
        assets.authorizeSite(actor,account,site,true);
        var current=store.lock(account,site,missionId);
        var validated=MissionFinancialRevision.create(0,remuneration,expenses,reason,actor.userId(),clock.instant());
        String operation="mission-publish/"+missionId;
        String request=CommandFingerprint.of(Long.toString(expected),validated.remuneration().toPlainString(),
                string(validated.expenses()),string(validated.reason()));
        var replay=receipts.replayResource(actor.userId(),operation,key,request); if(replay.isPresent()) { return replay.get(); }
        Instant now=clock.instant();
        current.expect(expected); current.requirePublishable(now);
        UUID equipment=null;
        if(current.ticketId()!=null) {
            var ticket=tickets.findSiteTicket(account,site,current.ticketId()).orElseThrow(MissionFailure::invalid);
            if(!ticket.qualified()) { throw MissionFailure.conflict(); }
            equipment=ticket.equipmentId();
        }
        if(current.contractRevisionId()!=null) { contracts.requireApplicable(account,current.contractRevisionId(),site,equipment,now); }
        var revision=MissionFinancialRevision.create(0,remuneration,expenses,reason,actor.userId(),now);
        var saved=store.publish(account,current,revision,now);
        changed(account,saved,actor,BusinessEvents.Kind.MISSION_PUBLISHED);
        return result(actor,operation,key,request,saved);
    }
    public CommandReceipts.ResourceResult revise(IdentitySession actor,UUID account,UUID site,UUID missionId,long expected,
            String remuneration,String expenses,String reason,String key) {
        assets.authorizeSite(actor,account,site,true);
        var current=store.lock(account,site,missionId);
        var validated=MissionFinancialRevision.create(0,remuneration,expenses,reason,actor.userId(),clock.instant());
        String operation="mission-financial/"+missionId;
        String request=CommandFingerprint.of(Long.toString(expected),validated.remuneration().toPlainString(),
                string(validated.expenses()),string(validated.reason()));
        var replay=receipts.replayResource(actor.userId(),operation,key,request); if(replay.isPresent()) { return replay.get(); }
        current.expect(expected); current.revisable();
        long number=current.financialRevision()+1; current.revised(number);
        Instant now=clock.instant();
        var revision=MissionFinancialRevision.create(number,remuneration,expenses,reason,actor.userId(),now);
        var saved=store.revise(account,current,revision,now);
        changed(account,saved,actor,BusinessEvents.Kind.MISSION_REVISED);
        return result(actor,operation,key,request,saved);
    }
    public List<MissionFinancialRevision> financialRevisions(IdentitySession actor,UUID account,UUID site,UUID missionId,long after,int size) {
        historyBounds(after,size); assets.authorizeSite(actor,account,site,true); store.lock(account,site,missionId);
        return store.financialRevisions(account,site,missionId,after,size);
    }
    private void changed(UUID account,Mission mission,IdentitySession actor,BusinessEvents.Kind kind) {
        audit.record(SecurityAudit.Action.MISSION_CHANGED,actor.userId(),mission.missionId());
        events.append(kind,account,mission.siteId(),mission.missionId(),mission.version(),actor.userId(),clock.instant());
    }
    private CommandReceipts.ResourceResult result(IdentitySession actor,String operation,String key,String request,Mission mission) {
        return receipts.recordResource(actor.userId(),operation,key,request,mission.missionId(),mission.version(),mission.state().name());
    }
    private static String string(String value) { return value; }
    private static String string(UUID value) { return value==null?null:value.toString(); }
    private static String string(BigDecimal value) { return value==null?null:value.toPlainString(); }
    private static void bounds(int page,int size) { if(page<0 || page>10000 || size<1 || size>100) { throw MissionFailure.invalid(); } }
    private static void historyBounds(long after,int size) { if(after< -1 || size<1 || size>100) { throw MissionFailure.invalid(); } }
}
