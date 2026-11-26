package fr.entreprise.maintenance.features.missions.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Discovery projection: public point and published terms only, never account/site/ticket/contract identifiers or notes. */
public record DiscoveredMission(UUID missionId, String title, String publicSummary, List<String> skills,
        Instant windowStart, Instant windowEnd, BigDecimal publicLatitude, BigDecimal publicLongitude, String publicLabel,
        long distanceMeters, long financialRevision, BigDecimal remuneration, BigDecimal expenses, String currency,
        String taxBasis, String remunerationBasis, String expensesBasis) {
}
