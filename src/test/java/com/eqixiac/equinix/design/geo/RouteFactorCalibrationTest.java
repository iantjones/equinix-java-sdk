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

package com.eqixiac.equinix.design.geo;

import com.eqixiac.equinix.core.enums.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteFactorCalibrationTest {

    private static final SpeedOfLightLatency FLOOR = SpeedOfLightLatency.roundTrip();

    private static RouteFactorCalibration.MeasuredPair pair(Region a, Region b, double km, double factor) {
        return new RouteFactorCalibration.MeasuredPair(a, b, km, FLOOR.millisForKm(km) * factor);
    }

    @Test
    @DisplayName("the factor is the median measured/floor ratio, and the live SV-DC pair alone gives 1.75")
    void medianOfRatios() {
        // Live 2026-11-11: SV-DC 65.8 ms measured over 3,843 km.
        RouteFactorCalibration live = RouteFactorCalibration.fit(List.of(
                new RouteFactorCalibration.MeasuredPair(Region.AMER, Region.AMER, 3843, 65.8)));
        assertEquals(1.75, live.overall().factor(), 0.01);
        assertEquals(1, live.overall().pairs());

        RouteFactorCalibration fit = RouteFactorCalibration.fit(List.of(
                pair(Region.AMER, Region.AMER, 2000, 1.5), pair(Region.AMER, Region.AMER, 3000, 1.6),
                pair(Region.AMER, Region.AMER, 4000, 9.0)));
        assertEquals(1.6, fit.overall().factor(), 1e-9, "one outlier does not move a median");
        assertTrue(fit.isCalibrated());
    }

    @Test
    @DisplayName("short pairs are excluded and a region pair needs five pairs for its own factor")
    void exclusionsAndRegionPairs() {
        List<RouteFactorCalibration.MeasuredPair> pairs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            pairs.add(pair(Region.EMEA, Region.AMER, 6000 + i * 100, 1.3));
        }
        pairs.add(pair(Region.AMER, Region.AMER, 3000, 2.0));
        pairs.add(pair(Region.AMER, Region.AMER, 50, 10.0));
        RouteFactorCalibration fit = RouteFactorCalibration.fit(pairs);
        assertEquals(1, fit.pairsExcluded(), "a 50 km pair says nothing about routing");
        assertEquals(1.3, fit.fitFor(Region.AMER, Region.EMEA).factor(), 1e-9);
        assertEquals(5, fit.byRegionPair().get("AMER/EMEA").pairs());
        assertFalse(fit.byRegionPair().containsKey("AMER/AMER"), "one AMER pair is too few for its own factor");
        assertEquals(fit.overall(), fit.fitFor(Region.AMER, Region.AMER));
        assertTrue(fit.describe().contains("per region pair: AMER/EMEA 1.30 (5 pairs)"), fit.describe());
    }

    @Test
    @DisplayName("with nothing to fit, the default 1.75 is used and the description says so")
    void uncalibrated() {
        RouteFactorCalibration none = RouteFactorCalibration.fit(List.of());
        assertFalse(none.isCalibrated());
        assertEquals(RouteFactorCalibration.DEFAULT_FACTOR, none.overall().factor());
        assertTrue(none.describe().contains("no Fabric-measured metro pair was usable"), none.describe());
        assertEquals(FLOOR.millisForKm(1000) * 1.75, none.estimateRoundTripMs(1000, null, null), 1e-9);
    }

    @Test
    @DisplayName("SpeedOfLightLatency rejects NaN and infinite inputs instead of returning NaN")
    void speedOfLightRejectsNaN() {
        assertThrows(IllegalArgumentException.class, () -> FLOOR.millisForKm(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> FLOOR.millisForKm(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> SpeedOfLightLatency.distanceKm(Double.NaN, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SpeedOfLightLatency.builder().routeFactor(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> SpeedOfLightLatency.builder().refractiveIndex(Double.NaN));
        assertEquals(37.6, FLOOR.millisForKm(3843), 0.1, "the SV-DC floor cited in the design brief");
    }
}
