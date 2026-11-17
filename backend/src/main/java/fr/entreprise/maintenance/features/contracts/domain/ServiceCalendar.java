package fr.entreprise.maintenance.features.contracts.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Local service windows, not an assumed national holiday or office-hours policy. */
public record ServiceCalendar(Mode mode, List<WeeklyWindow> weekly, List<DayOverride> overrides) {
    public enum Mode { ALWAYS, WEEKLY }
    public record Window(int startMinute, int endMinute) {
        public Window { if(startMinute<0 || startMinute>=endMinute || endMinute>1440) { throw ContractFailure.invalid(); } }
    }
    public record WeeklyWindow(int dayOfWeek, int startMinute, int endMinute) {
        public WeeklyWindow { if(dayOfWeek<1 || dayOfWeek>7) { throw ContractFailure.invalid(); } new Window(startMinute,endMinute); }
    }
    public record DayOverride(LocalDate date, List<Window> windows) {
        public DayOverride {
            if(date==null || date.getYear()<2000 || date.getYear()>2200) { throw ContractFailure.invalid(); }
            windows=checked(windows);
        }
    }
    public ServiceCalendar {
        if(mode==null || weekly==null || overrides==null || weekly.size()>28 || overrides.size()>100
                || weekly.stream().anyMatch(w->w==null) || overrides.stream().anyMatch(w->w==null)
                || (mode==Mode.ALWAYS && !weekly.isEmpty()) || (mode==Mode.WEEKLY && weekly.isEmpty())) { throw ContractFailure.invalid(); }
        for(int day=1;day<=7;day++) {
            final int current=day;
            checked(weekly.stream().filter(w->w.dayOfWeek()==current).map(w->new Window(w.startMinute(),w.endMinute())).toList());
        }
        if(new HashSet<>(overrides.stream().map(DayOverride::date).toList()).size()!=overrides.size()) { throw ContractFailure.invalid(); }
        weekly=weekly.stream().sorted(Comparator.comparingInt(WeeklyWindow::dayOfWeek).thenComparingInt(WeeklyWindow::startMinute)).toList();
        overrides=overrides.stream().sorted(Comparator.comparing(DayOverride::date)).toList();
    }
    private static List<Window> checked(List<Window> windows) {
        if(windows==null || windows.size()>4 || windows.stream().anyMatch(w->w==null)) { throw ContractFailure.invalid(); }
        var ordered=windows.stream().sorted(Comparator.comparingInt(Window::startMinute)).toList();
        for(int i=1;i<ordered.size();i++) { if(ordered.get(i-1).endMinute()>ordered.get(i).startMinute()) { throw ContractFailure.invalid(); } }
        return ordered;
    }
    public List<Window> windows(LocalDate date) {
        var override=overrides.stream().filter(d->d.date().equals(date)).findFirst();
        if(override.isPresent()) { return override.get().windows(); }
        if(mode==Mode.ALWAYS) { return List.of(new Window(0,1440)); }
        return weekly.stream().filter(w->w.dayOfWeek()==date.getDayOfWeek().getValue()).map(w->new Window(w.startMinute(),w.endMinute())).toList();
    }
    /** Real elapsed service seconds in [from,until), including both occurrences during a DST overlap. */
    public long serviceSeconds(Instant from, Instant until, String timeZone) {
        if(from==null || until==null || until.isBefore(from) || Duration.between(from,until).compareTo(Duration.ofDays(3660))>0
                || timeZone==null || !ZoneId.getAvailableZoneIds().contains(timeZone)) { throw ContractFailure.invalid(); }
        ZoneId zone=ZoneId.of(timeZone); Duration total=Duration.ZERO;
        for(LocalDate date=from.atZone(zone).toLocalDate(); !date.isAfter(until.atZone(zone).toLocalDate()); date=date.plusDays(1)) {
            Instant dayStart=date.atStartOfDay(zone).toInstant(), dayEnd=date.plusDays(1).atStartOfDay(zone).toInstant();
            // Split at offset transitions. Converting only the two local endpoints would count
            // non-service minutes between repeated wall-clock intervals in the autumn overlap.
            for(Instant segment=dayStart;segment.isBefore(dayEnd);) {
                var next=zone.getRules().nextTransition(segment);
                Instant end=next!=null && next.getInstant().isBefore(dayEnd) ? next.getInstant() : dayEnd;
                var offset=zone.getRules().getOffset(segment);
                for(Window window:windows(date)) {
                    Instant start=date.atStartOfDay().plusMinutes(window.startMinute()).toInstant(offset);
                    Instant finish=date.atStartOfDay().plusMinutes(window.endMinute()).toInstant(offset);
                    start=max(max(start,segment),from); finish=min(min(finish,end),until);
                    if(start.isBefore(finish)) { total=total.plus(Duration.between(start,finish)); }
                }
                segment=end;
            }
        }
        return total.getSeconds();
    }
    private static Instant min(Instant a, Instant b) { return a.isBefore(b)?a:b; }
    private static Instant max(Instant a, Instant b) { return a.isAfter(b)?a:b; }
}
