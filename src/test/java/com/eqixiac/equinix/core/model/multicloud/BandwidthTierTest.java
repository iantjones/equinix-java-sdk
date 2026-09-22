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

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.design.optimizer.model.ServiceProfileOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BandwidthTier}. The tier list {@code [1000, 10000, 100000]} is the
 * {@code supportedBandwidths} example of the Fabric v4 catalog's Beta {@code ProviderEnvironment}
 * schema (fetched 2026-09-21); all values are Mbps.
 */
@DisplayName("core.model.multicloud.BandwidthTier")
class BandwidthTierTest {

    private static final BandwidthTier CATALOG_EXAMPLE = BandwidthTier.of(1000, 10000, 100000);

    @Test
    @DisplayName("coveringTier returns the smallest tier that is not below the request")
    void coveringTier() {
        assertEquals(OptionalInt.of(1000), CATALOG_EXAMPLE.coveringTier(1));
        assertEquals(OptionalInt.of(1000), CATALOG_EXAMPLE.coveringTier(500));
        assertEquals(OptionalInt.of(1000), CATALOG_EXAMPLE.coveringTier(1000));
        assertEquals(OptionalInt.of(10000), CATALOG_EXAMPLE.coveringTier(1001));
        assertEquals(OptionalInt.of(10000), CATALOG_EXAMPLE.coveringTier(3000));
        assertEquals(OptionalInt.of(10000), CATALOG_EXAMPLE.coveringTier(10000));
        assertEquals(OptionalInt.of(100000), CATALOG_EXAMPLE.coveringTier(10001));
        assertEquals(OptionalInt.of(100000), CATALOG_EXAMPLE.coveringTier(100000));
    }

    @Test
    @DisplayName("coveringTier is empty when the request exceeds every tier")
    void exceedsAllTiers() {
        assertEquals(OptionalInt.empty(), CATALOG_EXAMPLE.coveringTier(100001));
        assertEquals(OptionalInt.empty(), CATALOG_EXAMPLE.coveringTier(Integer.MAX_VALUE));
        assertFalse(CATALOG_EXAMPLE.canCover(100001));
        assertTrue(CATALOG_EXAMPLE.canCover(100000));
    }

    @Test
    @DisplayName("coveringTier is empty for a request <= 0")
    void nonPositiveRequest() {
        assertEquals(OptionalInt.empty(), CATALOG_EXAMPLE.coveringTier(0));
        assertEquals(OptionalInt.empty(), CATALOG_EXAMPLE.coveringTier(-1));
        assertEquals(OptionalInt.empty(), CATALOG_EXAMPLE.coveringTier(Integer.MIN_VALUE));
    }

    @Test
    @DisplayName("an empty tier set covers nothing (documented difference from ServiceProfileOption)")
    void emptyCoversNothing() {
        for (BandwidthTier empty : List.of(BandwidthTier.none(), BandwidthTier.of(), BandwidthTier.of((List<Integer>) null),
                BandwidthTier.of(new ArrayList<>()), BandwidthTier.of(0, -5))) {
            assertTrue(empty.isEmpty());
            assertSame(BandwidthTier.none(), empty);
            assertEquals(OptionalInt.empty(), empty.coveringTier(1000));
            assertFalse(empty.canCover(1));
            assertEquals(OptionalInt.empty(), empty.smallestMbps());
            assertEquals(OptionalInt.empty(), empty.largestMbps());
        }
        // The Fabric profile rule for a tierless profile is the opposite: any size is buildable.
        ServiceProfileOption tierless = ServiceProfileOption.builder()
                .supportedBandwidths(List.of()).allowCustomBandwidth(false).build();
        assertEquals(1000, tierless.coveringTier(1000));
    }

    @Test
    @DisplayName("construction sorts ascending and drops nulls, non-positive values and duplicates")
    void normalizesInput() {
        BandwidthTier tiers = BandwidthTier.of(Arrays.asList(10000, null, 0, 1000, -50, 10000, 50));
        assertEquals(List.of(50, 1000, 10000), new ArrayList<>(tiers.tiersMbps()));
        assertEquals(List.of(50, 1000, 10000), tiers.toList());
        assertEquals(OptionalInt.of(50), tiers.smallestMbps());
        assertEquals(OptionalInt.of(10000), tiers.largestMbps());
        assertEquals(BandwidthTier.of(50, 1000, 10000), tiers);
        assertEquals(BandwidthTier.of(50, 1000, 10000).hashCode(), tiers.hashCode());
        assertNotEquals(CATALOG_EXAMPLE, tiers);
        // The specification's schema example is [0, 0]: placeholders, not sizes.
        assertTrue(BandwidthTier.of(0, 0).isEmpty());
    }

    @Test
    @DisplayName("contains is the exact-tier test; no rounding")
    void containsIsExact() {
        assertTrue(CATALOG_EXAMPLE.contains(10000));
        assertFalse(CATALOG_EXAMPLE.contains(3000));
        assertFalse(CATALOG_EXAMPLE.contains(0));
    }

    @Test
    @DisplayName("the tier set is immutable and detached from the source collection")
    void immutable() {
        List<Integer> source = new ArrayList<>(List.of(1000, 10000));
        BandwidthTier tiers = BandwidthTier.of(source);
        source.add(50);
        assertEquals(List.of(1000, 10000), tiers.toList());
        assertThrows(UnsupportedOperationException.class, () -> tiers.tiersMbps().add(50));
        assertThrows(UnsupportedOperationException.class, () -> tiers.toList().add(50));
    }

    @Test
    @DisplayName("toString lists the tiers with their unit")
    void rendersUnit() {
        assertEquals("[1000, 10000, 100000] Mbps", CATALOG_EXAMPLE.toString());
        assertEquals("[] Mbps", BandwidthTier.none().toString());
    }

    @Test
    @DisplayName("JSON form is the ascending array, the shape of supportedConnectionSizeMbps")
    void jsonRoundTrip() throws Exception {
        assertEquals("[1000,10000,100000]", Constants.mapper().writeValueAsString(CATALOG_EXAMPLE));
        assertEquals(CATALOG_EXAMPLE, Constants.mapper().readValue("[100000, 1000, 10000, 0]", BandwidthTier.class));
        assertSame(BandwidthTier.none(), Constants.mapper().readValue("[]", BandwidthTier.class));
    }

    @Test
    @DisplayName("parity: for a discrete tier list the round-up rule equals ServiceProfileOption.coveringTier")
    void parityWithServiceProfileOption() {
        List<List<Integer>> tierLists = List.of(
                List.of(1000, 10000, 100000),
                List.of(50, 200, 500, 1000, 2000, 5000, 10000),
                List.of(10000, 50, 1000),
                List.of(1000));
        for (List<Integer> tierList : tierLists) {
            BandwidthTier tiers = BandwidthTier.of(tierList);
            ServiceProfileOption option = ServiceProfileOption.builder()
                    .supportedBandwidths(tierList)
                    .allowCustomBandwidth(false)
                    .vcBandwidthMax(null)
                    .build();
            List<Integer> requests = new ArrayList<>(List.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE));
            for (int tier : tierList) {
                requests.addAll(List.of(tier - 1, tier, tier + 1));
            }
            for (int requested : requests) {
                assertEquals(option.coveringTier(requested),
                        tiers.coveringTier(requested).orElse(ServiceProfileOption.NO_COVERING_TIER),
                        "tiers=" + tierList + " requested=" + requested);
                assertEquals(option.canCover(requested), tiers.canCover(requested),
                        "tiers=" + tierList + " requested=" + requested);
            }
        }
    }
}
