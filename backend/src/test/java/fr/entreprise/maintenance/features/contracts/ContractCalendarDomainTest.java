package fr.entreprise.maintenance.features.contracts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.contracts.domain.ContractDefinition;
import fr.entreprise.maintenance.features.contracts.domain.ContractFailure;
import fr.entreprise.maintenance.features.contracts.domain.ServiceCalendar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractCalendarDomainTest {
    private static final ServiceCalendar ALWAYS=new ServiceCalendar(ServiceCalendar.Mode.ALWAYS,List.of(),List.of());
    @Test void coverageUsesHalfOpenBoundsAndDoesNotAssumeAnEquipmentForAGeneralIncident() {
        UUID site=UUID.randomUUID(),equipment=UUID.randomUUID(); Instant from=Instant.parse("2026-01-01T00:00:00Z"),until=Instant.parse("2027-01-01T00:00:00Z");
        var whole=definition(from,until,List.of(new ContractDefinition.Coverage(site,true,List.of())));
        assertThat(whole.covers(site,null,from)).isTrue(); assertThat(whole.covers(site,UUID.randomUUID(),until.minusNanos(1))).isTrue();
        assertThat(whole.covers(site,null,until)).isFalse(); assertThat(whole.covers(site,null,from.minusNanos(1))).isFalse();
        var selected=definition(from,until,List.of(new ContractDefinition.Coverage(site,false,List.of(equipment))));
        assertThat(selected.covers(site,equipment,from)).isTrue(); assertThat(selected.covers(site,null,from)).isFalse(); assertThat(selected.covers(site,UUID.randomUUID(),from)).isFalse();
    }
    @Test void explicitEquipmentCoverageCannotBeEmptyOrMixWithWholeSiteCoverage() {
        UUID site=UUID.randomUUID(),equipment=UUID.randomUUID();
        assertThatThrownBy(()->new ContractDefinition.Coverage(site,false,List.of())).isInstanceOf(ContractFailure.class);
        assertThatThrownBy(()->new ContractDefinition.Coverage(site,true,List.of(equipment))).isInstanceOf(ContractFailure.class);
        assertThatThrownBy(()->new ContractDefinition.Coverage(site,false,List.of(equipment,equipment))).isInstanceOf(ContractFailure.class);
    }
    @Test void validityAndObjectivesHaveNoInventedDefaultsOrImplicitWaitingPause() {
        Instant now=Instant.parse("2026-01-01T00:00:00Z"); UUID site=UUID.randomUUID();
        assertThatThrownBy(()->definition(now,now,List.of(new ContractDefinition.Coverage(site,true,List.of())))).isInstanceOf(ContractFailure.class);
        assertThat(definition(now,now.plusSeconds(60),List.of(new ContractDefinition.Coverage(site,true,List.of()))).slaRules()).isEmpty();
        var rule=new ContractDefinition.SlaRule(ContractDefinition.Priority.HIGH,ContractDefinition.Metric.RESOLUTION,240,false);
        assertThat(rule.pauseWhileWaitingClient()).isFalse();
        assertThatThrownBy(()->new ContractDefinition.SlaRule(rule.priority(),rule.metric(),0,true)).isInstanceOf(ContractFailure.class);
    }
    @Test void aDatedClosureOverridesEvenTwentyFourSevenAndHolidaysAreNotInferred() {
        var closed=new ServiceCalendar(ServiceCalendar.Mode.ALWAYS,List.of(),List.of(new ServiceCalendar.DayOverride(LocalDate.of(2026,11,11),List.of())));
        Instant start=Instant.parse("2026-11-10T23:00:00Z"),end=Instant.parse("2026-11-11T23:00:00Z");
        assertThat(closed.serviceSeconds(start,end,"Europe/Paris")).isZero(); assertThat(ALWAYS.serviceSeconds(start,end,"Europe/Paris")).isEqualTo(86400);
    }
    @Test void weeklyWindowsCountOnlyTheIntersectionAndRespectSiteTimeZone() {
        var calendar=new ServiceCalendar(ServiceCalendar.Mode.WEEKLY,List.of(new ServiceCalendar.WeeklyWindow(1,9*60,12*60),new ServiceCalendar.WeeklyWindow(1,13*60,17*60)),List.of());
        Instant from=Instant.parse("2026-11-02T09:30:00Z"),until=Instant.parse("2026-11-02T13:30:00Z");
        assertThat(calendar.serviceSeconds(from,until,"Europe/Paris")).isEqualTo(3*3600);
        assertThat(calendar.serviceSeconds(from,until,"Indian/Reunion")).isEqualTo(3*3600+1800);
        assertThat(calendar.serviceSeconds(Instant.parse("2026-11-01T00:00:00Z"),Instant.parse("2026-11-02T00:00:00Z"),"Europe/Paris")).isZero();
    }
    @Test void twentyFourSevenCountsRealTwentyThreeAndTwentyFiveHourDays() {
        assertThat(ALWAYS.serviceSeconds(Instant.parse("2026-03-28T23:00:00Z"),Instant.parse("2026-03-29T22:00:00Z"),"Europe/Paris")).isEqualTo(23*3600);
        assertThat(ALWAYS.serviceSeconds(Instant.parse("2026-10-24T22:00:00Z"),Instant.parse("2026-10-25T23:00:00Z"),"Europe/Paris")).isEqualTo(25*3600);
    }
    @Test void aRepeatedPartialWindowCountsBothOccurrencesButNotTheGapBetweenThem() {
        var calendar=new ServiceCalendar(ServiceCalendar.Mode.WEEKLY,List.of(new ServiceCalendar.WeeklyWindow(7,135,165)),List.of());
        assertThat(calendar.serviceSeconds(Instant.parse("2026-10-24T22:00:00Z"),Instant.parse("2026-10-25T23:00:00Z"),"Europe/Paris")).isEqualTo(3600);
        assertThat(calendar.serviceSeconds(Instant.parse("2026-03-28T23:00:00Z"),Instant.parse("2026-03-29T22:00:00Z"),"Europe/Paris")).isZero();
    }
    @Test void localExceptionsCanOpenAnOtherwiseClosedDay() {
        var calendar=new ServiceCalendar(ServiceCalendar.Mode.WEEKLY,List.of(new ServiceCalendar.WeeklyWindow(1,540,1020)),List.of(new ServiceCalendar.DayOverride(LocalDate.of(2026,11,1),List.of(new ServiceCalendar.Window(600,660)))));
        assertThat(calendar.serviceSeconds(Instant.parse("2026-11-01T00:00:00Z"),Instant.parse("2026-11-02T00:00:00Z"),"Europe/Paris")).isEqualTo(3600);
    }
    @Test void overlappingDuplicateOrUnboundedCalendarDefinitionsAreRejected() {
        assertThatThrownBy(()->new ServiceCalendar(ServiceCalendar.Mode.WEEKLY,List.of(new ServiceCalendar.WeeklyWindow(1,540,720),new ServiceCalendar.WeeklyWindow(1,700,800)),List.of())).isInstanceOf(ContractFailure.class);
        assertThatThrownBy(()->new ServiceCalendar.Window(1430,10)).isInstanceOf(ContractFailure.class);
        assertThatThrownBy(()->new ServiceCalendar(ServiceCalendar.Mode.WEEKLY,List.of(),List.of())).isInstanceOf(ContractFailure.class);
        var override=new ServiceCalendar.DayOverride(LocalDate.of(2026,11,1),List.of());
        assertThatThrownBy(()->new ServiceCalendar(ServiceCalendar.Mode.ALWAYS,List.of(),List.of(override,override))).isInstanceOf(ContractFailure.class);
        assertThatThrownBy(()->ALWAYS.serviceSeconds(Instant.EPOCH,Instant.EPOCH.plusSeconds(3661L*86400),"Europe/Paris")).isInstanceOf(ContractFailure.class);
    }
    @Test void duplicateObjectivesOrSitesCannotCreateAmbiguousContractDefinitions() {
        UUID site=UUID.randomUUID(); var coverage=new ContractDefinition.Coverage(site,true,List.of());
        var rule=new ContractDefinition.SlaRule(ContractDefinition.Priority.NORMAL,ContractDefinition.Metric.RESOLUTION,60,true);
        Instant from=Instant.parse("2026-01-01T00:00:00Z"),until=Instant.parse("2027-01-01T00:00:00Z");
        assertThatThrownBy(()->new ContractDefinition("Service",null,from,until,List.of(coverage,coverage),ALWAYS,List.of(),"Motif")).isInstanceOf(ContractFailure.class);
        assertThatThrownBy(()->new ContractDefinition("Service",null,from,until,List.of(coverage),ALWAYS,List.of(rule,rule),"Motif")).isInstanceOf(ContractFailure.class);
    }
    private static ContractDefinition definition(Instant from,Instant until,List<ContractDefinition.Coverage> coverage) { return new ContractDefinition("Service",null,from,until,coverage,ALWAYS,List.of(),"Création"); }
}
