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

package com.eqixiac.equinix.core.model.multicloud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An immutable, ascending set of discrete connection sizes in Mbps, as published by the
 * specification's {@code Environment.supportedConnectionSizeMbps} (and by the Fabric v4 Beta
 * {@code ProviderEnvironment.supportedBandwidths}).
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation).</p>
 *
 * <h2>Construction</h2>
 * <p>{@link #of(Collection)} and {@link #of(int...)} drop {@code null} entries, drop entries
 * {@code <= 0} and collapse duplicates. The specification's schema examples contain {@code 0}
 * placeholders; a size of zero is not a connection size. All units are Mbps.</p>
 *
 * <h2>Round-up rule</h2>
 * <p>{@link #coveringTier(int)} returns the smallest tier that is not below the requested size.
 * For a non-empty discrete tier list this is the same rule as
 * {@code design.optimizer.model.ServiceProfileOption.coveringTier(int)} with no per-metro ceiling
 * and custom bandwidth disallowed; a parity test locks the two implementations together.</p>
 *
 * <table>
 *   <caption>Behavior compared with {@code ServiceProfileOption.coveringTier(int)}</caption>
 *   <tr><th>Case</th><th>{@code BandwidthTier}</th><th>{@code ServiceProfileOption}</th></tr>
 *   <tr><td>requested {@code <= 0}</td><td>{@code OptionalInt.empty()}</td><td>{@code -1}</td></tr>
 *   <tr><td>requested equals a tier</td><td>that tier</td><td>that tier</td></tr>
 *   <tr><td>requested between two tiers</td><td>the higher tier</td><td>the higher tier</td></tr>
 *   <tr><td>requested above the largest tier</td><td>{@code OptionalInt.empty()}</td><td>{@code -1}</td></tr>
 *   <tr><td>empty tier list</td><td>{@code OptionalInt.empty()}</td>
 *       <td>the requested size (a Fabric profile without a tier list accepts any size)</td></tr>
 * </table>
 *
 * <p>The last row is a deliberate difference. The specification has no custom-bandwidth flag:
 * an environment that publishes no sizes offers no size this type can confirm, so nothing is
 * reported as covered.</p>
 *
 * @author ianjones
 */
public final class BandwidthTier {

    private static final BandwidthTier NONE = new BandwidthTier(Collections.emptyNavigableSet());

    private final NavigableSet<Integer> tiersMbps;

    private BandwidthTier(NavigableSet<Integer> tiersMbps) {
        this.tiersMbps = tiersMbps;
    }

    /**
     * @return the empty tier set; {@link #coveringTier(int)} on it is always empty
     */
    public static BandwidthTier none() {
        return NONE;
    }

    /**
     * Creates a tier set from a collection of sizes in Mbps.
     *
     * @param tiersMbps the sizes in Mbps, in any order; {@code null} is treated as empty;
     *                  {@code null} elements, elements {@code <= 0} and duplicates are dropped
     * @return the tier set
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BandwidthTier of(Collection<Integer> tiersMbps) {
        if (tiersMbps == null || tiersMbps.isEmpty()) {
            return NONE;
        }
        TreeSet<Integer> sorted = new TreeSet<>();
        for (Integer tier : tiersMbps) {
            if (tier != null && tier > 0) {
                sorted.add(tier);
            }
        }
        return sorted.isEmpty() ? NONE : new BandwidthTier(Collections.unmodifiableNavigableSet(sorted));
    }

    /**
     * Creates a tier set from sizes in Mbps.
     *
     * @param tiersMbps the sizes in Mbps, in any order; {@code null} is treated as empty; elements
     *                  {@code <= 0} and duplicates are dropped
     * @return the tier set
     */
    public static BandwidthTier of(int... tiersMbps) {
        if (tiersMbps == null || tiersMbps.length == 0) {
            return NONE;
        }
        List<Integer> boxed = new ArrayList<>(tiersMbps.length);
        for (int tier : tiersMbps) {
            boxed.add(tier);
        }
        return of(boxed);
    }

    /**
     * @return the tiers in Mbps, ascending, unmodifiable; empty when no tier is published
     */
    public SortedSet<Integer> tiersMbps() {
        return tiersMbps;
    }

    /**
     * The JSON form: the ascending tier list, matching the shape of the specification's
     * {@code supportedConnectionSizeMbps} array.
     *
     * @return the tiers in Mbps as an ascending, unmodifiable list
     */
    @JsonValue
    public List<Integer> toList() {
        return List.copyOf(tiersMbps);
    }

    /**
     * The smallest published tier that is not below the requested size.
     *
     * @param requestedMbps the requested connection size in Mbps
     * @return the smallest tier {@code >= requestedMbps}; empty when {@code requestedMbps <= 0},
     *         when it exceeds every tier, or when no tier is published
     */
    public OptionalInt coveringTier(int requestedMbps) {
        if (requestedMbps <= 0) {
            return OptionalInt.empty();
        }
        Integer tier = tiersMbps.ceiling(requestedMbps);
        return tier == null ? OptionalInt.empty() : OptionalInt.of(tier);
    }

    /**
     * @param requestedMbps the requested connection size in Mbps
     * @return {@code true} when {@link #coveringTier(int)} is present for {@code requestedMbps}
     */
    public boolean canCover(int requestedMbps) {
        return coveringTier(requestedMbps).isPresent();
    }

    /**
     * Exact-tier test, with no rounding.
     *
     * @param mbps a connection size in Mbps
     * @return {@code true} when {@code mbps} is one of the published tiers
     */
    public boolean contains(int mbps) {
        return tiersMbps.contains(mbps);
    }

    /**
     * @return the smallest published tier in Mbps, or empty when no tier is published
     */
    public OptionalInt smallestMbps() {
        return tiersMbps.isEmpty() ? OptionalInt.empty() : OptionalInt.of(tiersMbps.first());
    }

    /**
     * @return the largest published tier in Mbps, or empty when no tier is published
     */
    public OptionalInt largestMbps() {
        return tiersMbps.isEmpty() ? OptionalInt.empty() : OptionalInt.of(tiersMbps.last());
    }

    /**
     * @return {@code true} when no tier is published
     */
    public boolean isEmpty() {
        return tiersMbps.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BandwidthTier)) {
            return false;
        }
        return tiersMbps.equals(((BandwidthTier) o).tiersMbps);
    }

    @Override
    public int hashCode() {
        return tiersMbps.hashCode();
    }

    /** @return the ascending tier list with its unit, for example {@code [1000, 10000] Mbps} */
    @Override
    public String toString() {
        return tiersMbps + " Mbps";
    }
}
