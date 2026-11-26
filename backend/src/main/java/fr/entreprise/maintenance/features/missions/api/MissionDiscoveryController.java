package fr.entreprise.maintenance.features.missions.api;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.missions.application.MissionDiscoveryService;
import fr.entreprise.maintenance.features.missions.domain.DiscoveredMission;
import fr.entreprise.maintenance.features.missions.domain.MissionDiscoveryQuery;
import fr.entreprise.maintenance.features.missions.domain.MissionFailure;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MissionDiscoveryController {
    private static final String NEARBY="/api/mobile/v1/technician/missions/nearby";
    private final MissionDiscoveryService missions;
    public MissionDiscoveryController(MissionDiscoveryService missions) { this.missions=missions; }
    @GetMapping(NEARBY)
    public ResponseEntity<PageView> nearby(@AuthenticationPrincipal IdentitySession actor,
            @RequestParam BigDecimal latitude,@RequestParam BigDecimal longitude,
            @RequestParam(defaultValue="50") Integer radiusKm,@RequestParam(required=false) List<String> skills,
            @RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
            @RequestParam(required=false) String cursor,@RequestParam(defaultValue="20") Integer size) {
        var after=decode(cursor);
        var query=new MissionDiscoveryQuery(latitude,longitude,radiusKm==null?50:radiusKm,skills,from,to,
                after==null?null:after.distanceMeters(),after==null?null:after.missionId(),size==null?20:size);
        var page=missions.nearby(actor,query);
        String next=page.hasMore()&&!page.items().isEmpty()?encode(page.items().getLast()):null;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new PageView(page.items().stream().map(MissionDiscoveryController::view).toList(),next));
    }
    private static ItemView view(DiscoveredMission m) {
        return new ItemView(m.missionId(),m.title(),m.publicSummary(),m.skills(),m.windowStart(),m.windowEnd(),
                m.publicLatitude().toPlainString(),m.publicLongitude().toPlainString(),m.publicLabel(),m.distanceMeters(),
                new TermsView(m.financialRevision(),m.remuneration().toPlainString(),m.expenses()==null?null:m.expenses().toPlainString(),
                        m.currency(),m.taxBasis(),m.remunerationBasis(),m.expensesBasis()));
    }
    private static String encode(DiscoveredMission mission) {
        String value=mission.distanceMeters()+":"+mission.missionId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static After decode(String cursor) {
        if(cursor==null || cursor.isBlank()) { return null; }
        if(cursor.length()>120) { throw MissionFailure.invalid(); }
        try {
            String value=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8);
            String[] parts=value.split(":",-1);
            if(parts.length!=2) { throw MissionFailure.invalid(); }
            long distance=Long.parseLong(parts[0]);
            UUID mission=UUID.fromString(parts[1]);
            if(distance<0) { throw MissionFailure.invalid(); }
            return new After(distance,mission);
        } catch (IllegalArgumentException failure) { throw MissionFailure.invalid(); }
    }
    private record After(long distanceMeters,UUID missionId) { }
    public record PageView(List<ItemView> items,String nextCursor) { }
    public record ItemView(UUID missionId,String title,String publicSummary,List<String> skills,Instant windowStart,Instant windowEnd,
            String publicLatitude,String publicLongitude,String publicLabel,long distanceMeters,TermsView financial) { }
    public record TermsView(long revision,String remuneration,String expenses,String currency,String taxBasis,
            String remunerationBasis,String expensesBasis) { }
}
