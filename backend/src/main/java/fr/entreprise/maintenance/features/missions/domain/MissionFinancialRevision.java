package fr.entreprise.maintenance.features.missions.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable published amounts. A price change is always a new revision. */
public record MissionFinancialRevision(long number, BigDecimal remuneration, BigDecimal expenses, String reason,
        UUID authorUserId, Instant createdAt) {
    public MissionFinancialRevision {
        if(number<0 || authorUserId==null || createdAt==null) { throw MissionFailure.invalid(); }
        remuneration=MissionRules.money(remuneration);
        expenses=expenses==null?null:MissionRules.money(expenses);
        reason=MissionRules.optionalText(reason,2000);
    }
    public static MissionFinancialRevision create(long number,String remuneration,String expenses,String reason,UUID author,Instant at) {
        return new MissionFinancialRevision(number,MissionRules.money(remuneration,true),MissionRules.money(expenses,false),reason,author,at);
    }
    @Override public String toString() { return "MissionFinancialRevision[redacted]"; }
}
