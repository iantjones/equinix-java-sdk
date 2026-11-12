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
import com.eqixiac.equinix.design.estate.EstateInventory.NetworkRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.ServiceTokenRecord;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class IdleResourceRuleTest {

    private final IdleResourceRule rule = new IdleResourceRule();

    private static EstateFinding only(List<EstateFinding> f, String idPrefix) {
        List<EstateFinding> m = f.stream().filter(x -> x.id().startsWith(idPrefix)).toList();
        assertEquals(1, m.size(), "expected one " + idPrefix + " in " + f);
        return m.get(0);
    }

    @Test
    void activePortWithNoConnectionsIsUnpriced() {
        EstateInventory inv = inventory().port(port("p0", "DC", 10000, 0, 0, "sw1"))
                .port(port("p1", "DC", 10000, 1000, 2, "sw1")).build();
        List<EstateFinding> f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard()));
        EstateFinding x = only(f, "IDLE_OR_UNUSED:port:");
        assertEquals("IDLE_OR_UNUSED:port:p0", x.id());
        assertEquals(FinancialImpact.Kind.UNPRICED, x.impact().kind());
        assertTrue(x.impact().reason().contains("no Fabric port prices"));
        assertEquals(1, f.size(), "the port with connections is not idle");
    }

    @Test
    void connectionWithoutTrafficSavesItsPrice() {
        EstateInventory inv = inventory().connection(conn("c1", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .build();
        EstateFinding x = only(rule.evaluate(inv, util(flat("c1", 1000, 0, 0, 168)), ctx(customCard())),
                "IDLE_OR_UNUSED:connection:");
        assertEquals(0, new BigDecimal("300.00").compareTo(x.impact().monthlySaving()));
        assertEquals(Severity.MEDIUM, x.severity());
        // Any traffic at all is not idle.
        assertTrue(rule.evaluate(inv, util(flat("c1", 1000, 0, 0.1, 168)), ctx(customCard())).isEmpty());
        // Unknown utilisation is not idle.
        assertTrue(rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard())).isEmpty());
    }

    @Test
    void idleSecondaryIsInformational() {
        EstateInventory inv = inventory().connection(withGroup(conn("c2", 1000, colo("p2", "DC"),
                cloud("aws", "AWS", "DC")), "g", "SECONDARY")).build();
        EstateFinding x = rule.evaluate(inv, util(flat("c2", 1000, 0, 0, 168)), ctx(customCard())).get(0);
        assertEquals(Severity.INFO, x.severity());
        assertTrue(x.action().contains("deliberate standby"));
    }

    @Test
    void routerWithoutConnectionsSavesItsPrice() {
        EstateInventory inv = inventory().router(router("r1", "STANDARD", "DC", 0, 100))
                .router(router("r2", "LAB", "DC", 0, 100)).build();
        List<EstateFinding> f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard()));
        EstateFinding std = f.stream().filter(x -> x.id().endsWith(":r1")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("1500.00").compareTo(std.impact().monthlySaving()));
        assertEquals(Severity.MEDIUM, std.severity());
        EstateFinding lab = f.stream().filter(x -> x.id().endsWith(":r2")).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(lab.impact().monthlySaving()), "LAB is free, priced at zero");
        assertEquals(Severity.LOW, lab.severity());
    }

    @Test
    void expiredTokensAndDeprovisionedRecordsAreAggregated() {
        EstateInventory inv = inventory()
                .serviceToken(new ServiceTokenRecord("t1", "a", "VC_TOKEN", "EXPIRED", daysAgo(5), daysAgo(40), PROJECT))
                .serviceToken(new ServiceTokenRecord("t2", "b", "VC_TOKEN", "INACTIVE", daysAgo(1), daysAgo(40), PROJECT))
                .serviceToken(new ServiceTokenRecord("t3", "c", "VC_TOKEN", "ACTIVE", daysAgo(-5), daysAgo(1), PROJECT))
                .network(new NetworkRecord("n1", "n", "EVPLAN", "REGIONAL", "DELETED", 0, daysAgo(10), PROJECT, 1))
                .connection(withState(conn("c1", 50, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "DEPROVISIONED",
                        "DEPROVISIONED", daysAgo(30)))
                .build();
        List<EstateFinding> f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard()));
        EstateFinding tokens = only(f, "IDLE_OR_UNUSED:expired-service-tokens");
        assertEquals(2, tokens.resources().size(), "t3 has not expired yet");
        assertEquals(FinancialImpact.Kind.NONE, tokens.impact().kind());
        EstateFinding gone = only(f, "IDLE_OR_UNUSED:deprovisioned-records");
        assertEquals(2, gone.resources().size());
        assertEquals(Severity.INFO, gone.severity());
    }
}
