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

package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link SavingsCalculator}, driven by a deterministic
 * {@link CustomRateCard} (no network). Uses round figures close to the verified
 * reference rates: AWS internet egress $0.09/GB vs Direct Connect $0.02/GB.
 */
class SavingsCalculatorTest {

    private static CustomRateCard fullCard() {
        return CustomRateCard.builder()
                .currency("USD")
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000"), new BigDecimal("1000"))
                .build();
    }

    @Test
    void computesEgressSavingsAndNet() {
        SavingsEstimate s = SavingsCalculator.builder(null) // rate card supplied; Fabric unused
                .egress(50, DataUnit.TERABYTE)               // 50 TB = 50,000 GB
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .connectionType(ConnectionType.EVPL_VC)
                .rateCard(fullCard())
                .calculate();

        assertTrue(s.isComplete());
        assertEquals(0, new BigDecimal("50000").compareTo(s.getMonthlyEgressGb()));
        assertEquals(0, new BigDecimal("4500").compareTo(s.getInternetEgressMonthlyCost()), "0.09 × 50000");
        assertEquals(0, new BigDecimal("1000").compareTo(s.getPrivateEgressMonthlyCost()), "0.02 × 50000");
        assertEquals(0, new BigDecimal("3500").compareTo(s.getMonthlyEgressSavings()), "internet - private");
        assertEquals(0, new BigDecimal("2000").compareTo(s.getEquinixMonthlyCost()));
        // The AWS 10G hosted-connection fee (reference 1810.40/mo = 2.48/h x 730 h) is a real cost of
        // the private path and is netted out, as TcoEngine does.
        assertEquals(0, new BigDecimal("1810.40").compareTo(s.getCloudProviderPortMonthlyCost()));
        assertEquals(0, new BigDecimal("-310.40").compareTo(s.getNetMonthlySavings()),
                "egress saving - Equinix - AWS hosted connection: 3500 - 2000 - 1810.40");
        assertEquals(0, new BigDecimal("-3724.80").compareTo(s.getAnnualNetSavings()));
        assertEquals(0, new BigDecimal("-4724.80").compareTo(s.getFirstYearNetSavings()), "annual - setup");
        assertEquals("USD", s.getCurrency());
    }

    @Test
    void computesBreakEvenAndPayback() {
        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(200, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(fullCard())
                .calculate();

        // break-even GB = (equinixMonthly + AWS port) / (internetRate - privateRate) = 3810.40 / 0.07
        assertEquals(0, new BigDecimal("54434.29").compareTo(s.getBreakEvenGbPerMonth()));
        // net = 0.07 x 200,000 - 3810.40 = 10189.60; payback = setup / net = 1000 / 10189.60 = 0.1
        assertEquals(0, new BigDecimal("0.1").compareTo(s.getPaybackMonths()));
    }

    @Test
    void incompleteWhenEgressRatesMissing() {
        // Card prices the Equinix connection but has no egress rates.
        CustomRateCard noEgress = CustomRateCard.builder()
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000"))
                .build();

        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(noEgress)
                .calculate();

        assertFalse(s.isEgressPriced());
        assertFalse(s.isComplete());
        assertEquals(0, BigDecimal.ZERO.compareTo(s.getMonthlyEgressSavings()),
                "no egress rates => no fabricated egress savings");
        assertNotNull(s.toMarkdown());
        assertTrue(s.toMarkdown().contains("Incomplete"));
    }

    @Test
    void includesCloudRouterCostWhenRequested() {
        CustomRateCard card = CustomRateCard.builder()
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000"))
                .cloudRouterRate("STANDARD", new BigDecimal("300"))
                .build();

        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .includeCloudRouter("STANDARD")
                .rateCard(card)
                .calculate();

        assertTrue(s.isComplete());
        assertEquals(0, new BigDecimal("2300").compareTo(s.getEquinixMonthlyCost()), "connection 2000 + router 300");
        assertEquals(0, new BigDecimal("-610.40").compareTo(s.getNetMonthlySavings()), "3500 - 2300 - 1810.40 AWS 10G hosted connection (2.48 USD/h x 730 h)");
    }

    @Test
    void negativeNetSavingsPropagatesAndSuppressesPayback() {
        // Small egress (1 TB) against an expensive interconnect (2000/mo): a money-losing design.
        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(1, DataUnit.TERABYTE).fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000).rateCard(fullCard())
                .calculate();

        // egress saving = 0.07 × 1000 = 70; net = 70 - 2000 - 1810.40 (AWS 10G hosted connection) = -3740.40
        assertEquals(0, new BigDecimal("-3740.40").compareTo(s.getNetMonthlySavings()));
        assertEquals(0, new BigDecimal("-44884.80").compareTo(s.getAnnualNetSavings()), "net×12, not clamped to zero");
        assertEquals(0, new BigDecimal("-45884.80").compareTo(s.getFirstYearNetSavings()), "annual - 1000 setup");
        assertNull(s.getPaybackMonths(), "no payback when net is not positive");
        assertNotNull(s.getBreakEvenGbPerMonth(), "break-even still computable: 2000 / 0.07");
    }

