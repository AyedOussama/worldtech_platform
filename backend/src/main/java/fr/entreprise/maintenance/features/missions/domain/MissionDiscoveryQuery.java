package fr.entreprise.maintenance.features.missions.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Bounded, stable geographic discovery request. Coordinates are public latitude/longitude, not the private site point. */
public record MissionDiscoveryQuery(BigDecimal latitude, BigDecimal longitude, int radiusKm, List<String> skills,
        Instant from, Instant to, Long afterDistanceMeters, UUID afterMissionId, int size) {
    private static final BigDecimal MIN_LATITUDE=new BigDecimal("-90"), MAX_LATITUDE=new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE=new BigDecimal("-180"), MAX_LONGITUDE=new BigDecimal("180");
    public MissionDiscoveryQuery {
        latitude=MissionRules.coordinate(latitude,MIN_LATITUDE,MAX_LATITUDE);
        longitude=MissionRules.coordinate(longitude,MIN_LONGITUDE,MAX_LONGITUDE);
        if(radiusKm<10 || radiusKm>200 || size<1 || size>100 || (afterDistanceMeters==null)!=(afterMissionId==null)
                || (afterDistanceMeters!=null && afterDistanceMeters<0)) { throw MissionFailure.invalid(); }
        skills=MissionRules.optionalSkills(skills);
        if(from!=null && to!=null && !to.isAfter(from)) { throw MissionFailure.invalid(); }
    }
    public long radiusMeters() { return radiusKm*1000L; }
}
