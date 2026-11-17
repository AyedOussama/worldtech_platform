package fr.entreprise.maintenance.testinfra;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {
    private final AtomicReference<Instant> instant = new AtomicReference<>(Instant.now());
    public void reset() { instant.set(Instant.now()); }
    public void advance(long seconds) { instant.updateAndGet(value -> value.plusSeconds(seconds)); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
    @Override public Instant instant() { return instant.get(); }
}
