package fr.entreprise.maintenance.features.missions.api;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.missions.application.MissionService;
import fr.entreprise.maintenance.features.missions.domain.Mission;
import fr.entreprise.maintenance.features.missions.domain.MissionFailure;
import fr.entreprise.maintenance.features.missions.domain.MissionFinancialRevision;
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
public class MissionController {
    public static final String ADMIN="/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/missions";
    private final MissionService missions;
    public MissionController(MissionService missions) { this.missions=missions; }
    @PostMapping(ADMIN)
    public ResponseEntity<CommandView> create(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateBody body) {
        var r=missions.create(actor,accountId,siteId,body.ticketId(),body.contractRevisionId(),body.title(),body.publicSummary(),body.skills(),
                body.windowStart(),body.windowEnd(),body.publicLatitude(),body.publicLongitude(),body.publicLabel(),key);
        return ResponseEntity.created(URI.create("/api/web/v1/admin/client-accounts/"+accountId+"/sites/"+siteId+"/missions/"+r.resourceId()))
                .cacheControl(CacheControl.noStore()).eTag(tag(r.version())).body(command(r));
    }
    @GetMapping(ADMIN)
    public ResponseEntity<PageView> list(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,
            @RequestParam(required=false) Mission.State state,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        var p=missions.list(actor,accountId,siteId,state,page,size); return noStore(new PageView(p.items().stream().map(MissionController::summary).toList(),p.total(),p.page(),p.size()));
    }
    @GetMapping(ADMIN+"/{missionId}")
    public ResponseEntity<DetailView> detail(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,
            @PathVariable UUID missionId) {
        var d=missions.detail(actor,accountId,siteId,missionId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(d.mission().version())).body(new DetailView(summary(d.mission()),financial(d.financial())));
    }
    @PostMapping(ADMIN+"/{missionId}/publish")
    public ResponseEntity<CommandView> publish(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,
            @PathVariable UUID missionId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody FinancialBody body) {
        return response(missions.publish(actor,accountId,siteId,missionId,version(expected),body.remunerationText(),body.expensesText(),body.reason(),key));
    }
    @PostMapping(ADMIN+"/{missionId}/financial-revisions")
    public ResponseEntity<CommandView> revise(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,
            @PathVariable UUID missionId,@RequestHeader("If-Match") String expected,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody FinancialBody body) {
        return response(missions.revise(actor,accountId,siteId,missionId,version(expected),body.remunerationText(),body.expensesText(),body.reason(),key));
    }
    @GetMapping(ADMIN+"/{missionId}/financial-revisions")
    public ResponseEntity<FinancialHistory> financialRevisions(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,
            @PathVariable UUID missionId,@RequestParam(defaultValue="-1") long after,@RequestParam(defaultValue="20") int size) {
        var items=missions.financialRevisions(actor,accountId,siteId,missionId,after,size);
        return noStore(new FinancialHistory(items.stream().map(MissionController::financial).toList(),items.isEmpty()?after:items.getLast().number()));
    }
    private static ResponseEntity<CommandView> response(CommandReceipts.ResourceResult r) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(r.version())).body(command(r));
    }
    private static <T> ResponseEntity<T> noStore(T value) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value); }
    private static CommandView command(CommandReceipts.ResourceResult r) { return new CommandView(r.resourceId(),r.version(),r.state()); }
    private static String tag(long v) { return "\""+v+"\""; }
    private static long version(String input) { if(input==null || !input.matches("\"[0-9]{1,18}\"")) { throw MissionFailure.invalid(); } return Long.parseLong(input.substring(1,input.length()-1)); }
    private static SummaryView summary(Mission m) {
        return new SummaryView(m.missionId(),m.siteId(),m.ticketId(),m.contractRevisionId(),m.title(),m.publicSummary(),m.skills(),
                m.windowStart(),m.windowEnd(),m.publicLatitude().toPlainString(),m.publicLongitude().toPlainString(),m.publicLabel(),
                m.state(),m.financialRevision(),m.version(),m.createdAt(),m.publishedAt());
    }
    private static FinancialView financial(MissionFinancialRevision f) {
        return f==null?null:new FinancialView(f.number(),f.remuneration().toPlainString(),f.expenses()==null?null:f.expenses().toPlainString(),
                "EUR","HT","FIXED","FIXED",f.reason(),f.authorUserId(),f.createdAt());
    }
    public record CreateBody(UUID ticketId,UUID contractRevisionId,@NotNull @Size(max=160) String title,@NotNull @Size(max=2000) String publicSummary,
            @NotNull @Size(min=1,max=10) List<@NotNull @Size(max=80) String> skills,@NotNull Instant windowStart,@NotNull Instant windowEnd,
            @NotNull BigDecimal publicLatitude,@NotNull BigDecimal publicLongitude,@Size(max=200) String publicLabel) {
        @Override public String toString() { return "CreateBody[redacted]"; }
    }
    public record FinancialBody(@NotNull Object remuneration,Object expenses,@Size(max=2000) String reason) {
        String remunerationText() { return text(remuneration); }
        String expensesText() { return text(expenses); }
        private static String text(Object value) { if(value==null) { return null; } if(value instanceof String text) { return text; } throw MissionFailure.invalid(); }
        @Override public String toString() { return "FinancialBody[redacted]"; }
    }
    public record CommandView(UUID resourceId,long version,String state) { }
    public record SummaryView(UUID missionId,UUID siteId,UUID ticketId,UUID contractRevisionId,String title,String publicSummary,List<String> skills,
            Instant windowStart,Instant windowEnd,String publicLatitude,String publicLongitude,String publicLabel,Mission.State state,
            long financialRevision,long version,Instant createdAt,Instant publishedAt) { }
    public record FinancialView(long number,String remuneration,String expenses,String currency,String taxBasis,String remunerationBasis,
            String expensesBasis,String reason,UUID authorUserId,Instant createdAt) { }
    public record DetailView(SummaryView mission,FinancialView financial) { }
    public record PageView(List<SummaryView> items,long total,int page,int size) { }
    public record FinancialHistory(List<FinancialView> items,long nextAfter) { }
}
