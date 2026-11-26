package fr.entreprise.maintenance.features.missions.domain;

import java.time.Instant;
import java.util.List;

public interface MissionDiscovery {
    List<DiscoveredMission> nearby(MissionDiscoveryQuery query, Instant now);
}