    @Test
    void termSelectsTheTermScopedConnectionRate() {
        // A card with distinct 12- and 36-month rates for the same connection: the builder's
        // term() lever must drive which rate the engine resolves.
        CustomRateCard termed = CustomRateCard.builder()
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .connectionRate(ConnectionType.EVPL_VC, 10_000, MetroCode.DC,
                        com.eqixiac.equinix.design.value.ratecard.Term.MONTH_12, new BigDecimal("2000"))
                .connectionRate(ConnectionType.EVPL_VC, 10_000, MetroCode.DC,
                        com.eqixiac.equinix.design.value.ratecard.Term.MONTH_36, new BigDecimal("1500"))
                .build();

        SavingsEstimate longTerm = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .term(com.eqixiac.equinix.design.value.ratecard.Term.MONTH_36)
                .rateCard(termed)
                .calculate();
        assertEquals(0, new BigDecimal("1500").compareTo(longTerm.getEquinixMonthlyCost()),
                "term(MONTH_36) must resolve the 36-month rate");
        assertEquals(0, new BigDecimal("189.60").compareTo(longTerm.getNetMonthlySavings()), "3500 - 1500 - 1810.40 AWS 10G hosted connection (2.48 USD/h x 730 h)");

        SavingsEstimate defaultTerm = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(termed)
                .calculate();
        assertEquals(0, new BigDecimal("2000").compareTo(defaultTerm.getEquinixMonthlyCost()),
                "the default term is MONTH_12, so the 12-month rate applies");
    }

    @Test
    void rejectsNegativeEgress() {
        assertThrows(IllegalArgumentException.class,
                () -> SavingsCalculator.builder(null).egress(-5, DataUnit.TERABYTE));
    }

    // Requested-but-unpriceable Cloud Router (D8)

    @Test
    void unpriceableRouterKeepsPartialFiguresAndNamesTheMissingComponent() {
        // fullCard() prices the connection (2000/mo + 1000 setup) but has no Cloud Router rate.
        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .includeCloudRouter("STANDARD")
                .rateCard(fullCard())
                .calculate();

        assertFalse(s.isEquinixPriced());
        assertFalse(s.isComplete());
        // The connection figures are kept, explicitly labelled partial via the disclaimer - 
        // consistent numbers, honestly flagged.
        assertEquals(0, new BigDecimal("2000").compareTo(s.getEquinixMonthlyCost()));
        assertEquals(0, new BigDecimal("1000").compareTo(s.getEquinixSetupCost()));
        assertTrue(s.getDisclaimer().contains("Cloud Router (STANDARD)"),
                "the disclaimer names the exact unpriced component: " + s.getDisclaimer());
        assertTrue(s.getDisclaimer().contains("partial"),
                "the disclaimer states the figures are partial: " + s.getDisclaimer());
        assertFalse(s.getDisclaimer().contains("connection cost was unavailable"),
                "the cause must not be misstated as a missing connection price: " + s.getDisclaimer());
        assertTrue(s.toMarkdown().contains("Incomplete"));
    }

    // Builder validation (D7)

    @Test
    void builderFailsFastOnInvalidInputs() {
        SavingsCalculator.Builder b = SavingsCalculator.builder(null);
        assertThrows(IllegalArgumentException.class, () -> b.egress(5, null));
        assertThrows(IllegalArgumentException.class, () -> b.egress(Double.NaN, DataUnit.GIGABYTE));
        assertThrows(IllegalArgumentException.class, () -> b.egress(Double.POSITIVE_INFINITY, DataUnit.GIGABYTE));
        assertThrows(IllegalArgumentException.class, () -> b.bandwidthMbps(0));
        assertThrows(IllegalArgumentException.class, () -> b.bandwidthMbps(-10));
        assertThrows(IllegalArgumentException.class, () -> b.fromCloud(null));
        assertThrows(IllegalArgumentException.class, () -> b.viaMetro(null));
        assertThrows(IllegalArgumentException.class, () -> b.connectionType(null));
        assertThrows(IllegalArgumentException.class, () -> b.term(null));
        assertThrows(IllegalArgumentException.class, () -> b.includeCloudRouter(null));
        assertThrows(IllegalArgumentException.class, () -> b.includeCloudRouter(" "));
    }
}
