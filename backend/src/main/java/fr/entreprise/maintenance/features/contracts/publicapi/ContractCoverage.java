package fr.entreprise.maintenance.features.contracts.publicapi;

import java.time.Instant;
import java.util.UUID;

/** Internal transactional capability; the ticket use case has already authorized and locked its site. */
public interface ContractCoverage {
    void requireAccountRevision(UUID accountId, UUID revisionId);
    void requireApplicable(UUID accountId, UUID revisionId, UUID siteId, UUID equipmentId, Instant declaredAt);
}
