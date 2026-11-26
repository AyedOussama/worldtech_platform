package fr.entreprise.maintenance.features.contracts.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.assets.publicapi.AssetAccess;
import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.clients.publicapi.ClientAccountAccess;
import fr.entreprise.maintenance.features.contracts.domain.ContractDefinition;
import fr.entreprise.maintenance.features.contracts.domain.ContractFailure;
import fr.entreprise.maintenance.features.contracts.domain.ContractRevision;
import fr.entreprise.maintenance.features.contracts.domain.ContractStore;
import fr.entreprise.maintenance.features.contracts.publicapi.ContractCoverage;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.notifications.publicapi.BusinessEvents;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContractService implements ContractCoverage {
    public record Page(List<ContractStore.Summary> items,long total,int page,int size) { }
    public record SitePage(List<ContractStore.SiteSummary> items,long total,int page,int size) { }
    private final ContractStore store;
    private final ClientAccountAccess clients;
    private final AssetAccess assets;
    private final SecurityAudit audit;
    private final BusinessEvents events;
    private final Clock clock;
    public ContractService(ContractStore store,ClientAccountAccess clients,AssetAccess assets,SecurityAudit audit,BusinessEvents events,Clock clock) { this.store=store; this.clients=clients; this.assets=assets; this.audit=audit; this.events=events; this.clock=clock; }
    public ContractRevision create(IdentitySession actor,UUID account,ContractDefinition definition) {
        clients.authorize(actor,account,true); validateCoverage(actor,account,definition);
        var revision=store.create(account,definition,actor.userId(),clock.instant()); changed(actor,account,revision); return revision;
    }
    public ContractRevision revise(IdentitySession actor,UUID account,UUID contract,long expected,ContractDefinition definition) {
        clients.authorize(actor,account,true); var current=store.current(account,contract); current.expect(expected); validateCoverage(actor,account,definition);
        var revision=store.revise(account,current,definition,actor.userId(),clock.instant()); changed(actor,account,revision); return revision;
    }
    private void validateCoverage(IdentitySession actor,UUID account,ContractDefinition definition) {
        for(var c:definition.coverage()) { var site=assets.authorizeSite(actor,account,c.siteId(),true); for(UUID equipment:c.equipmentIds()) { assets.requireEquipment(site,equipment); } }
    }
    private void changed(IdentitySession actor,UUID account,ContractRevision revision) {
        audit.record(SecurityAudit.Action.CONTRACT_CHANGED,actor.userId(),revision.revisionId());
        events.append(BusinessEvents.Kind.CONTRACT_REVISED,account,null,revision.contractId(),revision.version(),actor.userId(),clock.instant());
    }
    public Page list(IdentitySession actor,UUID account,int page,int size) { bounds(page,size); clients.authorize(actor,account,true); return new Page(store.list(account,page*size,size),store.count(account),page,size); }
    public ContractRevision current(IdentitySession actor,UUID account,UUID contract) { clients.authorize(actor,account,true); return store.current(account,contract); }
    public ContractRevision revision(IdentitySession actor,UUID account,UUID contract,UUID revision) { clients.authorize(actor,account,true); return store.revision(account,contract,revision); }
    public List<ContractStore.Summary> history(IdentitySession actor,UUID account,UUID contract,long after,int size) {
        historyBounds(after,size); clients.authorize(actor,account,true); store.current(account,contract); return store.history(account,contract,after,size);
    }
    public SitePage siteList(IdentitySession actor,UUID account,UUID site,Instant at,int page,int size) {
        bounds(page,size); assets.authorizeSite(actor,account,site,false); Instant when=at==null?clock.instant():at;
        if(when.isBefore(Instant.parse("2000-01-01T00:00:00Z")) || when.isAfter(Instant.parse("2201-01-01T00:00:00Z"))) { throw ContractFailure.invalid(); }
        return new SitePage(store.siteList(account,site,when,page*size,size),store.siteCount(account,site,when),page,size);
    }
    public ContractStore.SiteRevision siteRevision(IdentitySession actor,UUID account,UUID site,UUID revision) { assets.authorizeSite(actor,account,site,false); return store.siteRevision(account,site,revision); }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void requireAccountRevision(UUID accountId,UUID revisionId) { store.requireRevision(accountId,revisionId); }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void requireApplicable(UUID accountId,UUID revisionId,UUID siteId,UUID equipmentId,Instant declaredAt) { store.applicable(accountId,revisionId,siteId,equipmentId,declaredAt); }
    private static void bounds(int page,int size) { if(page<0 || page>10000 || size<1 || size>100) { throw ContractFailure.invalid(); } }
    private static void historyBounds(long after,int size) { if(after< -1 || size<1 || size>100) { throw ContractFailure.invalid(); } }
}
