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

package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.DesignSpecIO;
import com.eqixiac.equinix.design.spec.SpecIssue;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** {@code DesignConstraints.minSurvivalPct}: the spec field behind the min_survival constraint. */
class MinSurvivalFieldTest {

    private static DesignSpec withField(Double pct, String label) {
        DesignSpec.DesignSpecBuilder b = ResilienceFixtures.specBuilder()
                .constraints(DesignConstraints.builder().minSurvivalPct(pct).build());
        if (label != null) {
            b.label(MinSurvivalConstraint.LABEL, label);
        }
        return b.build();
    }

    @Test
    @DisplayName("the spec field: validated 0..100, written only when set, so other specs keep their fingerprint")
    void specField() {
        DesignSpec unset = ResilienceFixtures.spec();
        assertFalse(DesignSpecIO.toJson(unset).contains("minSurvivalPct"));
        assertEquals(unset.fingerprint(), withField(null, null).fingerprint(),
                "an unset field leaves the canonical JSON, and so the fingerprint, unchanged");

        DesignSpec set = withField(75.0, null);
        assertNotEquals(unset.fingerprint(), set.fingerprint());
        assertTrue(DesignSpecIO.toJson(set).contains("\"minSurvivalPct\" : 75.0"), DesignSpecIO.toJson(set));
        assertEquals(set, DesignSpecIO.fromYaml(DesignSpecIO.toYaml(set)));
        assertEquals(set, DesignSpecIO.fromJson(DesignSpecIO.toJson(set)));
        assertTrue(set.validate().stream().noneMatch(i -> i.severity() == SpecIssue.Severity.ERROR));

        for (double bad : new double[]{-1, 100.5, Double.NaN}) {
            assertTrue(withField(bad, null).validate().stream()
                            .anyMatch(i -> i.path().equals("constraints.minSurvivalPct")), "rejects " + bad);
        }
    }

    @Test
    @DisplayName("min_survival reads the field, and the field wins over the older label")
    void fieldWinsOverLabel() {
        MinSurvivalConstraint c = new MinSurvivalConstraint(null,
                ResilienceOptions.defaults().withStandbyCapacityFraction(0.4));
        MetroPortfolio dcNy = MetroPortfolio.ofCodes("DC", "NY");

        ConstraintVerdict field = c.check(dcNy, ResilienceFixtures.context(withField(75.0, null)));
        assertFalse(field.satisfied(), "40% survives, below 75%");
        assertTrue(field.reason().contains("below the 75% minimum"), field.reason());

        ConstraintVerdict labelOnly = c.check(dcNy, ResilienceFixtures.context(withField(null, "30")));
        assertTrue(labelOnly.satisfied(), "the label still works on its own: " + labelOnly.reason());

        ConstraintVerdict both = c.check(dcNy, ResilienceFixtures.context(withField(75.0, "30")));
        assertFalse(both.satisfied(), "the field (75) wins over the label (30)");

        ConstraintVerdict malformedLabel = c.check(dcNy, ResilienceFixtures.context(withField(30.0, "most")));
        assertTrue(malformedLabel.satisfied(), "a set field means the label is not read");

        assertEquals(new MinSurvivalConstraint(75.0, null).check(MetroPortfolio.ofCodes("DC"),
                        ResilienceFixtures.context(withField(10.0, null))).satisfied(), false,
                "an explicit constructor threshold still takes precedence over the spec");
    }

    @Test
    @DisplayName("the optimiser honours the field without the resilience plug-in")
    void optimizerHonoursField() {
        DesignSpec spec = ResilienceFixtures.specBuilder().constraints(
                DesignConstraints.builder().maxMetros(2).minSurvivalPct(60.0).build()).build();
        CatalogDesignContext ctx = CatalogDesignContext.builder(spec)
                .metro("DC", Region.AMER, 38.9, -77.4, "US").metro("NY", Region.AMER, 40.7, -74.0, "US")
                .metro("LD", Region.EMEA, 51.5, -0.1, "UK")
                .measuredLatency("DC", "NY", 6.3).measuredLatency("DC", "LD", 73.4)
                .reachable("DC", "aws", "azure").reachable("NY", "aws", "azure").reachable("LD", "aws")
                .build();
        OptimizationResult result = MetroOptimizer.forContext(ctx).run();
        assertTrue(result.getConstraintVerdicts().stream()
                .anyMatch(v -> v.constraintId().equals(MinSurvivalConstraint.ID) && v.satisfied()
                        && v.reason().contains("minimum 60%")), result.getConstraintVerdicts().toString());
        assertTrue(result.getFrontier().stream().allMatch(o -> o.portfolio().size() == 2),
                "no single metro survives its own loss");

        DesignSpec without = ResilienceFixtures.specBuilder().constraints(
                DesignConstraints.builder().maxMetros(2).build()).build();
        OptimizationResult free = MetroOptimizer.forContext(CatalogDesignContext.builder(without)
                .metro("DC", Region.AMER, 38.9, -77.4, "US").metro("NY", Region.AMER, 40.7, -74.0, "US")
                .reachable("DC", "aws", "azure").reachable("NY", "aws", "azure").build()).run();
        assertTrue(free.getConstraintVerdicts().stream().noneMatch(v -> v.constraintId().equals(MinSurvivalConstraint.ID)),
                "not added when the spec does not ask for it");
    }
}
