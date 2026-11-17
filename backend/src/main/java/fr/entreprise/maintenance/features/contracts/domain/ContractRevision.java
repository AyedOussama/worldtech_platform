package fr.entreprise.maintenance.features.contracts.domain;

import java.time.Instant;
import java.util.UUID;

public record ContractRevision(UUID contractId, UUID revisionId, long version, ContractDefinition definition, UUID authorUserId, Instant createdAt) {
    public void expect(long expected) { if(version!=expected) { throw new ContractFailure(ContractFailure.Code.CONTRACT_VERSION_STALE); } }
    @Override public String toString() { return "ContractRevision[redacted]"; }
}
