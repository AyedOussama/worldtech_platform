package fr.entreprise.maintenance.features.technicians.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.identity.publicapi.AdmissionIdentity;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.identity.publicapi.RegistrationConflict;
import fr.entreprise.maintenance.features.technicians.application.TechnicianOnboardingService;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianAdmissions;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianDossier;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianFailure;
import fr.entreprise.maintenance.features.technicians.domain.TechnicianProfile;
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
public class TechnicianOnboardingController {
    private final TechnicianOnboardingService technicians;
    public TechnicianOnboardingController(TechnicianOnboardingService technicians) { this.technicians=technicians; }
    @PostMapping({"/api/web/v1/onboarding/technician/register","/api/mobile/v1/onboarding/technician/register"})
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationBody body,HttpServletRequest request) {
        try { technicians.register(body.email(),body.password(),request.getRemoteAddr()); }
        catch (RegistrationConflict ignored) { /* Same response for a new/existing mailbox; never merge or mutate the existing account. */ }
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping({"/api/web/v1/onboarding/technician/start","/api/mobile/v1/onboarding/technician/start"})
    public ResponseEntity<DossierView> start(@AuthenticationPrincipal IdentitySession actor,HttpServletRequest request) { return view(technicians.open(actor,channel(request))); }
    @GetMapping({"/api/web/v1/onboarding/technician","/api/mobile/v1/onboarding/technician"})
    public ResponseEntity<DossierView> own(@AuthenticationPrincipal IdentitySession actor,HttpServletRequest request) { return view(technicians.own(actor,channel(request))); }
    @PostMapping({"/api/web/v1/onboarding/technician/profile","/api/mobile/v1/onboarding/technician/profile"})
    public ResponseEntity<DossierView> save(@AuthenticationPrincipal IdentitySession actor,@Valid @RequestBody ProfileBody body,@RequestHeader("If-Match") String expected,HttpServletRequest request) {
        return view(technicians.save(actor,channel(request),body.profile(),version(expected)));
    }
    @PostMapping({"/api/web/v1/onboarding/technician/submit","/api/mobile/v1/onboarding/technician/submit"})
    public ResponseEntity<CommandReceipts.Result> submit(@AuthenticationPrincipal IdentitySession actor,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,HttpServletRequest request) {
        var result=technicians.submit(actor,channel(request),version(expected),key);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(result.version())).body(result);
    }
    @GetMapping({"/api/web/v1/onboarding/technician/history","/api/mobile/v1/onboarding/technician/history"})
    public ResponseEntity<List<HistoryView>> ownHistory(@AuthenticationPrincipal IdentitySession actor,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return history(technicians.ownHistory(actor,channel(request),after,size));
    }
    @GetMapping("/api/web/v1/admin/onboardings/technicians")
    public ResponseEntity<List<SummaryView>> review(@AuthenticationPrincipal IdentitySession actor,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="SUBMITTED") TechnicianDossier.State state) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(technicians.review(actor,page,size,state).stream()
                .map(d -> new SummaryView(d.dossierId(),d.state().name(),d.version(),d.createdAt())).toList());
    }
    @GetMapping("/api/web/v1/admin/onboardings/technicians/{id}")
    public ResponseEntity<AdminView> detail(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id) {
        var d=technicians.detail(actor,id); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(d.version())).body(new AdminView(d.userId(),dossier(d)));
    }
    @GetMapping("/api/web/v1/admin/onboardings/technicians/{id}/history")
    public ResponseEntity<List<AdminHistoryView>> history(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(technicians.history(actor,id,after,size).stream()
                .map(h -> new AdminHistoryView(h.revision(),h.actorUserId(),h.action(),h.reason(),h.occurredAt())).toList());
    }
    @PostMapping("/api/web/v1/admin/onboardings/technicians/{id}/approve")
    public ResponseEntity<DossierView> approve(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestHeader("If-Match") String expected,@Valid @RequestBody DecisionBody body) {
        return view(technicians.decide(actor,id,version(expected),TechnicianDossier.Decision.APPROVED,body.reason()));
    }
    @PostMapping("/api/web/v1/admin/onboardings/technicians/{id}/request-information")
    public ResponseEntity<DossierView> requestInformation(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestHeader("If-Match") String expected,@Valid @RequestBody DecisionBody body) {
        return view(technicians.decide(actor,id,version(expected),TechnicianDossier.Decision.NEEDS_INFORMATION,body.reason()));
    }
    @PostMapping("/api/web/v1/admin/onboardings/technicians/{id}/reject")
    public ResponseEntity<DossierView> reject(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID id,@RequestHeader("If-Match") String expected,@Valid @RequestBody DecisionBody body) {
        return view(technicians.decide(actor,id,version(expected),TechnicianDossier.Decision.REJECTED,body.reason()));
    }
    private static ResponseEntity<DossierView> view(TechnicianDossier d) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(d.version())).body(dossier(d)); }
    private static DossierView dossier(TechnicianDossier d) {
        var p=d.profile(); return new DossierView(d.dossierId(),d.state().name(),d.version(),new ProfileBody(p.firstName(),p.lastName(),p.phone(),p.baseCity(),p.radiusKm(),p.declaredSkills()),"REQUIRED_NOT_AVAILABLE");
    }
    private static ResponseEntity<List<HistoryView>> history(List<TechnicianAdmissions.History> events) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(events.stream().map(h -> new HistoryView(h.revision(),h.action(),h.reason(),h.occurredAt())).toList());
    }
    private static AdmissionIdentity.Channel channel(HttpServletRequest request) { return request.getRequestURI().startsWith("/api/mobile/v1/") ? AdmissionIdentity.Channel.MOBILE : AdmissionIdentity.Channel.WEB; }
    private static String tag(long version) { return "\""+version+"\""; }
    private static long version(String value) {
        if (value==null || !value.matches("\"[0-9]{1,18}\"")) { throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT); }
        return Long.parseLong(value.substring(1,value.length()-1));
    }
    public record RegistrationBody(@NotNull @Size(max=254) String email,@NotNull @Size(max=256) String password) { @Override public String toString() { return "RegistrationBody[redacted]"; } }
    public record ProfileBody(@Size(max=100) String firstName,@Size(max=100) String lastName,@Size(max=40) String phone,@Size(max=200) String baseCity,Integer radiusKm,
            @Size(max=20) List<@NotNull @Size(min=1,max=80) String> declaredSkills) {
        TechnicianProfile profile() { return new TechnicianProfile(firstName,lastName,phone,baseCity,radiusKm,declaredSkills); }
        @Override public String toString() { return "ProfileBody[redacted]"; }
    }
    public record DecisionBody(@NotNull @Size(min=1,max=1000) String reason) { @Override public String toString() { return "DecisionBody[redacted]"; } }
    public record DossierView(UUID dossierId,String state,long version,ProfileBody profile,String documentsStatus) { }
    public record SummaryView(UUID dossierId,String state,long version,Instant createdAt) { }
    public record AdminView(UUID userId,DossierView dossier) { }
    public record HistoryView(long revision,String action,String reason,Instant occurredAt) { @Override public String toString() { return "HistoryView[redacted]"; } }
    public record AdminHistoryView(long revision,UUID actorUserId,String action,String reason,Instant occurredAt) { @Override public String toString() { return "AdminHistoryView[redacted]"; } }
}
