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

        assertEquals(0, new BigDecimal("0.08").compareTo(rate.getPricePerGb()),
                "headline Internet-routing data-transfer-out tier, not the pricier MGN (0.087) or inter-region (0.02)");
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
    @DisplayName("private egress follows NextPageLink: a cheaper ExpressRoute meter on page 2 is considered")
    void followsNextPageLinkAcrossPages() {
        // Page 1's cheapest metered rate is $0.05; the true minimum ($0.025) lives on page 2, so it
        // is only found by following NextPageLink and computing the lowest over BOTH pages.
        String page1 = loadFixture("/json/provider/azure_expressroute_page1.json")
                .replace("__NEXT__", wireMockUrl() + PATH + "?$skip=100");
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("$filter", containing("ExpressRoute"))
                .withQueryParam("$skip", absent())
                .willReturn(okJson(page1)));
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("$skip", equalTo("100"))
                .willReturn(okJson(loadFixture("/json/provider/azure_expressroute_page2.json"))));

        EgressRate rate = card().egress(CloudProviderType.AZURE, "eastus", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.025").compareTo(rate.getPricePerGb()),
                "the cheapest metered rate is on page 2, reachable only by following NextPageLink");
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH)).withQueryParam("$skip", equalTo("100")));
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

    @Test
    @DisplayName("a transient API failure is not cached: the same adapter retries and succeeds")
    void transientFailureIsRetriedNotCached() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        AzureRetailPricesRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AZURE, "eastus", EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "the outage yields no rate");

        // The API recovers. The SAME adapter must retry — a fetch FAILURE must never be stored
        // in the result cache as if the SKU authoritatively did not exist.
        wireMock.stubFor(get(urlPathEqualTo(PATH)).withQueryParam("$filter", containing("Bandwidth"))
                .willReturn(okJson(loadFixture("/json/provider/azure_bandwidth.json"))));

        EgressRate rate = card.egress(CloudProviderType.AZURE, "eastus", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.08").compareTo(rate.getPricePerGb()),
                "after recovery the same adapter resolves the rate");
    }

    @Test
    @DisplayName("an authoritative empty result set IS cached (a genuine no-such-SKU is not refetched)")
    void authoritativeEmptyResultIsCached() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson("{\"Items\": [], \"NextPageLink\": null}")));

        AzureRetailPricesRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AZURE, "nosuchregion", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
        assertTrue(card.egress(CloudProviderType.AZURE, "nosuchregion", EgressPath.INTERNET, Term.MONTH_12).isEmpty());

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("PRIVATE (ExpressRoute) lookups share one cache entry across regions — the query ignores region, so must the key")
    void privateLookupsShareOneCacheEntryAcrossRegions() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).withQueryParam("$filter", containing("ExpressRoute"))
                .willReturn(okJson(loadFixture("/json/provider/azure_expressroute.json"))));

        AzureRetailPricesRateCard card = card();
        EgressRate east = card.egress(CloudProviderType.AZURE, "eastus", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();
        EgressRate west = card.egress(CloudProviderType.AZURE, "westus2", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();
        EgressRate none = card.egress(CloudProviderType.AZURE, null, EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.025").compareTo(east.getPricePerGb()));
        assertEquals(0, east.getPricePerGb().compareTo(west.getPricePerGb()));
        assertEquals(0, east.getPricePerGb().compareTo(none.getPricePerGb()));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("a mid-pagination failure fails the whole lookup and is retried, never answered from a partial page set")
    void midPaginationFailureIsRetriedNotCached() {
        String page1 = loadFixture("/json/provider/azure_expressroute_page1.json")
                .replace("__NEXT__", wireMockUrl() + PATH + "?$skip=100");
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("$filter", containing("ExpressRoute"))
                .withQueryParam("$skip", absent())
                .willReturn(okJson(page1)));
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("$skip", equalTo("100"))
                .willReturn(aResponse().withStatus(502)));

        AzureRetailPricesRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AZURE, "eastus", EgressPath.PRIVATE, Term.MONTH_12).isEmpty(),
                "page 1's $0.05 meter must NOT be served: the lowest-rate aggregate over a partial "
                        + "result set would be a wrong number labelled authoritative");

        // Page 2 recovers; the SAME adapter retries the whole walk and now finds the true minimum.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("$skip", equalTo("100"))
                .willReturn(okJson(loadFixture("/json/provider/azure_expressroute_page2.json"))));

        EgressRate rate = card.egress(CloudProviderType.AZURE, "eastus", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.025").compareTo(rate.getPricePerGb()),
                "the retried, complete walk finds the cheaper page-2 meter");
    }
}
