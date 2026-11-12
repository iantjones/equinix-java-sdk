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
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class HygieneRuleTest {

    private final HygieneRule rule = new HygieneRule();

    private List<EstateFinding> run(EstateInventory inv) {
        return rule.evaluate(inv, UtilizationSnapshot.none(), ctx(customCard()));
    }

    private static EstateInventory pending(long ageDays, String... ids) {
        EstateInventory.Builder b = inventory();
        for (String id : ids) {
            b.connection(withState(conn(id, 50, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "PENDING",
                    "PENDING_APPROVAL", daysAgo(ageDays)));
        }
        return b.build();
    }

    @Test
    void stuckConnectionsAreOneFindingListingEveryResource() {
        List<EstateFinding> f = run(pending(10, "c1", "c2", "c3"));
        assertEquals(1, f.size());
        EstateFinding x = f.get(0);
        assertEquals(3, x.resources().size());
        assertEquals(Severity.MEDIUM, x.severity());
        assertTrue(x.title().startsWith("3 connection(s) in PENDING/PENDING_APPROVAL"), x.title());
        assertTrue(x.action().contains("approve them with the service provider"));
        assertEquals(FinancialImpact.Kind.NONE, x.impact().kind());
    }

    @Test
    void stuckAgeBoundaryIsInclusiveAndUnknownAgeIsSkipped() {
        assertTrue(run(pending(6, "c1")).isEmpty());
        assertEquals(1, run(pending(7, "c1")).size());
        EstateInventory noDate = inventory().connection(withState(conn("c1", 50, colo("p1", "DC"),
                cloud("aws", "AWS", "DC")), "PENDING", "PENDING_APPROVAL", null)).build();
        assertTrue(run(noDate).isEmpty());
    }

    @Test
    void failedStateIsHigh() {
        EstateInventory inv = inventory().connection(withState(conn("c1", 50, colo("p1", "DC"),
                cloud("aws", "AWS", "DC")), "FAILED", "FAILED", daysAgo(30))).build();
        assertEquals(Severity.HIGH, run(inv).get(0).severity());
    }

    @Test
    void missingNotificationsProjectAndPurchaseOrderAreAggregated() {
        EstateInventory inv = inventory()
                .connection(withTags(conn("c1", 50, colo("p1", "DC"), cloud("aws", "AWS", "DC")), null, 0))
                .connection(withTags(conn("c2", 50, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "", 2))
                .connection(conn("c3", 50, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .build();
        List<EstateFinding> f = run(inv);
        assertEquals(2, f.size(), f.toString());
        EstateFinding notify = f.stream().filter(x -> x.id().startsWith("HYGIENE:no-notifications")).findFirst()
                .orElseThrow();
        assertEquals(1, notify.resources().size());
        EstateFinding po = f.stream().filter(x -> x.id().startsWith("HYGIENE:no-purchase-order")).findFirst()
                .orElseThrow();
        assertEquals(2, po.resources().size());
        assertEquals(Severity.INFO, po.severity());
    }

    @Test
    void cleanEstateHasNoHygieneFindings() {
        assertTrue(run(inventory().connection(conn("c1", 50, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .router(router("r1", "STANDARD", "DC", 1, 100)).build()).isEmpty());
    }
}
