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

package api.equinix.javasdk.design.value;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.CustomRateCard;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.savings.DataUnit;
import api.equinix.javasdk.design.value.savings.SavingsCalculator;
import api.equinix.javasdk.design.value.savings.SavingsEstimate;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link SavingsCalculator}, driven by a deterministic
 * {@link CustomRateCard} (no network). Uses round figures close to the verified
 * reference rates: AWS internet egress $0.09/GB vs Direct Connect $0.02/GB.
 */
class SavingsCalculatorTest {

    /** A complete card: AWS internet/private egress + an Equinix connection price. */
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
        assertEquals(0, new BigDecimal("3500").compareTo(s.getMonthlyEgressSavings()), "internet − private");
        assertEquals(0, new BigDecimal("2000").compareTo(s.getEquinixMonthlyCost()));
        assertEquals(0, new BigDecimal("1500").compareTo(s.getNetMonthlySavings()), "egress saving − Equinix");
        assertEquals(0, new BigDecimal("18000").compareTo(s.getAnnualNetSavings()));
        assertEquals(0, new BigDecimal("17000").compareTo(s.getFirstYearNetSavings()), "annual − setup");
        assertEquals("USD", s.getCurrency());
    }

    @Test
    void computesBreakEvenAndPayback() {
        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(fullCard())
                .calculate();

        // break-even GB = equinixMonthly / (internetRate − privateRate) = 2000 / 0.07
        assertEquals(0, new BigDecimal("28571.43").compareTo(s.getBreakEvenGbPerMonth()));
        // payback months = setup / netMonthly = 1000 / 1500 = 0.7 (1 dp)
        assertEquals(0, new BigDecimal("0.7").compareTo(s.getPaybackMonths()));
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
        assertEquals(0, new BigDecimal("1200").compareTo(s.getNetMonthlySavings()), "3500 − 2300");
    }
}
