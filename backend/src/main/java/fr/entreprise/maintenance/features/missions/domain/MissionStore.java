package fr.entreprise.maintenance.features.missions.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionStore {
    Mission create(UUID accountId,UUID siteId,Mission draft,UUID actor,Instant now);
    Mission lock(UUID accountId,UUID siteId,UUID missionId);
    List<Mission> list(UUID accountId,UUID siteId,Mission.State state,int offset,int size);
    long count(UUID accountId,UUID siteId,Mission.State state);
    Mission publish(UUID accountId,Mission current,MissionFinancialRevision revision,Instant now);
    Mission revise(UUID accountId,Mission current,MissionFinancialRevision revision,Instant now);
    List<MissionFinancialRevision> financialRevisions(UUID accountId,UUID siteId,UUID missionId,long after,int size);
    Optional<MissionFinancialRevision> currentFinancial(UUID accountId,UUID siteId,UUID missionId);
}
