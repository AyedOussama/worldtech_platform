package fr.entreprise.maintenance.features.missions.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public mission projection. Private site, ticket and contract data never enter this aggregate. */
public record Mission(UUID missionId, UUID accountId, UUID siteId, UUID ticketId, UUID contractRevisionId,
        String title, String publicSummary, List<String> skills, Instant windowStart, Instant windowEnd,
        BigDecimal publicLatitude, BigDecimal publicLongitude, String publicLabel,
        State state, long financialRevision, long version, Instant createdAt, Instant publishedAt) {
    public enum State { DRAFT, PUBLISHED }
    private static final Instant MIN=Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant MAX=Instant.parse("2201-01-01T00:00:00Z");
    private static final BigDecimal MIN_LATITUDE=new BigDecimal("-90"), MAX_LATITUDE=new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE=new BigDecimal("-180"), MAX_LONGITUDE=new BigDecimal("180");
    public Mission {
        if(missionId==null || accountId==null || siteId==null || createdAt==null || version<0 || financialRevision< -1) { throw MissionFailure.invalid(); }
        title=MissionRules.text(title,160);
        publicSummary=MissionRules.text(publicSummary,2000);
        skills=MissionRules.skills(skills);
        publicLabel=MissionRules.optionalText(publicLabel,200);
        if(windowStart==null || windowEnd==null || !windowEnd.isAfter(windowStart) || windowStart.isBefore(MIN) || windowEnd.isAfter(MAX)) { throw MissionFailure.invalid(); }
        publicLatitude=MissionRules.coordinate(publicLatitude,MIN_LATITUDE,MAX_LATITUDE);
        publicLongitude=MissionRules.coordinate(publicLongitude,MIN_LONGITUDE,MAX_LONGITUDE);
        if(state==null) { throw MissionFailure.invalid(); }
        if((state==State.DRAFT && (financialRevision!=-1 || publishedAt!=null))
                || (state==State.PUBLISHED && (financialRevision<0 || publishedAt==null))) { throw MissionFailure.invalid(); }
    }
    public static Mission draft(UUID missionId,UUID accountId,UUID siteId,UUID ticketId,UUID contractRevisionId,
            String title,String publicSummary,List<String> skills,Instant windowStart,Instant windowEnd,
            BigDecimal publicLatitude,BigDecimal publicLongitude,String publicLabel,Instant createdAt) {
        return new Mission(missionId,accountId,siteId,ticketId,contractRevisionId,title,publicSummary,skills,windowStart,windowEnd,
                publicLatitude,publicLongitude,publicLabel,State.DRAFT,-1,0,createdAt,null);
    }
    public void expect(long expected) { if(expected!=version) { throw new MissionFailure(MissionFailure.Code.MISSION_VERSION_STALE); } }
    public void publishable() { if(state!=State.DRAFT) { throw MissionFailure.conflict(); } }
    public void revisable() { if(state!=State.PUBLISHED) { throw MissionFailure.conflict(); } }
    public void requirePublishable(Instant now) {
        publishable();
        if(now==null || !windowEnd.isAfter(now)) { throw MissionFailure.invalid(); }
    }
    public Mission published(long revision,Instant at) {
        publishable();
        if(revision<0 || at==null) { throw MissionFailure.invalid(); }
        return new Mission(missionId,accountId,siteId,ticketId,contractRevisionId,title,publicSummary,skills,windowStart,windowEnd,
                publicLatitude,publicLongitude,publicLabel,State.PUBLISHED,revision,version+1,createdAt,at);
    }
    public Mission revised(long revision) {
        revisable();
        if(revision!=financialRevision+1) { throw MissionFailure.invalid(); }
        return new Mission(missionId,accountId,siteId,ticketId,contractRevisionId,title,publicSummary,skills,windowStart,windowEnd,
                publicLatitude,publicLongitude,publicLabel,State.PUBLISHED,revision,version+1,createdAt,publishedAt);
    }
    @Override public String toString() { return "Mission[redacted]"; }
}
