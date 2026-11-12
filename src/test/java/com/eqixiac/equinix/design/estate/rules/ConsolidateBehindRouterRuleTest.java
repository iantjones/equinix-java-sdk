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

import com.eqixiac.equinix.design.estate.EstateFinding;
import com.eqixiac.equinix.design.estate.EstateInventory;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsolidateBehindRouterRuleTest {

    private final ConsolidateBehindRouterRule rule = new ConsolidateBehindRouterRule();

    private static EstateInventory.Builder threeCloudConnections() {
        return inventory()
                .connection(conn("c1", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .connection(conn("c2", 1000, colo("p1", "DC"), cloud("azure", "Azure", "DC")))
                .connection(conn("c3", 500, colo("p2", "DC"), cloud("gcp", "Google", "DC")))
                .connection(conn("c4", 500, colo("p9", "SV"), cloud("aws", "AWS", "SV")));
    }

    @Test
    void suggestsANewRouterForThreeCloudConnectionsInOneMetroAndPricesTheChange() {
        List<EstateFinding> f = rule.evaluate(threeCloudConnections().build(), UtilizationSnapshot.none(),
                ctx(customCard()));
        assertEquals(1, f.size(), "SV has one connection only");
        EstateFinding x = f.get(0);
        assertEquals(FindingCategory.CONSOLIDATE_BEHIND_ROUTER, x.category());
        assertEquals(3, x.resources().size());
        assertTrue(x.action().startsWith("Order a STANDARD Fabric Cloud Router in DC"), x.action());
        // Today 300+300+200 = 800; proposed 300+300+200 IP_VC + 1500 router = 2300 -> costs 1500 more.
        assertEquals(0, new BigDecimal("-1500.00").compareTo(x.impact().monthlySaving()));
    }

    @Test
    void reHomesOntoAnExistingRouterWithoutPricingANewOne() {
        EstateInventory inv = threeCloudConnections().router(router("r1", "STANDARD", "DC", 5, 300)).build();
        EstateFinding x = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard())).get(0);
        assertTrue(x.action().startsWith("Re-home 3 port-to-cloud connections in DC onto Cloud Router"), x.action());
        assertEquals(0, BigDecimal.ZERO.compareTo(x.impact().monthlySaving()));
    }

    @Test
    void twoConnectionsAreBelowTheDefaultMinimum() {
        EstateInventory inv = inventory()
                .connection(conn("c1", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .connection(conn("c2", 1000, colo("p1", "DC"), cloud("azure", "Azure", "DC"))).build();
        assertTrue(rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard())).isEmpty());
    }

    @Test
    void routerWithOneConnectionSavesTheRouterPrice() {
        EstateInventory inv = inventory().router(router("r1", "STANDARD", "DC", 1, 300))
                .connection(conn("c1", 1000, router("r1", "DC"), cloud("aws", "AWS", "DC"))).build();
        List<EstateFinding> f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard()));
        assertEquals(1, f.size());
        assertEquals("CONSOLIDATE_BEHIND_ROUTER:single-connection-router:r1", f.get(0).id());
        assertEquals(0, new BigDecimal("1500.00").compareTo(f.get(0).impact().monthlySaving()));
        assertEquals(2, f.get(0).resources().size());
    }

    @Test
    void routerWithNoOrTwoConnectionsIsNotThisRulesBusiness() {
        EstateInventory inv = inventory().router(router("r0", "STANDARD", "DC", 0, 300))
                .router(router("r2", "STANDARD", "SV", 2, 300)).build();
        assertTrue(rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard())).isEmpty());
    }
}
