package fr.entreprise.maintenance.features.missions.application;

import java.time.Clock;
import java.util.List;
import fr.entreprise.maintenance.features.identity.publicapi.ActiveTechnician;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import fr.entreprise.maintenance.features.missions.domain.DiscoveredMission;
import fr.entreprise.maintenance.features.missions.domain.MissionDiscovery;
import fr.entreprise.maintenance.features.missions.domain.MissionDiscoveryQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class MissionDiscoveryService {
    public record Page(List<DiscoveredMission> items,boolean hasMore) { }
    private final MissionDiscovery discovery;
    private final ActiveTechnician technicians;
    private final Clock clock;
    public MissionDiscoveryService(MissionDiscovery discovery,ActiveTechnician technicians,Clock clock) {
        this.discovery=discovery; this.technicians=technicians; this.clock=clock;
    }
    public Page nearby(IdentitySession actor,MissionDiscoveryQuery query) {
        technicians.require(actor);
        var rows=discovery.nearby(query,clock.instant());
        boolean more=rows.size()>query.size();
        var items=more?rows.subList(0,query.size()):rows;
        return new Page(List.copyOf(items),more);
    }
}
