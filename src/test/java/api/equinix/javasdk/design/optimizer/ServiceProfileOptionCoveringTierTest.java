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

package api.equinix.javasdk.design.optimizer;

import api.equinix.javasdk.design.optimizer.model.ServiceProfileOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bandwidth round-up primitive on {@link ServiceProfileOption}: {@link ServiceProfileOption#covers(int)}
 * stays the <em>exact</em>-tier test (the {@code PlanValidator.checkProfile} mirror), while
 * {@link ServiceProfileOption#coveringTier(int)} implements the owner's round-up — the smallest tier that
 * satisfies (is not below) the requirement, clamped to the per-metro ceiling, and
 * {@link ServiceProfileOption#NO_COVERING_TIER} only when even rounding up cannot satisfy it.
 */
@DisplayName("ServiceProfileOption — bandwidth round-up primitive")
class ServiceProfileOptionCoveringTierTest {

    private static ServiceProfileOption tiered(List<Integer> tiers, Integer vcBandwidthMax) {
        return ServiceProfileOption.builder()
                .serviceProfileUuid("sp")
                .sellerRegions(List.of("r"))
                .supportedBandwidths(tiers)
                .allowCustomBandwidth(false)
                .vcBandwidthMax(vcBandwidthMax)
                .build();
    }

    @Nested
    @DisplayName("discrete-tier profile")
    class DiscreteTiers {

        private final ServiceProfileOption option = tiered(List.of(1000, 5000, 10000), null);

        @Test
        @DisplayName("a non-exact requirement rounds UP to the smallest satisfying tier (3000 → 5000)")
        void roundsUpToSmallestSatisfyingTier() {
            assertEquals(5000, option.coveringTier(3000));
            assertEquals(10000, option.coveringTier(6000));
            assertEquals(5000, option.coveringTier(5000), "an exact tier returns itself");
            assertEquals(1000, option.coveringTier(1000));
            assertEquals(1000, option.coveringTier(1), "below the smallest tier rounds up to it");
        }

        @Test
        @DisplayName("a requirement above the largest tier has no covering tier")
        void aboveLargestTierHasNoCoveringTier() {
            assertEquals(ServiceProfileOption.NO_COVERING_TIER, option.coveringTier(11000));
            assertFalse(option.canCover(11000));
            assertTrue(option.canCover(3000), "3000 is coverable via round-up even though not exact");
        }

        @Test
        @DisplayName("covers() stays EXACT — the round-up is coveringTier's job, not covers()'")
        void coversStaysExact() {
            assertFalse(option.covers(3000), "3000 is not an exact tier");
            assertTrue(option.covers(5000), "5000 is an exact tier — and is what a rounded-up connection is stamped at");
            assertTrue(option.covers(1000));
        }

        @Test
        @DisplayName("maxCoverableMbps is the largest tier")
        void maxCoverableIsLargestTier() {
            assertEquals(10000, option.maxCoverableMbps());
        }
    }

    @Nested
    @DisplayName("per-metro ceiling")
    class Ceiling {

        @Test
        @DisplayName("a tier above the per-metro ceiling is not selectable")
        void ceilingClampsCoveringTier() {
            ServiceProfileOption capped = tiered(List.of(1000, 5000, 10000), 5000);
            assertEquals(5000, capped.coveringTier(3000), "5000 is at the ceiling and satisfies 3000");
            assertEquals(ServiceProfileOption.NO_COVERING_TIER, capped.coveringTier(6000),
                    "10000 would satisfy 6000 but is above the 5000 ceiling");
            assertEquals(5000, capped.maxCoverableMbps(), "the ceiling caps the max coverable below the largest tier");
        }
    }

    @Nested
    @DisplayName("custom / tierless profile")
    class CustomBandwidth {

        @Test
        @DisplayName("custom bandwidth carries the exact requested speed (nothing to round)")
        void customCarriesExactSpeed() {
            ServiceProfileOption custom = ServiceProfileOption.builder()
                    .serviceProfileUuid("sp").sellerRegions(List.of("r"))
                    .supportedBandwidths(List.of()).allowCustomBandwidth(true).build();
            assertEquals(3000, custom.coveringTier(3000), "custom bandwidth needs no round-up");
            assertTrue(custom.covers(3000));
            assertEquals(Integer.MAX_VALUE, custom.maxCoverableMbps(), "unbounded custom profile");
        }

        @Test
        @DisplayName("a tierless profile (no discrete tiers) behaves like custom, clamped by the ceiling")
        void tierlessClampedByCeiling() {
            ServiceProfileOption tierless = ServiceProfileOption.builder()
                    .serviceProfileUuid("sp").sellerRegions(List.of("r"))
                    .supportedBandwidths(null).allowCustomBandwidth(false).vcBandwidthMax(4000).build();
            assertEquals(3000, tierless.coveringTier(3000));
            assertEquals(ServiceProfileOption.NO_COVERING_TIER, tierless.coveringTier(5000),
                    "above the ceiling is not coverable");
            assertEquals(4000, tierless.maxCoverableMbps());
        }
    }
}
