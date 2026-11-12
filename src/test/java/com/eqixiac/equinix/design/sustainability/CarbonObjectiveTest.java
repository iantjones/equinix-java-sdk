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

package com.eqixiac.equinix.design.sustainability;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.ValueScale;
import java.util.List;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The intensity figures here are test values, not real grid data. */
class CarbonObjectiveTest {

    private static final CarbonIntensityTable TABLE = new CarbonIntensityTable(Map.of("dc", 300.0, "NY", 200.0),
            "test fixture (not real grid data)", "test", "location-based");

    private static FakeDesignContext ctx() {
        DesignSpec spec = DesignSpec.builder().name("c")
                .site(SiteIntent.builder().label("HQ").metro("DC").weight(3.0).build())
                .site(SiteIntent.builder().label("Branch").metro("NY").weight(1.0).build())
                .workload(WorkloadIntent.builder().label("big").bandwidthMbps(900).pinnedMetro("DC").build())
                .workload(WorkloadIntent.builder().label("small").bandwidthMbps(100).pinnedMetro("NY").build())
                .build();
        return new FakeDesignContext(spec).metro("DC", Region.AMER).metro("NY", Region.AMER).metro("LD", Region.EMEA)
                .latency("DC", "HQ", 0.5).latency("NY", "Branch", 0.5);
    }

    @Test
    @DisplayName("no table: unknown for every portfolio")
    void inert() {
        ObjectiveValue v = new CarbonObjective(null).evaluate(MetroPortfolio.ofCodes("DC"), ctx());
        assertFalse(v.known());
        assertTrue(v.explanation().contains("no carbon-intensity table"));
    }

    @Test
    @DisplayName("the discovered plug-in measures only when the spec weights carbon")
    void discoveredPluginIsOptIn() {
        FakeDesignContext unweighted = ctx();
        ObjectiveValue off = new SustainabilityPlugin().objectives().get(0)
                .evaluate(MetroPortfolio.ofCodes("DC"), unweighted);
        assertFalse(off.known());
        assertTrue(off.explanation().contains("no weight"), off.explanation());

        DesignSpec weighted = unweighted.spec().toBuilder()
                .objectives(new DesignObjectives(null, Map.of(CarbonObjective.ID, 0.2))).build();
        FakeDesignContext on = new FakeDesignContext(weighted).metroIn("DC", Region.AMER, "US")
                .metroIn("NY", Region.AMER, "US").latency("DC", "HQ", 0.5).latency("NY", "Branch", 0.5);
        ObjectiveValue v = new SustainabilityPlugin().objectives().get(0).evaluate(MetroPortfolio.ofCodes("DC", "NY"), on);
        assertTrue(v.known(), v.explanation());
        assertEquals(CarbonIntensityTable.bundledReference().intensity("DC", "US"), v.value(), 1e-9);
        assertTrue(v.explanation().startsWith("Beta:"), v.explanation());
        assertTrue(v.explanation().contains("Ember"), v.explanation());
    }

    @Test
    @DisplayName("the bundled table states its provenance and covers every country of the live metro catalogue")
    void bundledReference() {
        CarbonIntensityTable table = CarbonIntensityTable.bundledReference();
        assertTrue(table.source().contains("Ember") && table.source().contains("CC BY 4.0")
                && table.source().contains("Beta"), table.source());
        assertTrue(table.method().startsWith("location-based"), table.method());
        // the 32 country codes the live /fabric/v4/metros catalogue returned on 2026-11-12
        for (String country : List.of("AE", "AU", "BG", "BR", "CA", "CH", "CL", "CN", "CO", "DE", "ES", "FI", "FR",
                "GB", "ID", "IE", "IN", "IT", "JP", "KR", "MX", "MY", "NL", "PE", "PH", "PL", "PT", "SE", "SG", "TR",
                "US", "ZA")) {
            Double g = table.intensity("ZZ", country);
            assertTrue(g != null && g > 0 && g < 1000, country + " -> " + g);
        }
        // Hong Kong is listed under CN and Lagos has no country in the catalogue: both have metro entries
        assertTrue(table.intensity("HK") > table.intensity("ZZ", "CN"));
        assertTrue(table.intensity("LG", null) > 0);
        assertEquals(null, table.intensity("ZZ", "XX"));
    }

    @Test
    @DisplayName("a metro figure wins over its country's, and a country figure fills a metro with none")
    void countryFallback() {
        CarbonIntensityTable table = new CarbonIntensityTable(Map.of("DC", 300.0), Map.of("us", 400.0),
                "test fixture (not real grid data)", "test", "location-based");
        assertEquals(300.0, table.intensity("dc", "US"), 1e-9);
        assertEquals(400.0, table.intensity("NY", "US"), 1e-9);
        assertEquals(null, table.intensity("LD", "GB"));
    }

    @Test
    @DisplayName("scored on a fixed scale, so a score does not depend on the other designs")
    void fixedScale() {
        ValueScale scale = new CarbonObjective(TABLE).scale(ctx());
        assertEquals(50.0, scale.score(350, 100, 600, Direction.MINIMIZE), 1e-9);
        assertEquals(50.0, scale.score(350, 340, 360, Direction.MINIMIZE), 1e-9);
        assertEquals(0.0, scale.score(900, 100, 900, Direction.MINIMIZE), 1e-9);
    }

    @Test
    @DisplayName("load-weighted intensity of where the workloads run, with its source in the explanation")
    void weighted() {
        ObjectiveValue v = new CarbonObjective(TABLE).evaluate(MetroPortfolio.ofCodes("DC", "NY"), ctx());
        // (900 x 300 + 100 x 200) / 1000
        assertEquals(290.0, v.value(), 1e-9);
        assertTrue(v.explanation().contains("test fixture"));
        assertEquals("gCO2e/kWh", new CarbonObjective(TABLE).unit());
    }

    @Test
    @DisplayName("a metro missing from the table makes the value unknown, naming it; nothing is filled in")
    void missing() {
        ObjectiveValue v = new CarbonObjective(TABLE).evaluate(MetroPortfolio.ofCodes("LD"), ctx());
        assertFalse(v.known());
        assertTrue(v.explanation().contains("LD"), v.explanation());
    }

    @Test
    @DisplayName("a table needs its provenance and finite, non-negative figures")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new CarbonIntensityTable(Map.of("DC", 1.0), null, "x", "y"));
        assertThrows(IllegalArgumentException.class,
                () -> new CarbonIntensityTable(Map.of("DC", Double.NaN), "s", "x", "y"));
        assertThrows(IllegalArgumentException.class, () -> new CarbonIntensityTable(Map.of("DC", -1.0), "s", "x", "y"));
    }
}
