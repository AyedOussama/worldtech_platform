package fr.entreprise.maintenance.features.contracts.api;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.contracts.application.ContractService;
import fr.entreprise.maintenance.features.contracts.domain.ContractDefinition;
import fr.entreprise.maintenance.features.contracts.domain.ContractFailure;
import fr.entreprise.maintenance.features.contracts.domain.ContractRevision;
import fr.entreprise.maintenance.features.contracts.domain.ContractStore;
import fr.entreprise.maintenance.features.contracts.domain.ServiceCalendar;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContractController {
    public static final String ADMIN="/api/web/v1/admin/client-accounts/{accountId}/contracts";
    public static final String CLIENT="/api/web/v1/client/accounts/{accountId}/sites/{siteId}/contract-revisions";
    private final ContractService contracts;
    public ContractController(ContractService contracts) { this.contracts=contracts; }
    @PostMapping(ADMIN)
    public ResponseEntity<RevisionView> create(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@Valid @RequestBody ContractBody body) {
        var r=contracts.create(actor,accountId,body.definition());
        return ResponseEntity.created(URI.create("/api/web/v1/admin/client-accounts/"+accountId+"/contracts/"+r.contractId())).cacheControl(CacheControl.noStore()).eTag(tag(r.version())).body(view(r));
    }
    @GetMapping(ADMIN)
    public ResponseEntity<ContractPage> list(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var p=contracts.list(actor,accountId,page,size); return noStore(new ContractPage(p.items().stream().map(ContractController::summary).toList(),p.total(),p.page(),p.size()));
    }
    @GetMapping(ADMIN+"/{contractId}")
    public ResponseEntity<RevisionView> current(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID contractId) { var r=contracts.current(actor,accountId,contractId); return response(r); }
    @PostMapping(ADMIN+"/{contractId}/revisions")
    public ResponseEntity<RevisionView> revise(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID contractId,@RequestHeader("If-Match") String expected,@Valid @RequestBody ContractBody body) {
        var r=contracts.revise(actor,accountId,contractId,version(expected),body.definition());
        return ResponseEntity.created(URI.create("/api/web/v1/admin/client-accounts/"+accountId+"/contracts/"+contractId+"/revisions/"+r.revisionId())).cacheControl(CacheControl.noStore()).eTag(tag(r.version())).body(view(r));
    }
    @GetMapping(ADMIN+"/{contractId}/revisions")
    public ResponseEntity<RevisionHistory> history(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID contractId,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size) {
        var items=contracts.history(actor,accountId,contractId,after,size); return noStore(new RevisionHistory(items.stream().map(ContractController::summary).toList(),items.isEmpty()?after:items.getLast().version()));
    }
    @GetMapping(ADMIN+"/{contractId}/revisions/{revisionId}")
    public ResponseEntity<RevisionView> revision(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID contractId,@PathVariable UUID revisionId) { return response(contracts.revision(actor,accountId,contractId,revisionId)); }
    @GetMapping(CLIENT)
    public ResponseEntity<SiteContractPage> siteList(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@RequestParam(required=false) Instant at,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var p=contracts.siteList(actor,accountId,siteId,at,page,size); return noStore(new SiteContractPage(p.items().stream().map(s->new SiteSummaryView(summary(new ContractStore.Summary(s.contractId(),s.revisionId(),s.version(),s.publicName(),s.validFrom(),s.validUntil())),s.wholeSite())).toList(),p.total(),p.page(),p.size()));
    }
    @GetMapping(CLIENT+"/{revisionId}")
    public ResponseEntity<SiteRevisionView> siteRevision(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@PathVariable UUID revisionId) {
        var r=contracts.siteRevision(actor,accountId,siteId,revisionId); return noStore(new SiteRevisionView(r.contractId(),r.revisionId(),r.version(),r.publicName(),r.validFrom(),r.validUntil(),r.wholeSite(),r.equipmentIds(),calendar(r.calendar()),r.slaRules().stream().map(ContractController::rule).toList()));
    }
    private static ResponseEntity<RevisionView> response(ContractRevision r) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(r.version())).body(view(r)); }
    private static <T> ResponseEntity<T> noStore(T value) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value); }
    private static String tag(long v) { return "\""+v+"\""; }
    private static long version(String input) { if(!input.matches("\"[0-9]{1,18}\"")) { throw ContractFailure.invalid(); } return Long.parseLong(input.substring(1,input.length()-1)); }
    private static SummaryView summary(ContractStore.Summary s) { return new SummaryView(s.contractId(),s.revisionId(),s.version(),s.publicName(),s.validFrom(),s.validUntil()); }
    private static RevisionView view(ContractRevision r) {
        var d=r.definition(); var b=new ContractBody(d.publicName(),d.internalNotes(),d.validFrom(),d.validUntil(),d.coverage().stream().map(c->new CoverageBody(c.siteId(),c.wholeSite(),c.equipmentIds())).toList(),calendar(d.calendar()),d.slaRules().stream().map(ContractController::rule).toList(),d.reason());
        return new RevisionView(r.contractId(),r.revisionId(),r.version(),b,r.authorUserId(),r.createdAt());
    }
    private static CalendarBody calendar(ServiceCalendar c) { return new CalendarBody(c.mode(),c.weekly().stream().map(w->new WeeklyBody(w.dayOfWeek(),w.startMinute(),w.endMinute())).toList(),c.overrides().stream().map(d->new OverrideBody(d.date(),d.windows().stream().map(w->new WindowBody(w.startMinute(),w.endMinute())).toList())).toList()); }
    private static SlaBody rule(ContractDefinition.SlaRule r) { return new SlaBody(r.priority(),r.metric(),r.targetMinutes(),r.pauseWhileWaitingClient()); }
    public record CoverageBody(@NotNull UUID siteId,@NotNull Boolean wholeSite,@NotNull @Size(max=100) List<@NotNull UUID> equipmentIds) { ContractDefinition.Coverage coverage() { return new ContractDefinition.Coverage(siteId,wholeSite,equipmentIds); } }
    public record WindowBody(@NotNull Integer startMinute,@NotNull Integer endMinute) { ServiceCalendar.Window window() { return new ServiceCalendar.Window(startMinute,endMinute); } }
    public record WeeklyBody(@NotNull Integer dayOfWeek,@NotNull Integer startMinute,@NotNull Integer endMinute) { ServiceCalendar.WeeklyWindow window() { return new ServiceCalendar.WeeklyWindow(dayOfWeek,startMinute,endMinute); } }
    public record OverrideBody(@NotNull LocalDate date,@NotNull @Size(max=4) List<@NotNull @Valid WindowBody> windows) { ServiceCalendar.DayOverride override() { return new ServiceCalendar.DayOverride(date,windows.stream().map(WindowBody::window).toList()); } }
    public record CalendarBody(@NotNull ServiceCalendar.Mode mode,@NotNull @Size(max=28) List<@NotNull @Valid WeeklyBody> weekly,@NotNull @Size(max=100) List<@NotNull @Valid OverrideBody> overrides) { ServiceCalendar calendar() { return new ServiceCalendar(mode,weekly.stream().map(WeeklyBody::window).toList(),overrides.stream().map(OverrideBody::override).toList()); } }
    public record SlaBody(@NotNull ContractDefinition.Priority priority,@NotNull ContractDefinition.Metric metric,@NotNull Integer targetMinutes,@NotNull Boolean pauseWhileWaitingClient) { ContractDefinition.SlaRule rule() { return new ContractDefinition.SlaRule(priority,metric,targetMinutes,pauseWhileWaitingClient); } }
    public record ContractBody(@NotNull @Size(max=160) String publicName,@Size(max=2000) String internalNotes,@NotNull Instant validFrom,@NotNull Instant validUntil,@NotNull @Size(min=1,max=100) List<@NotNull @Valid CoverageBody> coverage,@NotNull @Valid CalendarBody calendar,@NotNull @Size(max=12) List<@NotNull @Valid SlaBody> slaRules,@NotNull @Size(max=1000) String reason) {
        ContractDefinition definition() { return new ContractDefinition(publicName,internalNotes,validFrom,validUntil,coverage.stream().map(CoverageBody::coverage).toList(),calendar.calendar(),slaRules.stream().map(SlaBody::rule).toList(),reason); }
        @Override public String toString() { return "ContractBody[redacted]"; }
    }
    public record RevisionView(UUID contractId,UUID revisionId,long version,ContractBody definition,UUID authorUserId,Instant createdAt) { }
    public record SummaryView(UUID contractId,UUID revisionId,long version,String publicName,Instant validFrom,Instant validUntil) { }
    public record ContractPage(List<SummaryView> items,long total,int page,int size) { }
    public record RevisionHistory(List<SummaryView> items,long nextAfter) { }
    public record SiteSummaryView(SummaryView revision,boolean wholeSite) { }
    public record SiteContractPage(List<SiteSummaryView> items,long total,int page,int size) { }
    public record SiteRevisionView(UUID contractId,UUID revisionId,long version,String publicName,Instant validFrom,Instant validUntil,boolean wholeSite,List<UUID> equipmentIds,CalendarBody calendar,List<SlaBody> slaRules) { }
}
