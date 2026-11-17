package fr.entreprise.maintenance.features.clients.application;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.audit.publicapi.SecurityAudit;
import fr.entreprise.maintenance.features.clients.domain.ClientAdmissions;
import fr.entreprise.maintenance.features.clients.domain.ClientDossier;
import fr.entreprise.maintenance.features.clients.domain.ClientFailure;
import fr.entreprise.maintenance.features.clients.domain.ClientProfile;
import fr.entreprise.maintenance.features.clients.domain.InvitationTokens;
import fr.entreprise.maintenance.features.identity.publicapi.AdmissionIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.identity.publicapi.RegistrationAccounts;
import fr.entreprise.maintenance.features.notifications.publicapi.MailOutbox;
import fr.entreprise.maintenance.shared.idempotency.CommandReceipts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ClientOnboardingService implements fr.entreprise.maintenance.features.clients.publicapi.GoogleClientAdmission {
    private final ClientAdmissions store;
    private final AdmissionIdentity identity;
    private final RegistrationAccounts registration;
    private final MailOutbox mail;
    private final SecurityAudit audit;
    private final CommandReceipts receipts;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final Duration invitationTtl;
    private final URI origin;
    public ClientOnboardingService(ClientAdmissions store, AdmissionIdentity identity, RegistrationAccounts registration, MailOutbox mail,
            SecurityAudit audit, CommandReceipts receipts, Clock clock, PlatformTransactionManager manager,
            @Value("${app.onboarding.invitation-ttl:48h}") Duration invitationTtl, @Value("${app.identity.frontend-origin}") URI origin) {
        if (invitationTtl.isNegative() || invitationTtl.isZero() || invitationTtl.compareTo(Duration.ofDays(7))>0) {
            throw new IllegalArgumentException("Invalid invitation lifetime.");
        }
        // The identity module already validates this operator-controlled origin. No request URL/Host is used.
        this.store=store; this.identity=identity; this.registration=registration; this.mail=mail; this.audit=audit;
        this.receipts=receipts; this.clock=clock; this.tx=new TransactionTemplate(manager); this.invitationTtl=invitationTtl; this.origin=origin;
    }
    public ClientAdmissions.Invitation invite(IdentitySession actor, String email, String peer) {
        String normalized = identity.normalizeEmail(email);
        identity.limit("INVITATION_SEND",normalized,peer);
        return tx.execute(status -> {
            identity.administrator(actor,null);
            String raw=InvitationTokens.generate(); var now=clock.instant();
            var result=store.invite(normalized,email.strip(),actor.userId(),InvitationTokens.fingerprint(raw),now,now.plus(invitationTtl));
            invitationMail(result,raw);
            audit.record(SecurityAudit.Action.CLIENT_INVITED,actor.userId(),result.invitationId()); return result;
        });
    }
    public ClientAdmissions.Invitation invitation(IdentitySession actor, UUID id) {
        return tx.execute(status -> { identity.administrator(actor,null); return store.invitation(id); });
    }
    public ClientAdmissions.Invitation resend(IdentitySession actor, UUID id, long expected, String peer) {
        identity.limit("INVITATION_RESEND",id.toString(),peer);
        return tx.execute(status -> {
            identity.administrator(actor,null); String raw=InvitationTokens.generate(); var now=clock.instant();
            var result=store.resend(id,actor.userId(),expected,InvitationTokens.fingerprint(raw),now,now.plus(invitationTtl));
            invitationMail(result,raw);
            audit.record(SecurityAudit.Action.CLIENT_INVITATION_RESENT,actor.userId(),id); return result;
        });
    }
    private void invitationMail(ClientAdmissions.Invitation invitation, String raw) {
        mail.enqueue(invitation.email(),"Invitation à compléter votre dossier client",
                "Ouvrez volontairement l'application pour accepter cette invitation personnelle :\n"
                        +origin.resolve("/onboarding/client/invitation")+"#token="+raw
                        +"\nCe lien ne valide pas votre dossier. Connectez-vous à votre accès existant ou créez un nouvel accès et vérifiez votre email.",invitation.expiresAt());
    }
    public ClientDossier register(String raw, String email, String password, String peer) {
        String normalized=identity.normalizeEmail(email); identity.limit("CLIENT_REGISTRATION",normalized,peer);
        String fingerprint=InvitationTokens.fingerprint(raw);
        store.checkLink(fingerprint,normalized,clock.instant());
        var prepared=registration.prepare(email,password); // Argon2 before acquiring invitation/user locks.
        return tx.execute(status -> {
            var link=store.lockLink(fingerprint,prepared.email(),clock.instant());
            UUID user=registration.client(prepared);
            var dossier=store.accept(link,fingerprint,user,clock.instant());
            audit.record(SecurityAudit.Action.CLIENT_INVITATION_ACCEPTED,user,dossier.dossierId()); return dossier;
        });
    }
    @Override public void checkInvitation(String fingerprint,String email) { store.checkLink(fingerprint,email,clock.instant()); }
    @Override @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void lockInvitation(String fingerprint,String email) { store.lockLink(fingerprint,email,clock.instant()); }
    @Override @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void enroll(UUID user,String fingerprint,String email) {
        var link=store.lockLink(fingerprint,email,clock.instant());
        var dossier=store.accept(link,fingerprint,user,clock.instant());
        audit.record(SecurityAudit.Action.CLIENT_INVITATION_ACCEPTED,user,dossier.dossierId());
    }
    public ClientDossier accept(IdentitySession actor, String raw, String peer) {
        identity.limit("INVITATION_ACCEPT",actor==null ? "" : actor.userId().toString(),peer);
        return tx.execute(status -> {
            var applicant=identity.applicant(actor,AdmissionIdentity.Channel.WEB,AdmissionIdentity.Role.CLIENT,true);
            String fingerprint=InvitationTokens.fingerprint(raw);
            var link=store.lockLink(fingerprint,applicant.email(),clock.instant());
            var dossier=store.accept(link,fingerprint,applicant.userId(),clock.instant());
            audit.record(SecurityAudit.Action.CLIENT_INVITATION_ACCEPTED,actor.userId(),dossier.dossierId()); return dossier;
        });
    }
    public ClientDossier own(IdentitySession actor) {
        return tx.execute(status -> { identity.applicant(actor,AdmissionIdentity.Channel.WEB,AdmissionIdentity.Role.CLIENT,false); return store.own(actor.userId()); });
    }
    public ClientDossier save(IdentitySession actor, ClientProfile profile, long expected) {
        return tx.execute(status -> {
            identity.applicant(actor,AdmissionIdentity.Channel.WEB,AdmissionIdentity.Role.CLIENT,false);
            var dossier=store.own(actor.userId()); dossier.expect(expected); dossier.editable();
            return store.save(dossier,profile,actor.userId(),clock.instant());
        });
    }
    public CommandReceipts.Result submit(IdentitySession actor, long expected, String key) {
        return tx.execute(status -> {
            identity.applicant(actor,AdmissionIdentity.Channel.WEB,AdmissionIdentity.Role.CLIENT,true);
            var dossier=store.own(actor.userId()); String operation="client-submit/"+dossier.dossierId();
            var replay=receipts.replay(actor.userId(),operation,key,Long.toString(expected));
            if (replay.isPresent()) { return replay.get(); }
            dossier.expect(expected); dossier.submittable();
            var next=store.transition(dossier,ClientDossier.State.SUBMITTED,actor.userId(),null,clock.instant());
            audit.record(SecurityAudit.Action.CLIENT_DOSSIER_SUBMITTED,actor.userId(),dossier.dossierId());
            return receipts.record(actor.userId(),operation,key,Long.toString(expected),next.version(),next.state().name());
        });
    }
    public List<ClientDossier> review(IdentitySession actor, int page, int size, ClientDossier.State state) {
        bounds(page,size);
        return tx.execute(status -> { identity.administrator(actor,null); return store.review((long)page*size,size,state); });
    }
    public ClientDossier detail(IdentitySession actor, UUID id) {
        return tx.execute(status -> { identity.administrator(actor,null); return store.lock(id); });
    }
    public List<ClientAdmissions.History> ownHistory(IdentitySession actor, long after, int size) {
        historyBounds(after,size);
        return tx.execute(status -> { identity.applicant(actor,AdmissionIdentity.Channel.WEB,AdmissionIdentity.Role.CLIENT,false); return store.history(store.own(actor.userId()).dossierId(),after,size); });
    }
    public List<ClientAdmissions.History> history(IdentitySession actor, UUID id, long after, int size) {
        historyBounds(after,size);
        return tx.execute(status -> { identity.administrator(actor,null); store.lock(id); return store.history(id,after,size); });
    }
    public ClientDossier decide(IdentitySession actor, UUID id, long expected, ClientDossier.Decision decision, String reason) {
        String checkedReason=ClientDossier.reason(reason);
        return tx.execute(status -> {
            // Authorize before looking up a private identifier, then acquire users in deterministic order.
            identity.checkAdministrator(actor);
            UUID user=store.owner(id); identity.administrator(actor,user);
            var dossier=store.lock(id); dossier.expect(expected); dossier.decidable();
            String email=identity.applicantEmail(user,AdmissionIdentity.Role.CLIENT);
            if (decision==ClientDossier.Decision.APPROVED) { identity.approveClient(user); }
            if (decision==ClientDossier.Decision.REJECTED) { identity.rejectApplicant(user,AdmissionIdentity.Role.CLIENT); }
            var result=store.transition(dossier,ClientDossier.State.valueOf(decision.name()),actor.userId(),checkedReason,clock.instant());
            mail.enqueue(email,"Votre dossier client a été examiné","Une décision est disponible pour votre dossier client : "+decision.name()
                    +". Consultez l'application ou contactez l'opérateur. Aucun motif privé n'est copié dans ce courriel.",clock.instant().plusSeconds(86400));
            audit.record(SecurityAudit.Action.CLIENT_DOSSIER_DECIDED,actor.userId(),id); return result;
        });
    }
    private static void bounds(int page,int size) {
        if (page<0 || page>10000 || size<1 || size>100) { throw new ClientFailure(ClientFailure.Code.INVALID_CLIENT_INPUT); }
    }
    private static void historyBounds(long after,int size) {
        if (after< -1 || size<1 || size>100) { throw new ClientFailure(ClientFailure.Code.INVALID_CLIENT_INPUT); }
    }
}
