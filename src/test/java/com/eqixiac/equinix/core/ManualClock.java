package com.eqixiac.equinix.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A test {@link Clock} that only moves when a test calls {@link #advance}, so time-dependent
 * behaviour (cooldowns, token expiry, {@code Retry-After} dates) is asserted without wall-clock
 * sleeps. Thread-safe.
 */
public final class ManualClock extends Clock {

    private final AtomicLong epochMillis;

    public ManualClock(Instant start) {
        this.epochMillis = new AtomicLong(start.toEpochMilli());
    }

    public ManualClock() {
        this(Instant.parse("2026-11-11T12:00:00Z"));
    }

    public ManualClock advance(Duration by) {
        epochMillis.addAndGet(by.toMillis());
        return this;
    }

    public ManualClock advanceMillis(long millis) {
        epochMillis.addAndGet(millis);
        return this;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public long millis() {
        return epochMillis.get();
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }
}
