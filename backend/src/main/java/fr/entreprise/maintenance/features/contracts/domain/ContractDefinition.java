package fr.entreprise.maintenance.features.contracts.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record ContractDefinition(String publicName, String internalNotes, Instant validFrom, Instant validUntil,
        List<Coverage> coverage, ServiceCalendar calendar, List<SlaRule> slaRules, String reason) {
    public enum Priority { LOW, NORMAL, HIGH, CRITICAL }
    public enum Metric { ACKNOWLEDGEMENT, INTERVENTION_START, RESOLUTION }
    public record Coverage(UUID siteId, boolean wholeSite, List<UUID> equipmentIds) {
        public Coverage {
            if(siteId==null || equipmentIds==null || equipmentIds.size()>100 || equipmentIds.stream().anyMatch(id->id==null)
                    || new HashSet<>(equipmentIds).size()!=equipmentIds.size() || (wholeSite && !equipmentIds.isEmpty())
                    || (!wholeSite && equipmentIds.isEmpty())) { throw ContractFailure.invalid(); }
            equipmentIds=equipmentIds.stream().sorted().toList();
        }
    }
    public record SlaRule(Priority priority, Metric metric, int targetMinutes, boolean pauseWhileWaitingClient) {
        public SlaRule { if(priority==null || metric==null || targetMinutes<1 || targetMinutes>525600) { throw ContractFailure.invalid(); } }
    }
    public ContractDefinition {
        publicName=text(publicName,160,true); internalNotes=text(internalNotes,2000,false); reason=text(reason,1000,true);
        if(validFrom!=null) { validFrom=validFrom.truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
        if(validUntil!=null) { validUntil=validUntil.truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
        if(validFrom==null || validUntil==null || !validUntil.isAfter(validFrom)
                || validFrom.isBefore(Instant.parse("2000-01-01T00:00:00Z")) || validUntil.isAfter(Instant.parse("2201-01-01T00:00:00Z"))
                || coverage==null || coverage.isEmpty() || coverage.size()>100 || coverage.stream().anyMatch(c->c==null)
                || calendar==null || slaRules==null || slaRules.size()>12 || slaRules.stream().anyMatch(r->r==null)) { throw ContractFailure.invalid(); }
        if(new HashSet<>(coverage.stream().map(Coverage::siteId).toList()).size()!=coverage.size()
                || new HashSet<>(slaRules.stream().map(r->r.priority()+"/"+r.metric()).toList()).size()!=slaRules.size()) { throw ContractFailure.invalid(); }
        coverage=coverage.stream().sorted(Comparator.comparing(Coverage::siteId)).toList();
        slaRules=slaRules.stream().sorted(Comparator.comparing(SlaRule::priority).thenComparing(SlaRule::metric)).toList();
    }
    public boolean covers(UUID site, UUID equipment, Instant at) {
        return !at.isBefore(validFrom) && at.isBefore(validUntil)
                && coverage.stream().anyMatch(c->c.siteId().equals(site) && (c.wholeSite() || (equipment!=null && c.equipmentIds().contains(equipment))));
    }
    private static String text(String value,int max,boolean required) {
        if(value==null) { if(required) { throw ContractFailure.invalid(); } return null; }
        if(value.isBlank() || value.length()>max || value.chars().anyMatch(Character::isISOControl)) { throw ContractFailure.invalid(); }
        return value.strip();
    }
    @Override public String toString() { return "ContractDefinition[redacted]"; }
}
