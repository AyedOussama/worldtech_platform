package fr.entreprise.maintenance.features.technicians.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.identity.publicapi.AdmissionIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.identity.publicapi.RegistrationAccounts;
import fr.entreprise.maintenance.features.notifications.publicapi.MailOutbox;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianAdmissions;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianDossier;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianFailure;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianProfile;
import fr.entreprise.maintenance.shared.idempotency.CommandReceipts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TechnicianOnboardingService implements fr.entreprise.maintenance.features.technicians.publicapi.GoogleTechnicianAdmission {
    private final TechnicianAdmissions store;
    private final AdmissionIdentity identity;
    private final RegistrationAccounts registration;
    private final MailOutbox mail;
    private final SecurityAudit audit;
    private final CommandReceipts receipts;
    private final Clock clock;
    private final TransactionTemplate tx;
    public TechnicianOnboardingService(TechnicianAdmissions store,AdmissionIdentity identity,RegistrationAccounts registration,MailOutbox mail,
            SecurityAudit audit,CommandReceipts receipts,Clock clock,PlatformTransactionManager manager) {
        this.store=store; this.identity=identity; this.registration=registration; this.mail=mail; this.audit=audit; this.receipts=receipts; this.clock=clock; this.tx=new TransactionTemplate(manager);
    }
    public TechnicianDossier register(String email,String password,String peer) {
        String normalized=identity.normalizeEmail(email); identity.limit("TECHNICIAN_REGISTRATION",normalized,peer);
        var prepared=registration.prepare(email,password);
        return tx.execute(status -> {
            UUID user=registration.technician(prepared); var dossier=store.open(user,clock.instant());
            audit.record(SecurityAudit.Action.TECHNICIAN_REGISTERED,user,dossier.dossierId()); return dossier;
        });
    }
    public TechnicianDossier open(IdentitySession actor,AdmissionIdentity.Channel channel) {
        return tx.execute(status -> { identity.applicant(actor,channel,AdmissionIdentity.Role.TECHNICIAN,false); return store.open(actor.userId(),clock.instant()); });
    }
    @Override @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void enroll(UUID user) {
        var dossier=store.open(user,clock.instant());
        audit.record(SecurityAudit.Action.TECHNICIAN_REGISTERED,user,dossier.dossierId());
    }
    public TechnicianDossier own(IdentitySession actor,AdmissionIdentity.Channel channel) {
        return tx.execute(status -> { identity.applicant(actor,channel,AdmissionIdentity.Role.TECHNICIAN,false); return store.own(actor.userId()); });
    }
    public TechnicianDossier save(IdentitySession actor,AdmissionIdentity.Channel channel,TechnicianProfile profile,long expected) {
        return tx.execute(status -> {
            identity.applicant(actor,channel,AdmissionIdentity.Role.TECHNICIAN,false);
            var dossier=store.own(actor.userId()); dossier.expect(expected); dossier.editable();
            return store.save(dossier,profile,actor.userId(),clock.instant());
        });
    }
    public CommandReceipts.Result submit(IdentitySession actor,AdmissionIdentity.Channel channel,long expected,String key) {
        return tx.execute(status -> {
            identity.applicant(actor,channel,AdmissionIdentity.Role.TECHNICIAN,true);
            var dossier=store.own(actor.userId()); String operation="technician-submit/"+dossier.dossierId();
            var replay=receipts.replay(actor.userId(),operation,key,Long.toString(expected)); if (replay.isPresent()) { return replay.get(); }
            dossier.expect(expected); dossier.submittable();
            var next=store.transition(dossier,TechnicianDossier.State.SUBMITTED,actor.userId(),null,clock.instant());
            audit.record(SecurityAudit.Action.TECHNICIAN_DOSSIER_SUBMITTED,actor.userId(),dossier.dossierId());
            return receipts.record(actor.userId(),operation,key,Long.toString(expected),next.version(),next.state().name());
        });
    }
    public List<TechnicianDossier> review(IdentitySession actor,int page,int size,TechnicianDossier.State state) {
        bounds(page,size);
        return tx.execute(status -> { identity.administrator(actor,null); return store.review((long)page*size,size,state); });
    }
    public TechnicianDossier detail(IdentitySession actor,UUID id) {
        return tx.execute(status -> { identity.administrator(actor,null); return store.lock(id); });
    }
    public List<TechnicianAdmissions.History> ownHistory(IdentitySession actor,AdmissionIdentity.Channel channel,long after,int size) {
        historyBounds(after,size);
        return tx.execute(status -> { identity.applicant(actor,channel,AdmissionIdentity.Role.TECHNICIAN,false); return store.history(store.own(actor.userId()).dossierId(),after,size); });
    }
    public List<TechnicianAdmissions.History> history(IdentitySession actor,UUID id,long after,int size) {
        historyBounds(after,size);
        return tx.execute(status -> { identity.administrator(actor,null); store.lock(id); return store.history(id,after,size); });
    }
    public TechnicianDossier decide(IdentitySession actor,UUID id,long expected,TechnicianDossier.Decision decision,String reason) {
        String checked=TechnicianDossier.reason(reason);
        return tx.execute(status -> {
            identity.checkAdministrator(actor); UUID user=store.owner(id); identity.administrator(actor,user);
            var dossier=store.lock(id); dossier.expect(expected); dossier.decidable(decision);
            String email=identity.applicantEmail(user,AdmissionIdentity.Role.TECHNICIAN);
            if (decision==TechnicianDossier.Decision.REJECTED) { identity.rejectApplicant(user,AdmissionIdentity.Role.TECHNICIAN); }
            var next=store.transition(dossier,TechnicianDossier.State.valueOf(decision.name()),actor.userId(),checked,clock.instant());
            mail.enqueue(email,"Votre dossier technicien a été examiné","Une décision est disponible pour votre dossier technicien : "+decision.name()+". Consultez l'application ou contactez l'opérateur.",clock.instant().plusSeconds(86400));
            audit.record(SecurityAudit.Action.TECHNICIAN_DOSSIER_DECIDED,actor.userId(),id); return next;
        });
    }
    private static void bounds(int page,int size) {
        if (page<0 || page>10000 || size<1 || size>100) { throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT); }
    }
    private static void historyBounds(long after,int size) {
        if (after< -1 || size<1 || size>100) { throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT); }
    }
}
