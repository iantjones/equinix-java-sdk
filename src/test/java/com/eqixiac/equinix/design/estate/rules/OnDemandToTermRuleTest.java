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
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class OnDemandToTermRuleTest {

    private final OnDemandToTermRule rule = new OnDemandToTermRule();

    private static EstateInventory connectionAged(int days, Integer term) {
        return inventory().connection(withTerm(conn("c1", 1000, colo("p1", "DC"), cloud("sp1", "AWS", "DC")), term,
                daysAgo(days))).build();
    }

    @Test
    void recommendsTheTermWithTheLargestSavingAndReportsBreakEven() {
        List<EstateFinding> f = rule.evaluate(connectionAged(200, 1), UtilizationSnapshot.none(), ctx(termCard()));
        assertEquals(1, f.size());
        EstateFinding x = f.get(0);
        assertEquals(FindingCategory.ON_DEMAND_TO_TERM, x.category());
        // 300 on demand vs 210 on 36 months = 90/month; setup 270 -> break-even in month 3.
        assertEquals(0, new BigDecimal("90.00").compareTo(x.impact().monthlySaving()));
        assertEquals(0, new BigDecimal("270.00").compareTo(x.impact().oneTimeCost()));
        assertTrue(x.action().contains("36-month term (break-even in month 3)"), x.action());
        assertEquals(Confidence.HIGH, x.confidence());
        assertTrue(x.evidence().stream().anyMatch(e -> e.label().equals("12-month price") && e.value().equals("270")));
    }

    @Test
    void respectsTheMinimumAgeBoundary() {
        assertTrue(rule.evaluate(connectionAged(89, 1), UtilizationSnapshot.none(), ctx(termCard())).isEmpty());
        assertEquals(1, rule.evaluate(connectionAged(90, 1), UtilizationSnapshot.none(), ctx(termCard())).size());
    }

    @Test
    void skipsResourcesAlreadyOnATermOrOfUnknownAge() {
        assertTrue(rule.evaluate(connectionAged(400, 12), UtilizationSnapshot.none(), ctx(termCard())).isEmpty());
        EstateInventory noDate = inventory().connection(withTerm(conn("c1", 1000, colo("p1", "DC"),
                cloud("sp1", "AWS", "DC")), null, null)).build();
        assertTrue(rule.evaluate(noDate, UtilizationSnapshot.none(), ctx(termCard())).isEmpty());
    }

    @Test
    void neverClaimsATermSavingFromACardWithoutATermAxis() {
        // The reference card ignores the term, so every term costs the same: no finding.
        assertTrue(rule.evaluate(connectionAged(400, 1), UtilizationSnapshot.none(),
                ctx(ReferenceRateCard.standard())).isEmpty());
    }

    @Test
    void unpricedOnDemandIsReportedAsUnpriced() {
        EstateFinding f = rule.evaluate(connectionAged(400, 1), UtilizationSnapshot.none(), ctx(null)).get(0);
        assertEquals(FinancialImpact.Kind.UNPRICED, f.impact().kind());
        assertEquals(Confidence.LOW, f.confidence());
    }

    @Test
    void pricesRoutersToo() {
        EstateInventory inv = inventory().router(router("r1", "STANDARD", "DC", 3, 365)).build();
        EstateFinding f = rule.evaluate(inv, UtilizationSnapshot.none(), ctx(termCard())).get(0);
        assertEquals(0, new BigDecimal("100.00").compareTo(f.impact().monthlySaving()));
        assertTrue(f.action().contains("12-month term"));
        assertEquals("ON_DEMAND_TO_TERM:cloudRouter:r1", f.id());
    }
}
