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

package api.equinix.javasdk.design.value.provider;

import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.EgressRate;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.design.value.ratecard.provider.AzureRetailPricesRateCard;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock tests for {@link AzureRetailPricesRateCard}, exercising the public
 * Azure Retail Prices egress path with the adapter pointed at the stub server.
 */
class AzureRetailPricesRateCardWireMockTest extends WireMockTestBase {

    private static final String PATH = "/api/retail/prices";

    private AzureRetailPricesRateCard card() {
        return AzureRetailPricesRateCard.create(wireMockUrl() + PATH);
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("internet egress resolves the Bandwidth data-transfer-out rate")
    void resolvesInternetEgress() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).withQueryParam("$filter", containing("Bandwidth"))
                .willReturn(okJson(loadFixture("/json/provider/azure_bandwidth.json"))));

        EgressRate rate = card().egress(CloudProviderType.AZURE, "eastus", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.087").compareTo(rate.getPricePerGb()),
                "skips the $0 free meter, picks the priced data-transfer-out meter");
        assertEquals(PriceSource.PROVIDER_API, rate.getSource());
        assertNotNull(rate.getNote());
    }

    @Test
    @DisplayName("private egress resolves the ExpressRoute metered rate")
    void resolvesPrivateEgress() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).withQueryParam("$filter", containing("ExpressRoute"))
                .willReturn(okJson(loadFixture("/json/provider/azure_expressroute.json"))));

        EgressRate rate = card().egress(CloudProviderType.AZURE, "eastus", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.025").compareTo(rate.getPricePerGb()));
        assertEquals(PriceSource.PROVIDER_API, rate.getSource());
    }

    @Test
    @DisplayName("prices only Azure; other providers and connection/router lookups are empty")
    void onlyPricesAzureEgress() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/azure_bandwidth.json"))));

        AzureRetailPricesRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AWS, "eastus", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
        assertTrue(card.connection(null, 1000, null, Term.MONTH_12).isEmpty());
        assertTrue(card.cloudRouter("STANDARD", null, Term.MONTH_12).isEmpty());
        assertEquals(PriceSource.PROVIDER_API, card.source());
    }

    @Test
    @DisplayName("degrades to empty when the pricing API errors")
    void degradesOnError() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        Optional<EgressRate> rate = card().egress(CloudProviderType.AZURE, "eastus", EgressPath.INTERNET, Term.MONTH_12);
        assertTrue(rate.isEmpty(), "an API error must yield no rate, not an exception");
    }
}
