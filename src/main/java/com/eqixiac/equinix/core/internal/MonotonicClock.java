/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.core.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A {@link Clock} whose {@link #millis()} advances with {@link System#nanoTime()}: it is monotonic,
 * so a wall-clock adjustment (NTP step, DST, a manual change) can neither shorten nor extend an
 * elapsed-time measurement. It is the default time source of the SDK's elapsed-time logic (for
 * example {@link com.eqixiac.equinix.core.http.CircuitBreaker CircuitBreaker} cooldowns); tests
 * substitute a controllable {@code Clock} instead of sleeping.
 *
 * <p>The instant it reports is anchored to the wall clock once, at class initialisation, so it is
 * only meaningful for measuring durations, not as a calendar time.</p>
 *
 * @author ianjones
 */
public final class MonotonicClock extends Clock {

    private static final MonotonicClock INSTANCE = new MonotonicClock();
    private static final long ANCHOR_EPOCH_MILLIS = System.currentTimeMillis();
    private static final long ANCHOR_NANOS = System.nanoTime();

    private MonotonicClock() {
    }

    /**
     * Returns the shared monotonic clock.
     *
     * @return the singleton instance
     */
    public static Clock get() {
        return INSTANCE;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /**
     * Only UTC is supported: the clock exists to measure durations, where a zone is meaningless.
     *
     * @throws UnsupportedOperationException for any zone other than UTC
     */
    @Override
    public Clock withZone(ZoneId zone) {
        if (ZoneOffset.UTC.equals(zone)) {
            return this;
        }
        throw new UnsupportedOperationException("MonotonicClock measures durations and only supports UTC");
    }

    @Override
    public long millis() {
        return ANCHOR_EPOCH_MILLIS + (System.nanoTime() - ANCHOR_NANOS) / 1_000_000L;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }
}
