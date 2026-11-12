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
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class RightSizeRuleTest {

    private final RightSizeRule rule = new RightSizeRule();

    private static EstateInventory oneConnection(int mbps) {
        return inventory().connection(conn("c1", mbps, colo("p1", "DC"), cloud("sp1", "AWS", "DC"))).build();
    }

    @Test
    void sizesDownToSmallestTierWithHeadroomAndPricesTheDifference() {
        // p95 200 Mbps of 1000 (20 %) -> 200 x 1.3 = 260 -> 500 tier; 300 - 200 = USD 100/month.
        List<EstateFinding> f = rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 200, 250, 168)),
                ctx(customCard()));
        assertEquals(1, f.size());
        EstateFinding x = f.get(0);
        assertEquals(FindingCategory.RIGHT_SIZE_DOWN, x.category());
        assertEquals("Reduce connection conn-c1 (c1) from 1000 to 500 Mbps", x.action());
        assertEquals(FinancialImpact.Kind.PRICED, x.impact().kind());
        assertEquals(0, new BigDecimal("100.00").compareTo(x.impact().monthlySaving()));
        assertEquals("USD", x.impact().currency());
        assertEquals(Confidence.HIGH, x.confidence(), "a week of samples and contract prices");
        assertEquals(Severity.MEDIUM, x.severity());
        assertEquals("RIGHT_SIZE_DOWN:connection:c1", x.id());
        assertTrue(x.evidence().stream().anyMatch(e -> e.label().equals("p95 utilisation") && e.value().equals("200.0")));
    }

    @Test
    void doesNotSizeDownAtExactlyTheThreshold() {
        // p95 exactly 30 % of 1000 is not "below 30 %".
        assertTrue(rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 300, 300, 168)), ctx(customCard()))
                .isEmpty());
    }

    @Test
    void doesNotSizeDownWhenTheTargetTierIsNotSmaller() {
        // 50 Mbps connection at 10 %: the smallest tier is 50, so there is nothing to gain.
        assertTrue(rule.evaluate(oneConnection(50), util(flat("c1", 50, 5, 6, 168)), ctx(customCard())).isEmpty());
    }

    @Test
    void sizesUpWhenP95IsAboveTheUpperThreshold() {
        // p95 900 of 1000 (90 %) -> 900 x 1.3 = 1170 -> 2000; adds USD 200/month.
        List<EstateFinding> f = rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 900, 950, 168)),
                ctx(customCard()));
        assertEquals(1, f.size());
        assertEquals(FindingCategory.RIGHT_SIZE_UP, f.get(0).category());
        assertEquals("Increase connection conn-c1 (c1) from 1000 to 2000 Mbps", f.get(0).action());
        assertEquals(0, new BigDecimal("-200.00").compareTo(f.get(0).impact().monthlySaving()));
        assertEquals(Severity.MEDIUM, f.get(0).severity());
    }

    @Test
    void saturatedPeakIsHighSeverityEvenWithModerateP95() {
        List<EstateFinding> f = rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 500, 1000, 168)),
                ctx(customCard()));
        assertEquals(FindingCategory.RIGHT_SIZE_UP, f.get(0).category());
        assertEquals(Severity.HIGH, f.get(0).severity());
    }

    @Test
    void skipsWithoutEnoughSamplesOrData() {
        assertTrue(rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 100, 100, 23)), ctx(customCard()))
                .isEmpty(), "23 samples is below the 24-sample minimum");
        assertTrue(rule.evaluate(oneConnection(1000), UtilizationSnapshot.none(), ctx(customCard())).isEmpty());
        assertTrue(rule.evaluate(oneConnection(1000),
                util(UtilizationSnapshot.Stats.unknown("c1", 1000, "no datapoints")), ctx(customCard())).isEmpty());
    }

    @Test
    void leavesIdleAndNonLiveConnectionsToOtherRules() {
        assertTrue(rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 0, 0, 168)), ctx(customCard())).isEmpty());
        EstateInventory pending = inventory().connection(withState(conn("c1", 1000, colo("p1", "DC"),
                cloud("sp1", "AWS", "DC")), "PENDING", "PENDING_APPROVAL", daysAgo(10))).build();
        assertTrue(rule.evaluate(pending, util(flat("c1", 1000, 100, 100, 168)), ctx(customCard())).isEmpty());
    }

    @Test
    void unpricedWithoutARateCardSaysWhy() {
        EstateFinding f = rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 100, 100, 168)), ctx(null)).get(0);
        assertEquals(FinancialImpact.Kind.UNPRICED, f.impact().kind());
        assertEquals("no rate card was supplied", f.impact().reason());
        assertNull(f.impact().monthlySaving(), "unpriced is never zero");
    }

    @Test
    void thresholdsAreConfigurable() {
        EstateThresholds t = EstateThresholds.builder().rightSize(50, 90).build();
        // p95 35 % (-> 455 -> 500 tier) is a finding with a 50 % threshold, not with the default 30 %.
        assertEquals(1, rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 350, 350, 168)),
                ctx(customCard(), t)).size());
        assertTrue(rule.evaluate(oneConnection(1000), util(flat("c1", 1000, 350, 350, 168)), ctx(customCard()))
                .isEmpty());
    }
}
