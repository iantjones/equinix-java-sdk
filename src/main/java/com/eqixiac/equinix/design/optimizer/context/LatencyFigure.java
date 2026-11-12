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

package com.eqixiac.equinix.design.optimizer.context;

import java.util.Locale;
import java.util.OptionalDouble;

/**
 * One round-trip latency with where it came from. Unknown is a first-class value: an
 * {@link Basis#UNKNOWN} figure has no number at all.
 *
 * @param roundTripMs the round trip in ms, or {@code null} when unknown
 * @param basis       how the figure was obtained
 * @param note        detail for the reader (which pair was measured, the factor used); never {@code null}
 */
public record LatencyFigure(Double roundTripMs, Basis basis, String note) {

    /** How a latency figure was obtained. */
    public enum Basis {
        /** Fabric's measured {@code connectedMetros.avgLatency} for the metro pair (round trip). */
        MEASURED,
        /** Estimated from great-circle distance with the calibrated route factor. */
        ESTIMATED,
        /** The fixed intra-metro figure ({@link CatalogDesignContext#INTRA_METRO_RTT_MS}): an assumption. */
        INTRA_METRO,
        /** Not known: no measurement and no coordinates to estimate from. */
        UNKNOWN
    }

    /** Rejects a number on an unknown figure and a missing number on a known one. */
    public LatencyFigure {
        if (basis == null) {
            throw new IllegalArgumentException("A latency figure needs a basis");
        }
        if ((basis == Basis.UNKNOWN) != (roundTripMs == null)) {
            throw new IllegalArgumentException("An unknown latency has no number and a known one must have one");
        }
        if (roundTripMs != null && !(Double.isFinite(roundTripMs) && roundTripMs >= 0)) {
            throw new IllegalArgumentException("A latency must be a finite, non-negative number, got " + roundTripMs);
        }
        note = note == null ? "" : note;
    }

    /**
     * @param reason why the latency is unknown
     * @return an unknown figure
     */
    public static LatencyFigure unknown(String reason) {
        return new LatencyFigure(null, Basis.UNKNOWN, reason);
    }

    /** @return the figure, or empty when unknown */
    public OptionalDouble value() {
        return roundTripMs == null ? OptionalDouble.empty() : OptionalDouble.of(roundTripMs);
    }

    /** @return {@code true} for a Fabric measurement */
    public boolean isMeasured() {
        return basis == Basis.MEASURED;
    }

    /** @return the figure as {@code 12.3 ms (measured)}, or {@code unknown} */
    public String render() {
        if (roundTripMs == null) {
            return "unknown";
        }
        return String.format(Locale.ROOT, "%.1f ms (%s)", roundTripMs, basis.name().toLowerCase(Locale.ROOT)
                .replace('_', '-'));
    }
}
