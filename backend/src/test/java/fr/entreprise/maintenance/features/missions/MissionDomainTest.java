package fr.entreprise.maintenance.features.missions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.missions.domain.Mission;
import fr.entreprise.maintenance.features.missions.domain.MissionFailure;
import fr.entreprise.maintenance.features.missions.domain.MissionFinancialRevision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissionDomainTest {
    @Test void aDraftPublishesAndEachPriceChangeCreatesTheNextImmutableRevision() {
        var draft=draft();
        assertThat(draft.state()).isEqualTo(Mission.State.DRAFT);
        assertThat(draft.version()).isZero();
        var published=draft.published(0,Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(published.state()).isEqualTo(Mission.State.PUBLISHED);
        assertThat(published.version()).isEqualTo(1);
        assertThat(published.financialRevision()).isZero();
        var revised=published.revised(1);
        assertThat(revised.version()).isEqualTo(2);
        assertThat(revised.financialRevision()).isEqualTo(1);
        assertThatThrownBy(()->published.revised(2)).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->revised.published(1,Instant.now())).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->draft.expect(1)).isInstanceOf(MissionFailure.class);
    }
    @Test void publicationRequiresAnOpenWindowAndADraft() {
        var draft=draft();
        assertThatThrownBy(()->draft.requirePublishable(draft.windowEnd())).isInstanceOf(MissionFailure.class);
        draft.requirePublishable(draft.windowStart());
        assertThatThrownBy(()->draft.revised(0)).isInstanceOf(MissionFailure.class);
    }
    @ParameterizedTest @ValueSource(strings={"-1.00","-0.00","1.234","10000000000.00","1e2","NaN","1,50"," 2.00 ","01.00","",".50"})
    void invalidMoneyIsRejectedWithoutRounding(String input) {
        assertThatThrownBy(()->MissionFinancialRevision.create(0,input,null,null,UUID.randomUUID(),Instant.EPOCH)).isInstanceOf(MissionFailure.class);
    }
    @Test void moneyIsRequiredForAPublicationAndExpensesRemainOptional() {
        assertThatThrownBy(()->MissionFinancialRevision.create(0,null,null,null,UUID.randomUUID(),Instant.EPOCH)).isInstanceOf(MissionFailure.class);
        var revision=MissionFinancialRevision.create(0,"0","0.00","Publication",UUID.randomUUID(),Instant.EPOCH);
        assertThat(revision.remuneration().toPlainString()).isEqualTo("0.00");
        assertThat(revision.expenses().toPlainString()).isEqualTo("0.00");
        assertThat(MissionFinancialRevision.create(1,"9999999999.99",null,null,UUID.randomUUID(),Instant.EPOCH).expenses()).isNull();
    }
    @Test void skillsAreBoundedDistinctAndCanonical() {
        var mission=draft(List.of("Réseau","windows","WINDOWS","Sécurité"));
        assertThat(mission.skills()).containsExactly("Réseau","Sécurité","windows");
        assertThatThrownBy(()->draft(List.of())).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->draft(java.util.Collections.nCopies(11,"x"))).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->draft(List.of("x".repeat(81)))).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->draft(List.of(" "))).isInstanceOf(MissionFailure.class);
    }
    @Test void coordinatesWindowAndTextAreValidatedAtTheBoundary() {
        assertThatThrownBy(()->mission(new BigDecimal("90.000001"),new BigDecimal("2.3522"))).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->mission(new BigDecimal("48.8566"),new BigDecimal("180.000001"))).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->mission(new BigDecimal("48.1234567"),new BigDecimal("2.3522"))).isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->new Mission(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,null,"Titre","Résumé",
                List.of("Réseau"),Instant.parse("2026-01-01T10:00:00Z"),Instant.parse("2026-01-01T09:00:00Z"),
                new BigDecimal("48.8566"),new BigDecimal("2.3522"),null,Mission.State.DRAFT,-1,0,Instant.EPOCH,null))
                .isInstanceOf(MissionFailure.class);
        assertThatThrownBy(()->mission(new BigDecimal("48.8566"),new BigDecimal("2.3522")," ")).isInstanceOf(MissionFailure.class);
    }
    private static Mission draft() { return draft(List.of("Réseau","Windows")); }
    private static Mission draft(List<String> skills) {
        return Mission.draft(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,null,"Maintenance réseau","Résumé public",skills,
                Instant.parse("2026-01-01T10:00:00Z"),Instant.parse("2026-01-01T12:00:00Z"),
                new BigDecimal("48.8566"),new BigDecimal("2.3522"),"Paris",Instant.parse("2025-12-01T10:00:00Z"));
    }
    private static Mission mission(BigDecimal latitude,BigDecimal longitude) { return mission(latitude,longitude,"Paris"); }
    private static Mission mission(BigDecimal latitude,BigDecimal longitude,String label) {
        return Mission.draft(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,null,"Maintenance réseau","Résumé public",
                List.of("Réseau"),Instant.parse("2026-01-01T10:00:00Z"),Instant.parse("2026-01-01T12:00:00Z"),latitude,longitude,label,
                Instant.parse("2025-12-01T10:00:00Z"));
    }
}
