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

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.savings.DataUnit;
import api.equinix.javasdk.design.value.savings.SavingsEstimate;
import api.equinix.javasdk.design.value.tco.CostBreakdown;
import api.equinix.javasdk.design.value.tco.DeploymentArchetype;
import api.equinix.javasdk.design.value.tco.TcoComparison;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static api.equinix.javasdk.core.ResponseStubs.stubPaginatedPost;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the calculators' <em>default</em> rate-card resolution end-to-end: when no
 * {@code rateCard(...)} is supplied, {@code SavingsCalculatorEngine} and {@code TcoEngine}
 * resolve {@code RateCard.standardChain(fabric)} — live {@code EquinixRateCard} pricing
 * layered over the bundled {@code ReferenceRateCard} — so the Equinix interconnect cost
 * must come off the WireMock-stubbed {@code /fabric/v4/prices/search} catalogue while the
 * cloud-egress rates come from the reference card.
 *
 * <p>Every other calculator test injects a card explicitly ({@code SavingsCalculatorTest})
 * or runs the chain with a null gateway ({@code TcoComparisonTest}); this suite is the only
 * one where live Fabric pricing actually flows through a calculator.</p>
 */
class ValueDefaultRateCardWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
        // The live leg of the default chain: EVPL_VC_SV_DC_100 — 100 Mbps, MRC 250.00 / NRC 0.00.
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");
    }

    @Test
    @DisplayName("savingsCalculator() without a rate card prices the interconnect from live Fabric pricing")
    void savingsCalculatorDefaultsToLiveLayeredChain() {
        // 10 TB = 10,000 GB. Reference AWS egress: internet $0.09/GB, private $0.02/GB.
        SavingsEstimate s = fabric.savingsCalculator()
                .egress(10, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(100)
                .calculate();

        assertTrue(s.isComplete(), "live connection price + reference egress rates = complete");
        assertTrue(s.isEquinixPriced());
        assertTrue(s.isEgressPriced());

        // The interconnect cost is the LIVE catalogue row, not a reference or heuristic figure.
        assertEquals(0, new BigDecimal("250.00").compareTo(s.getEquinixMonthlyCost()),
                "the 100 Mbps EVPL_VC DC row prices at 250.00: " + s.getEquinixMonthlyCost());
        assertEquals(0, BigDecimal.ZERO.compareTo(s.getEquinixSetupCost()));

        // The egress legs come from the reference layer of the same chain.
        assertEquals(0, new BigDecimal("900").compareTo(s.getInternetEgressMonthlyCost()), "0.09 × 10,000");
        assertEquals(0, new BigDecimal("200").compareTo(s.getPrivateEgressMonthlyCost()), "0.02 × 10,000");
        assertEquals(0, new BigDecimal("700").compareTo(s.getMonthlyEgressSavings()));
        assertEquals(0, new BigDecimal("450").compareTo(s.getNetMonthlySavings()), "700 − 250");

        // The wire: the default chain really fetched the live catalogue, type-scoped.
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search"))
                .withRequestBody(containing("VIRTUAL_CONNECTION_PRODUCT")));
    }

    @Test
    @DisplayName("tcoComparison() without a rate card folds the live Fabric connection price into the Equinix archetype")
    void tcoComparisonDefaultsToLiveLayeredChain() {
        TcoComparison tco = fabric.tcoComparison()
                .egress(10, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(100)
                .compare();

        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertTrue(equinix.isPriced(), () -> "live VC price + reference private egress: " + equinix.getNote());

        // The Fabric-connection line item is the live 250.00 catalogue price, and the private
        // egress line is the reference rate (0.02 × 10,000 GB).
        assertEquals(0, new BigDecimal("250.00").compareTo(equinix.getLineItems().get("Equinix Fabric connection")),
                "live catalogue row, not the reference connection figure: " + equinix.getLineItems());
        assertEquals(0, new BigDecimal("200").compareTo(
                equinix.getLineItems().get("Cloud egress (private interconnect)")));

        // The internet baseline still prices from the reference card.
        CostBreakdown internet = tco.breakdown(DeploymentArchetype.PUBLIC_CLOUD_INTERNET).orElseThrow();
        assertEquals(0, new BigDecimal("900").compareTo(internet.getMonthlyTotal()), "0.09 × 10,000");

        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search"))
                .withRequestBody(containing("VIRTUAL_CONNECTION_PRODUCT")));
    }

    @Test
    @DisplayName("when the live catalogue cannot price the request, the default chain falls back to the reference layer")
    void defaultChainFallsBackToReferenceWhenLiveCannotPrice() {
        // 10 Gbps has no row in the stubbed catalogue, so EquinixRateCard yields empty and the
        // layered chain falls through to the ReferenceRateCard's 10 Gbps figure ($350/mo,
        // matching TcoComparisonTest's reference expectations).
        SavingsEstimate s = fabric.savingsCalculator()
                .egress(10, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .calculate();

        assertTrue(s.isEquinixPriced(), "the reference layer must catch what live pricing cannot");
        assertEquals(0, new BigDecimal("350").compareTo(s.getEquinixMonthlyCost()),
                "the reference 10 Gbps VC rate: " + s.getEquinixMonthlyCost());
    }
}
