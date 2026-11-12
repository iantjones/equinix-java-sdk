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
import java.util.List;
import java.util.Set;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class EstateInventoryTest {

    @Test
    void jsonRoundTripPreservesEverything() {
        EstateInventory inv = EstateAnalyzerTest.snapshot();
        EstateInventory back = EstateInventory.fromJson(inv.toJson());
        assertEquals(inv, back);
        assertEquals(4, back.connections().size());
        assertEquals("p-dc-1", back.connections().get(0).aSide().portId());
        assertEquals(AS_OF, back.asOf());
    }

    @Test
    void listsAreSortedByIdWhateverTheInsertionOrder() {
        EstateInventory inv = inventory().port(port("p2", "DC", 1000, 0, 0, null)).port(port("p1", "DC", 1000, 0, 0, null))
                .build();
        assertEquals(List.of("p1", "p2"), inv.ports().stream().map(EstateInventory.PortRecord::id).toList());
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> EstateInventory.fromJson("{not json"));
    }

    @Test
    void scopeAdmitsByProjectAccountOrExplicitId() {
        EstateScope p = EstateScope.projects("P");
        assertTrue(p.admits("P", null, "x"));
        assertFalse(p.admits("Q", null, "x"));
        assertFalse(p.admits(null, null, "x"), "unknown project is not in a project scope");
        assertTrue(p.plusResources("x").admits("Q", null, "x"));
        EstateScope a = EstateScope.accounts("100");
        assertTrue(a.admits("Q", "100", "x"));
        assertFalse(a.admits("Q", "200", "x"));
        EstateScope r = EstateScope.resources(Set.of("x"));
        assertTrue(r.admits(null, null, "x"));
        assertFalse(r.admits(null, null, "y"));
        assertTrue(EstateScope.entireAccount(10).admits(null, null, "anything"));
        assertTrue(EstateScope.entireAccount(10).isUnscoped());
        assertThrows(IllegalArgumentException.class, () -> EstateScope.entireAccount(0));
    }

    @Test
    void financialImpactRangesAndCurrencies() {
        CostLine now = new CostLine("now", null, BigDecimal.valueOf(300), null, "USD", PriceSource.REFERENCE,
                BigDecimal.valueOf(240), BigDecimal.valueOf(360), null);
        CostLine then = new CostLine("then", null, BigDecimal.valueOf(200), null, "USD", PriceSource.REFERENCE,
                BigDecimal.valueOf(160), BigDecimal.valueOf(240), null);
        FinancialImpact i = FinancialImpact.between(now, then, null);
        assertEquals(0, new BigDecimal("100").compareTo(i.monthlySaving()));
        assertEquals(0, BigDecimal.ZERO.compareTo(i.monthlySavingLow()));
        assertEquals(0, new BigDecimal("200").compareTo(i.monthlySavingHigh()));
        CostLine eur = CostLine.exact("eur", null, BigDecimal.TEN, null, "EUR", PriceSource.CUSTOM, null);
        FinancialImpact mixed = FinancialImpact.between(now, eur, null);
        assertEquals(FinancialImpact.Kind.UNPRICED, mixed.kind());
        assertTrue(mixed.reason().contains("different currencies"));
        assertThrows(IllegalArgumentException.class, () -> FinancialImpact.unpriced(" "));
    }

    @Test
    void pricingRangesFollowProvenance() {
        EstatePricing pricing = new EstatePricing(com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard.standard(),
                EstateThresholds.defaults());
        EstatePricing.Priced p = pricing.connection("x", "EVPL_VC", 1000, "DC",
                com.eqixiac.equinix.design.value.ratecard.Term.MONTH_1);
        assertTrue(p.isPriced());
        assertEquals(PriceSource.REFERENCE, p.line().source());
        assertEquals(0, p.line().monthly().multiply(new BigDecimal("0.8")).compareTo(p.line().monthlyLow()));
        assertFalse(p.isExact());
        EstatePricing.Priced none = pricing.connection("x", "EVPL_VC", 0, "DC",
                com.eqixiac.equinix.design.value.ratecard.Term.MONTH_1);
        assertEquals("the bandwidth is unknown or zero", none.reason());
        EstatePricing.Priced exact = new EstatePricing(customCard(), EstateThresholds.defaults())
                .connection("x", "EVPL_VC", 1000, "DC", com.eqixiac.equinix.design.value.ratecard.Term.MONTH_1);
        assertTrue(exact.isExact());
        assertEquals(exact.line().monthly(), exact.line().monthlyLow());
    }
}
