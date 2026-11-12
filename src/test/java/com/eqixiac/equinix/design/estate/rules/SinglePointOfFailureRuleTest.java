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
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class SinglePointOfFailureRuleTest {

    private final SinglePointOfFailureRule rule = new SinglePointOfFailureRule();

    private List<EstateFinding> run(EstateInventory inv) {
        return rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard()));
    }

    @Test
    void lonelyCloudConnectionNeedsASecondaryPricedAsAddedCost() {
        List<EstateFinding> f = run(inventory().connection(conn("c1", 1000, colo("p1", "DC"),
                cloud("aws", "AWS", "DC"))).build());
        assertEquals(1, f.size());
        assertEquals("SINGLE_POINT_OF_FAILURE:missing-secondary:c1", f.get(0).id());
        assertEquals(0, new BigDecimal("-300.00").compareTo(f.get(0).impact().monthlySaving()));
        assertTrue(f.get(0).assumptions().stream().anyMatch(a -> a.contains("not visible")));
    }

    @Test
    void lonelyPortToPortConnectionIsNotFlagged() {
        assertTrue(run(inventory().connection(conn("c1", 1000, colo("p1", "DC"), colo("p2", "DC"))).build()).isEmpty());
    }

    @Test
    void redundantPairOnOnePortIsHigh() {
        EstateInventory inv = inventory()
                .connection(withGroup(conn("c1", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "g", "PRIMARY"))
                .connection(withGroup(conn("c2", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "g", "SECONDARY"))
                .build();
        List<EstateFinding> f = run(inv);
        EstateFinding pair = f.stream().filter(x -> x.id().contains("pair-same-attachment")).findFirst().orElseThrow();
        assertEquals(Severity.HIGH, pair.severity());
        assertTrue(f.stream().anyMatch(x -> x.id().contains("one-attachment")), "the cloud is reached via one port");
    }

    @Test
    void redundantPairOnOneSwitchIsMediumAndOnTwoSwitchesIsFine() {
        EstateInventory.Builder b = inventory()
                .connection(withGroup(conn("c1", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "g", "PRIMARY"))
                .connection(withGroup(conn("c2", 1000, colo("p2", "DC"), cloud("aws", "AWS", "DC")), "g", "SECONDARY"));
        EstateInventory sameSwitch = b.port(port("p1", "DC", 10000, 1000, 1, "sw1"))
                .port(port("p2", "DC", 10000, 1000, 1, "sw1")).build();
        List<EstateFinding> f = run(sameSwitch);
        assertEquals(Severity.MEDIUM, f.stream().filter(x -> x.id().contains("pair-same-switch")).findFirst()
                .orElseThrow().severity());
        EstateInventory twoSwitches = inventory()
                .connection(withGroup(conn("c1", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "g", "PRIMARY"))
                .connection(withGroup(conn("c2", 1000, colo("p2", "DC"), cloud("aws", "AWS", "DC")), "g", "SECONDARY"))
                .port(port("p1", "DC", 10000, 1000, 1, "sw1")).port(port("p2", "DC", 10000, 1000, 1, "sw2")).build();
        List<EstateFinding> g = run(twoSwitches);
        assertTrue(g.stream().noneMatch(x -> x.id().contains("pair-same")), g.toString());
        assertTrue(g.stream().anyMatch(x -> x.id().contains("one-metro")), "both legs leave from DC");
    }

    @Test
    void secondMetroRemovesTheMetroFinding() {
        EstateInventory inv = inventory()
                .connection(withGroup(conn("c1", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "g", "PRIMARY"))
                .connection(withGroup(conn("c2", 1000, colo("p2", "NY"), cloud("aws", "AWS", "NY")), "g", "SECONDARY"))
                .build();
        assertTrue(run(inv).isEmpty());
    }

    @Test
    void singleNetworkEdgeDeviceIsFlaggedAndAPairIsNot() {
        List<EstateFinding> f = run(inventory().device(device("d1", null, null))
                .device(device("d2", "PRIMARY", "d3")).device(device("d3", "SECONDARY", "d2")).build());
        assertEquals(1, f.size());
        assertEquals("SINGLE_POINT_OF_FAILURE:single-device:d1", f.get(0).id());
    }
}
