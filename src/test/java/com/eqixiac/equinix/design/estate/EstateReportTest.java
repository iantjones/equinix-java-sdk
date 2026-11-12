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

package com.eqixiac.equinix.design.estate;

import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class EstateReportTest {

    static EstateFinding finding(String id, FindingCategory c, Severity s, Confidence conf, String resource,
                                 FinancialImpact impact) {
        return new EstateFinding(id, c, s, "t-" + id, List.of(new EstateFinding.ResourceRef("connection", resource, null)),
                List.of(), "do " + id, impact, conf, List.of(), "test");
    }

    static FinancialImpact saving(String ccy, int monthly) {
        CostLine now = CostLine.exact("now", null, BigDecimal.valueOf(monthly), null, ccy, PriceSource.CUSTOM, null);
        return FinancialImpact.between(now, null, null);
    }

    static List<EstateFinding> sample() {
        return List.of(
                finding("a", FindingCategory.RIGHT_SIZE_DOWN, Severity.MEDIUM, Confidence.HIGH, "c1", saving("USD", 100)),
                finding("b", FindingCategory.ON_DEMAND_TO_TERM, Severity.MEDIUM, Confidence.HIGH, "c1", saving("USD", 60)),
                finding("c", FindingCategory.IDLE_OR_UNUSED, Severity.MEDIUM, Confidence.HIGH, "c2", saving("EUR", 80)),
                finding("d", FindingCategory.CAPACITY_RISK, Severity.HIGH, Confidence.HIGH, "p1",
                        FinancialImpact.unpriced("no port prices")),
                finding("e", FindingCategory.HYGIENE, Severity.LOW, Confidence.HIGH, "c3", FinancialImpact.none("n/a")),
                finding("f", FindingCategory.SINGLE_POINT_OF_FAILURE, Severity.MEDIUM, Confidence.MEDIUM, "c4",
                        FinancialImpact.addedCost(CostLine.exact("add", null, BigDecimal.valueOf(300), null, "USD",
                                PriceSource.REFERENCE, null), null)));
    }

    static EstateReport report(List<EstateFinding> f) {
        EstateInventory inv = inventory().build();
        return EstateReport.assemble(inv, UtilizationSnapshot.none(), f, List.of("x"), List.of(), List.of("test"), AS_OF);
    }

    @Test
    void neverAddsCurrenciesAndCountsOverlappingSavingsOnce() {
        EstateReport r = report(sample());
        assertEquals(0, new BigDecimal("100.00").compareTo(r.addressableMonthlySaving().get("USD").monthly()),
                "c1 has two savings (100 and 60): only the larger counts; the added cost is not a saving");
        assertEquals(0, new BigDecimal("80.00").compareTo(r.addressableMonthlySaving().get("EUR").monthly()));
        assertEquals(1, r.unpriced().size());
        assertEquals("d", r.unpriced().get(0).findingId());
    }

    @Test
    void orderIsDeterministicAndByImpactTimesConfidence() {
        List<EstateFinding> shuffled = new ArrayList<>(sample());
        Collections.shuffle(shuffled, new java.util.Random(7));
        EstateReport a = report(sample());
        EstateReport b = report(shuffled);
        assertEquals(a.findings(), b.findings());
        assertEquals(a.actions(), b.actions());
        // MEDIUM/HIGH with USD 100: 3 x 1.0 x (1 + log10 101) = 9.01 beats HIGH unpriced: 4 x 1 x 1 = 4.
        assertEquals("a", a.actions().get(0).findingId());
        assertEquals(1, a.actions().get(0).rank());
        for (int i = 1; i < a.actions().size(); i++) {
            assertTrue(a.actions().get(i - 1).score() >= a.actions().get(i).score());
        }
    }

    @Test
    void categoryTotalsKeepCurrenciesApart() {
        EstateReport r = report(sample());
        EstateReport.CategoryTotal spof = r.categoryTotals().stream()
                .filter(t -> t.category() == FindingCategory.SINGLE_POINT_OF_FAILURE).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("-300.00").compareTo(spof.monthlySavingByCurrency().get("USD")));
        EstateReport.CategoryTotal cap = r.categoryTotals().stream()
                .filter(t -> t.category() == FindingCategory.CAPACITY_RISK).findFirst().orElseThrow();
        assertEquals(1, cap.unpriced());
        assertTrue(cap.monthlySavingByCurrency().isEmpty());
    }

    @Test
    void jsonRoundTripAndMarkdown() {
        EstateReport r = report(sample());
        EstateReport back = EstateReport.fromJson(r.toJson());
        assertEquals(r, back);
        String md = r.toMarkdown();
        assertTrue(md.contains("Addressable saving: **EUR 80.00/month**"), md);
        assertTrue(md.contains("Unpriced findings: 1"));
        assertTrue(md.contains("`d`: no port prices"));
        assertTrue(md.contains("- x"));
    }
}
