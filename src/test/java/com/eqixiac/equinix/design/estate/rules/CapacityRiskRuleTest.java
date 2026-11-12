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

package com.eqixiac.equinix.design.estate.rules;

import com.eqixiac.equinix.design.estate.Confidence;
import com.eqixiac.equinix.design.estate.EstateFinding;
import com.eqixiac.equinix.design.estate.EstateInventory;
import com.eqixiac.equinix.design.estate.EstateThresholds;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class CapacityRiskRuleTest {

    private final CapacityRiskRule rule = new CapacityRiskRule();

    private List<EstateFinding> ports(Integer used) {
        return rule.evaluate(inventory().port(port("p1", "DC", 10000, used, 3, "sw1")).build(),
                UtilizationSnapshot.none(), ctx(customCard()));
    }

    @Test
    void portThresholdsAreInclusive() {
        assertTrue(ports(7999).isEmpty());
        assertEquals(Severity.MEDIUM, ports(8000).get(0).severity());
        assertEquals(Severity.MEDIUM, ports(9499).get(0).severity());
        assertEquals(Severity.HIGH, ports(9500).get(0).severity());
        assertEquals(FinancialImpact.Kind.UNPRICED, ports(9500).get(0).impact().kind());
        assertEquals(Confidence.HIGH, ports(9500).get(0).confidence());
    }

    @Test
    void sumsInScopeConnectionsWhenTheApiDoesNotReportAllocation() {
        EstateInventory inv = inventory().port(port("p1", "DC", 1000, null, null, "sw1"))
                .connection(conn("c1", 500, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .connection(conn("c2", 500, colo("p1", "DC"), cloud("az", "Azure", "DC"))).build();
        EstateFinding f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard())).get(0);
        assertEquals(Severity.HIGH, f.severity());
        assertEquals(Confidence.MEDIUM, f.confidence(), "a sum of in-scope connections can only under-count");
    }

    @Test
    void routerNearItsPackageLimitPricesTheUpgrade() {
        EstateInventory inv = inventory().router(router("r1", "STANDARD", "DC", 8, 100))
                .routerPackageLimit("STANDARD", 10).build();
        EstateFinding f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard())).get(0);
        assertEquals(Severity.MEDIUM, f.severity());
        assertTrue(f.action().contains("from STANDARD to ADVANCED"));
        assertEquals(0, new BigDecimal("-1500.00").compareTo(f.impact().monthlySaving()));
        // Below 80 %: nothing.
        EstateInventory ok = inventory().router(router("r1", "STANDARD", "DC", 7, 100))
                .routerPackageLimit("STANDARD", 10).build();
        assertTrue(rule.evaluate(ok, UtilizationSnapshot.none(), ctx(customCard())).isEmpty());
    }

    @Test
    void routerLimitFromThresholdsWhenTheApiHasNone() {
        EstateInventory inv = inventory().router(router("r1", "STANDARD", "DC", 10, 100)).build();
        assertTrue(rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard())).isEmpty(),
                "no limit known: not assessed");
        EstateThresholds t = EstateThresholds.builder().routerPackageConnectionLimits(Map.of("STANDARD", 10)).build();
        EstateFinding f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard(), t)).get(0);
        assertEquals(Severity.HIGH, f.severity(), "at the limit");
        assertTrue(f.evidence().stream().anyMatch(e -> e.value().equals("caller-supplied threshold")));
    }
}
