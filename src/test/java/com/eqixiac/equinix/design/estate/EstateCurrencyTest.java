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

import com.eqixiac.equinix.design.cost.CurrencyConverters;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Estate savings are reported in one currency. London is quoted in GBP by the live Fabric price search (observed
 * 2026-11-12); the card here is synthetic GBP figures and the rate is a fixed test rate.
 */
class EstateCurrencyTest {

    /** EVPL in GBP: 500 Mbps 160, 1000 Mbps 240. */
    private static RateCard gbpCard() {
        return CustomRateCard.builder().currency("GBP")
                .connectionRate(ConnectionType.EVPL_VC, 500, BigDecimal.valueOf(160))
                .connectionRate(ConnectionType.EVPL_VC, 1000, BigDecimal.valueOf(240))
                .build();
    }

    /** 1 GBP = 1.25 USD, as a caller-supplied rate. */
    private static CurrencyConverter gbpToUsd() {
        return (from, to) -> "GBP".equals(from) && "USD".equals(to)
                ? Optional.of(new CurrencyConverter.Rate("GBP", "USD", new BigDecimal("1.25"),
                CurrencyConverter.Kind.SUPPLIED, "test treasury rate", "2026-11-12", 0))
                : Optional.empty();
    }

    private static EstateInventory londonConnection() {
        return inventory().connection(conn("c1", 1000, colo("p1", "LD"), cloud("sp1", "AWS", "LD"))).build();
    }

    @Test
    @DisplayName("a right-size finding in London (GBP prices) is reported in USD with the original GBP amounts and the rate")
    void londonFindingInUsd() {
        EstateReport r = EstateAnalyzer.fromSnapshot(londonConnection())
                .utilization(util(flat("c1", 1000, 200, 250, 168)))
                .rateCard(gbpCard()).currencyConverter(gbpToUsd()).defaultRates(CurrencyConverters.none())
                .analyze();

        EstateFinding f = r.findings(FindingCategory.RIGHT_SIZE_DOWN).get(0);
        assertEquals(FinancialImpact.Kind.PRICED, f.impact().kind());
        assertEquals("USD", f.impact().currency());
        // (240 - 160) GBP x 1.25 = USD 100.00
        assertEquals(0, new BigDecimal("100.00").compareTo(f.impact().monthlySaving()));
        CostLine current = f.impact().basis().get(0);
        assertTrue(current.converted());
        assertEquals("GBP", current.conversion().originalCurrency());
        assertEquals(0, new BigDecimal("240").compareTo(current.conversion().originalMonthly()));
        assertEquals(0, new BigDecimal("1.25").compareTo(current.conversion().rate()));
        assertEquals("test treasury rate", current.conversion().source());
        assertEquals("2026-11-12", current.conversion().asOf());

        assertEquals("USD", r.reportingCurrency());
        assertEquals(java.util.Set.of("USD"), r.addressableMonthlySaving().keySet());
        assertTrue(r.currencyNotes().get(0).contains("converted to USD: GBP at test treasury rate"),
                r.currencyNotes().toString());
        assertTrue(r.toMarkdown().contains("- Reporting currency: USD"));
        assertEquals(r.reportingCurrency(), EstateReport.fromJson(r.toJson()).reportingCurrency());
    }

    @Test
    @DisplayName("the reporting currency is caller-settable, and a price no source can convert is unpriced, not mixed")
    void reportingCurrencyAndMissingRate() {
        EstateReport inGbp = EstateAnalyzer.fromSnapshot(londonConnection())
                .utilization(util(flat("c1", 1000, 200, 250, 168)))
                .rateCard(gbpCard()).currency("GBP").defaultRates(CurrencyConverters.none())
                .analyze();
        EstateFinding f = inGbp.findings(FindingCategory.RIGHT_SIZE_DOWN).get(0);
        assertEquals("GBP", f.impact().currency());
        assertEquals(0, new BigDecimal("80").compareTo(f.impact().monthlySaving()));
        assertTrue(inGbp.currencyNotes().isEmpty(), "nothing was converted");

        EstateReport noRate = EstateAnalyzer.fromSnapshot(londonConnection())
                .utilization(util(flat("c1", 1000, 200, 250, 168)))
                .rateCard(gbpCard()).defaultRates(CurrencyConverters.none())
                .analyze();
        EstateFinding u = noRate.findings(FindingCategory.RIGHT_SIZE_DOWN).get(0);
        assertEquals(FinancialImpact.Kind.UNPRICED, u.impact().kind());
        assertTrue(noRate.addressableMonthlySaving().isEmpty(), "a GBP figure is never added to USD figures");
    }
}
