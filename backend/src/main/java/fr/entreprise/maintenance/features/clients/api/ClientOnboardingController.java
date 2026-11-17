package fr.entreprise.maintenance.features.clients.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.clients.application.ClientOnboardingService;
import fr.entreprise.maintenance.features.clients.domain.ClientAdmissions;
import fr.entreprise.maintenance.features.clients.domain.ClientDossier;
import fr.entreprise.maintenance.features.clients.domain.ClientFailure;
import fr.entreprise.maintenance.features.clients.domain.ClientProfile;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.shared.idempotency.CommandReceipts;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientOnboardingController {
    private final ClientOnboardingService clients;
    public ClientOnboardingController(ClientOnboardingService clients) { this.clients=clients; }
    @PostMapping("/api/web/v1/admin/client-invitations")
    public ResponseEntity<InvitationView> invite(@AuthenticationPrincipal IdentitySession actor,@Valid @RequestBody EmailBody body,HttpServletRequest request) {
        var result=clients.invite(actor,body.email(),request.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/web/v1/admin/client-invitations/"+result.invitationId())).cacheControl(CacheControl.noStore()).eTag(tag(result.version())).body(invitation(result));
    }
    @GetMapping("/api/web/v1/admin/client-invitations/{id}")
    public ResponseEntity<InvitationView> invitation(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id) {
        var result=clients.invitation(actor,id); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(result.version())).body(invitation(result));
    }
    @PostMapping("/api/web/v1/admin/client-invitations/{id}/resend")
    public ResponseEntity<InvitationView> resend(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestHeader("If-Match") String expected,HttpServletRequest request) {
        var result=clients.resend(actor,id,version(expected),request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(result.version())).body(invitation(result));
    }
    @PostMapping("/api/web/v1/onboarding/client/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationBody body,HttpServletRequest request) {
        clients.register(body.token(),body.email(),body.password(),request.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/web/v1/onboarding/client")).cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/api/web/v1/onboarding/client/accept-invitation")
    public ResponseEntity<DossierView> accept(@AuthenticationPrincipal IdentitySession actor,@Valid @RequestBody TokenBody body,HttpServletRequest request) {
        return view(clients.accept(actor,body.token(),request.getRemoteAddr()));
    }
    @GetMapping("/api/web/v1/onboarding/client")
    public ResponseEntity<DossierView> own(@AuthenticationPrincipal IdentitySession actor) { return view(clients.own(actor)); }
    @PostMapping("/api/web/v1/onboarding/client/profile")
    public ResponseEntity<DossierView> save(@AuthenticationPrincipal IdentitySession actor,@Valid @RequestBody ProfileBody body,@RequestHeader("If-Match") String expected) {
        return view(clients.save(actor,body.profile(),version(expected)));
    }
    @PostMapping("/api/web/v1/onboarding/client/submit")
    public ResponseEntity<CommandReceipts.Result> submit(@AuthenticationPrincipal IdentitySession actor,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key) {
        var result=clients.submit(actor,version(expected),key);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(result.version())).body(result);
    }
    @GetMapping("/api/web/v1/onboarding/client/history")
    public ResponseEntity<List<HistoryView>> ownHistory(@AuthenticationPrincipal IdentitySession actor,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size) {
        return history(clients.ownHistory(actor,after,size));
    }
    @GetMapping("/api/web/v1/admin/onboardings/clients")
    public ResponseEntity<List<SummaryView>> review(@AuthenticationPrincipal IdentitySession actor,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="SUBMITTED") ClientDossier.State state) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(clients.review(actor,page,size,state).stream()
                .map(d -> new SummaryView(d.dossierId(),d.state().name(),d.version(),d.createdAt())).toList());
    }
    @GetMapping("/api/web/v1/admin/onboardings/clients/{id}")
    public ResponseEntity<AdminView> detail(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id) {
        var d=clients.detail(actor,id); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(d.version())).body(new AdminView(d.userId(),dossier(d)));
    }
    @GetMapping("/api/web/v1/admin/onboardings/clients/{id}/history")
    public ResponseEntity<List<AdminHistoryView>> history(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(clients.history(actor,id,after,size).stream()
                .map(h -> new AdminHistoryView(h.revision(),h.actorUserId(),h.action(),h.reason(),h.occurredAt())).toList());
    }
    @PostMapping("/api/web/v1/admin/onboardings/clients/{id}/approve")
    public ResponseEntity<DossierView> approve(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestHeader("If-Match") String expected,@Valid @RequestBody DecisionBody body) {
        return view(clients.decide(actor,id,version(expected),ClientDossier.Decision.APPROVED,body.reason()));
    }
    @PostMapping("/api/web/v1/admin/onboardings/clients/{id}/request-information")
    public ResponseEntity<DossierView> requestInformation(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestHeader("If-Match") String expected,@Valid @RequestBody DecisionBody body) {
        return view(clients.decide(actor,id,version(expected),ClientDossier.Decision.NEEDS_INFORMATION,body.reason()));
    }
    @PostMapping("/api/web/v1/admin/onboardings/clients/{id}/reject")
    public ResponseEntity<DossierView> reject(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestHeader("If-Match") String expected,@Valid @RequestBody DecisionBody body) {
        return view(clients.decide(actor,id,version(expected),ClientDossier.Decision.REJECTED,body.reason()));
    }
    private static ResponseEntity<DossierView> view(ClientDossier d) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(d.version())).body(dossier(d)); }
    private static DossierView dossier(ClientDossier d) {
        var p=d.profile(); return new DossierView(d.dossierId(),d.state().name(),d.version(),new ProfileBody(p.accountType(),p.firstName(),p.lastName(),p.phone(),p.address(),p.organizationName()));
    }
    private static ResponseEntity<List<HistoryView>> history(List<ClientAdmissions.History> events) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(events.stream().map(h -> new HistoryView(h.revision(),h.action(),h.reason(),h.occurredAt())).toList());
    }
    private static InvitationView invitation(ClientAdmissions.Invitation i) { return new InvitationView(i.invitationId(),i.email(),i.expiresAt(),i.version(),i.accepted()); }
    private static String tag(long version) { return "\""+version+"\""; }
    private static long version(String value) {
        if (value==null || !value.matches("\"[0-9]{1,18}\"")) { throw new ClientFailure(ClientFailure.Code.INVALID_CLIENT_INPUT); }
        return Long.parseLong(value.substring(1,value.length()-1));
    }
    public record EmailBody(@NotNull @Size(max=254) String email) { @Override public String toString() { return "EmailBody[redacted]"; } }
    public record TokenBody(@NotNull @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) { @Override public String toString() { return "TokenBody[redacted]"; } }
    public record RegistrationBody(@NotNull @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,@NotNull @Size(max=254) String email,@NotNull @Size(max=256) String password) {
        @Override public String toString() { return "RegistrationBody[redacted]"; }
    }
    public record ProfileBody(ClientProfile.Type accountType,@Size(max=100) String firstName,@Size(max=100) String lastName,@Size(max=40) String phone,@Size(max=500) String address,@Size(max=200) String organizationName) {
        ClientProfile profile() { return new ClientProfile(accountType,firstName,lastName,phone,address,organizationName); }
        @Override public String toString() { return "ProfileBody[redacted]"; }
    }
    public record DecisionBody(@NotNull @Size(min=1,max=1000) String reason) { @Override public String toString() { return "DecisionBody[redacted]"; } }
    public record InvitationView(UUID invitationId,String email,Instant expiresAt,long version,boolean accepted) { @Override public String toString() { return "InvitationView[redacted]"; } }
    public record DossierView(UUID dossierId,String state,long version,ProfileBody profile) { }
    public record SummaryView(UUID dossierId,String state,long version,Instant createdAt) { }
    public record AdminView(UUID userId,DossierView dossier) { }
    public record HistoryView(long revision,String action,String reason,Instant occurredAt) { @Override public String toString() { return "HistoryView[redacted]"; } }
    public record AdminHistoryView(long revision,UUID actorUserId,String action,String reason,Instant occurredAt) { @Override public String toString() { return "AdminHistoryView[redacted]"; } }
}
