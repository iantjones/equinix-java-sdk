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
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.design.value.savings.DataUnit;
import api.equinix.javasdk.design.value.savings.SavingsCalculator;
import api.equinix.javasdk.design.value.savings.SavingsEstimate;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the bundled {@link ReferenceRateCard} and the out-of-the-box savings
 * calculation it enables (no caller-supplied rate card).
 */
class ReferenceRateCardTest {

    private static final ReferenceRateCard CARD = ReferenceRateCard.standard();

    @Test
    void loadsEgressRatesPerProviderAndPath() {
        assertEquals(0, new BigDecimal("0.09").compareTo(
                CARD.egress(CloudProviderType.AWS, "us", EgressPath.INTERNET, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(0, new BigDecimal("0.02").compareTo(
                CARD.egress(CloudProviderType.AWS, "us", EgressPath.PRIVATE, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(0, new BigDecimal("0.087").compareTo(
                CARD.egress(CloudProviderType.AZURE, null, EgressPath.INTERNET, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(0, new BigDecimal("0.02").compareTo(
                CARD.egress(CloudProviderType.GOOGLE_CLOUD, null, EgressPath.PRIVATE, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(PriceSource.REFERENCE,
                CARD.egress(CloudProviderType.AWS, "us", EgressPath.INTERNET, Term.MONTH_12).orElseThrow().getSource());
    }

    @Test
    void connectionPricingUsesNearestBandwidthTier() {
        assertEquals(0, new BigDecimal("75").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        assertEquals(0, new BigDecimal("350").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 10_000, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        // 5000 Mbps has no exact tier -> ceiling to the 10G tier (350).
        assertEquals(0, new BigDecimal("350").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 5_000, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        // 40000 Mbps is above the largest tier -> floor back to the 10G tier (350).
        assertEquals(0, new BigDecimal("350").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 40_000, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        // 50 Mbps is below the smallest tier -> ceiling up to the 100 Mbps tier (75).
        assertEquals(0, new BigDecimal("75").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 50, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
    }

    @Test
    void cloudRouterFallsBackToStandard() {
        assertEquals(0, new BigDecimal("1200").compareTo(
                CARD.cloudRouter("STANDARD", null, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        assertEquals(0, new BigDecimal("1200").compareTo(
                CARD.cloudRouter("NONEXISTENT", null, Term.MONTH_12).orElseThrow().getMonthlyRecurring()),
                "unknown package falls back to STANDARD");
    }

    @Test
    void exposesOnPremAndCspPortReferenceFigures() {
        assertEquals(0, new BigDecimal("0.75").compareTo(CARD.onPrem("transitPerMbpsMonth").orElseThrow()));
        assertEquals(0, new BigDecimal("195").compareTo(CARD.onPrem("powerPerKwMonth").orElseThrow()));
        assertEquals(0, new BigDecimal("300").compareTo(CARD.equinixCrossConnectMonthly().orElseThrow()));
        assertEquals(0, new BigDecimal("1642.50").compareTo(
                CARD.cspInterconnectPortMonthly(CloudProviderType.AWS, 10_000).orElseThrow()));
        assertNotNull(CARD.disclaimer());
        assertEquals("2026-06", CARD.asOf());
    }

    @Test
    void savingsCalculatorWorksOutOfTheBoxViaReferenceCard() {
        // No rate card and a null Fabric: the live card yields nothing (caught), the
        // reference card supplies both egress rates and a fallback connection price.
        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .calculate();

        assertTrue(s.isComplete(), "egress + connection both resolved from the reference card");
        assertEquals(0, new BigDecimal("4500").compareTo(s.getInternetEgressMonthlyCost()));
        assertEquals(0, new BigDecimal("1000").compareTo(s.getPrivateEgressMonthlyCost()));
        assertEquals(0, new BigDecimal("3500").compareTo(s.getMonthlyEgressSavings()));
        assertEquals(0, new BigDecimal("350").compareTo(s.getEquinixMonthlyCost()), "reference 10G VC");
        assertEquals(0, new BigDecimal("3150").compareTo(s.getNetMonthlySavings()), "3500 − 350");
    }
}
