package fr.entreprise.maintenance.features.contracts.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContractStore {
    ContractRevision create(UUID account, ContractDefinition definition, UUID actor, Instant now);
    ContractRevision current(UUID account, UUID contract);
    ContractRevision revise(UUID account, ContractRevision current, ContractDefinition definition, UUID actor, Instant now);
    ContractRevision revision(UUID account, UUID contract, UUID revision);
    ContractRevision applicable(UUID account, UUID revision, UUID site, UUID equipment, Instant at);
    record Summary(UUID contractId, UUID revisionId, long version, String publicName, Instant validFrom, Instant validUntil) { }
    List<Summary> list(UUID account, int offset, int size);
    long count(UUID account);
    List<Summary> history(UUID account, UUID contract, long after, int size);
    record SiteSummary(UUID contractId, UUID revisionId, long version, String publicName, Instant validFrom, Instant validUntil, boolean wholeSite) { }
    record SiteRevision(UUID contractId, UUID revisionId, long version, String publicName, Instant validFrom, Instant validUntil,
            boolean wholeSite, List<UUID> equipmentIds, ServiceCalendar calendar, List<ContractDefinition.SlaRule> slaRules) { }
    List<SiteSummary> siteList(UUID account, UUID site, Instant at, int offset, int size);
    long siteCount(UUID account, UUID site, Instant at);
    SiteRevision siteRevision(UUID account, UUID site, UUID revision);
}
