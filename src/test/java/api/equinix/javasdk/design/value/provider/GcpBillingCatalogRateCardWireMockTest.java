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
import api.equinix.javasdk.design.value.ratecard.provider.GcpBillingCatalogRateCard;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock tests for {@link GcpBillingCatalogRateCard}, exercising the Cloud
 * Billing Catalog SKU parse with the adapter pointed at the stub server.
 */
class GcpBillingCatalogRateCardWireMockTest extends WireMockTestBase {

    private static final String PATH = "/v1/services/" + GcpBillingCatalogRateCard.COMPUTE_ENGINE_SERVICE + "/skus";

    private GcpBillingCatalogRateCard card() {
        return GcpBillingCatalogRateCard.create("test-key", wireMockUrl());
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("internet egress resolves the Network Internet Egress SKU's first paid tier")
    void resolvesInternetEgress() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus.json"))));

        EgressRate rate = card().egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        // units 0 + nanos 120000000 = $0.12/GiB, converted $/GiB -> $/GB (÷ 1.073741824).
        assertEquals(0, new BigDecimal("0.1117587090").compareTo(rate.getPricePerGb()),
                "$0.12/GiB expressed per decimal GB is 0.12 / 1.073741824 = 0.1117587090");
        assertEquals(PriceSource.PROVIDER_API, rate.getSource());
        assertNotNull(rate.getNote());
    }

    @Test
    @DisplayName("private egress resolves the Interconnect Egress SKU")
    void resolvesPrivateEgress() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus.json"))));

        EgressRate rate = card().egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();

        // interconnect egress nanos 20000000 = $0.02/GiB, converted to per decimal GB.
        assertEquals(0, new BigDecimal("0.0186264515").compareTo(rate.getPricePerGb()),
                "$0.02/GiB expressed per decimal GB is 0.02 / 1.073741824 = 0.0186264515");
    }

    @Test
    @DisplayName("internet egress selection is deterministic: the standard worldwide SKU wins, not the first, premium, or destination-qualified one")
    void deterministicInternetEgressSelection() {
        // Catalogue order is premium, China, standard, interconnect — so a naive "first egress SKU"
        // would pick premium ($0.19). The adapter must pick the representative standard worldwide
        // meter ($0.12) regardless of order.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus_multi.json"))));

        EgressRate rate = card().egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.1117587090").compareTo(rate.getPricePerGb()),
                "picks the standard $0.12/GiB worldwide meter (converted), not premium $0.19 or China $0.23");
        assertTrue(rate.getNote().contains("Network Internet Egress Worldwide"),
                "the note identifies the standard worldwide SKU it chose");
        assertFalse(rate.getNote().toLowerCase().contains("premium"), "not the Premium-Tier SKU");
        assertFalse(rate.getNote().toLowerCase().contains("china"), "not the China-destination SKU");
    }

    @Test
    @DisplayName("without an API key the adapter yields no rate and makes no call")
    void requiresApiKey() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus.json"))));

        GcpBillingCatalogRateCard noKey = GcpBillingCatalogRateCard.create(null, wireMockUrl());
        assertTrue(noKey.egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .isEmpty());
    }

    @Test
    @DisplayName("prices only Google Cloud and degrades to empty on error")
    void onlyGoogleAndDegrades() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        GcpBillingCatalogRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AWS, "us-central1", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
        assertTrue(card.egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .isEmpty(), "an API error must yield no rate");
        assertEquals(PriceSource.PROVIDER_API, card.source());
    }

    @Test
    @DisplayName("follows nextPageToken across catalogue pages, echoing the token on the page-2 request")
    void followsNextPageTokenPagination() {
        // Page 1 carries only the INTERNET SKU plus nextPageToken; the Interconnect (PRIVATE)
        // SKU lives on page 2, so resolving a PRIVATE rate proves page 2 was actually fetched.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", absent())
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus_page1.json"))));
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", equalTo("gcp-page-2-token"))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus_page2.json"))));

        GcpBillingCatalogRateCard card = card();
        EgressRate privateRate = card
                .egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.0186264515").compareTo(privateRate.getPricePerGb()),
                "the page-2 Interconnect SKU must be reachable through pagination ($0.02/GiB per GB)");

        // A page-1 SKU still resolves from the same (cached, both-pages) catalogue.
        EgressRate internetRate = card
                .egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.1117587090").compareTo(internetRate.getPricePerGb()));

        // The wire: exactly two GETs — page 1 without a token, page 2 echoing the server's
        // nextPageToken (plus the key and page-size params on both).
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH))
                .withQueryParam("key", equalTo("test-key"))
                .withQueryParam("currencyCode", equalTo("USD"))
                .withQueryParam("pageSize", equalTo("5000"))
                .withQueryParam("pageToken", absent()));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", equalTo("gcp-page-2-token")));
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("an empty nextPageToken ends pagination after a single page")
    void emptyNextPageTokenStopsPagination() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus.json"))));

        card().egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("connection and cloud-router lookups are not priced by the GCP egress adapter")
    void doesNotPriceInterconnect() {
        GcpBillingCatalogRateCard card = card();
        assertTrue(card.connection(ConnectionType.EVPL_VC, 1000, null, Term.MONTH_12).isEmpty(),
                "GCP egress adapter prices egress only, never Equinix connections");
        assertTrue(card.cloudRouter("STANDARD", null, Term.MONTH_12).isEmpty(),
                "GCP egress adapter prices egress only, never Fabric Cloud Routers");
    }

    @Test
    @DisplayName("a mid-pagination failure fails the WHOLE fetch — no answer from a partial catalogue — and is retried")
    void midPaginationFailureFailsWholeFetchAndIsRetried() {
        // Page 1 alone carries the internet SKU, so a partial-catalogue implementation would
        // happily answer the INTERNET lookup. It must not: the "cheapest matching SKU" over a
        // partial catalogue is a wrong number, so a page-2 failure fails the whole fetch.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", absent())
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus_page1.json"))));
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", equalTo("gcp-page-2-token"))
                .willReturn(aResponse().withStatus(502)));

        GcpBillingCatalogRateCard card = card();
        assertTrue(card.egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .isEmpty(), "a failed page 2 must suppress the whole catalogue, including page 1's SKUs");

        // Page 2 recovers. The SAME adapter must refetch (the failed fetch was not memoized)
        // and now serve both paths from the complete catalogue.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", equalTo("gcp-page-2-token"))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus_page2.json"))));

        EgressRate internet = card
                .egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.1117587090").compareTo(internet.getPricePerGb()));
        EgressRate privateRate = card
                .egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.0186264515").compareTo(privateRate.getPricePerGb()));
    }

    @Test
    @DisplayName("a transient first-page failure is not memoized: the same adapter retries and succeeds")
    void transientFirstPageFailureIsRetried() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

        GcpBillingCatalogRateCard card = card();
        assertTrue(card.egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .isEmpty(), "the outage yields no rate");

        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/gcp_skus.json"))));

        EgressRate rate = card
                .egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.1117587090").compareTo(rate.getPricePerGb()),
                "after the endpoint recovers the same adapter resolves the rate");
    }

    @Test
    @DisplayName("a MAX_PAGES trip with a live nextPageToken proceeds (with a warning) over the collected prefix")
    void maxPagesTruncationProceedsWithCollectedSkus() {
        // Every page advertises another page, so the fetch runs into the 25-page safety cap
        // (GcpBillingCatalogRateCard.MAX_PAGES) with a live token. The adapter logs the
        // truncation and selects over the SKUs collected so far rather than failing.
        String loopingPage = loadFixture("/json/provider/gcp_skus_page1.json")
                .replace("gcp-page-2-token", "gcp-loop-token");
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", absent())
                .willReturn(okJson(loopingPage)));
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("pageToken", equalTo("gcp-loop-token"))
                .willReturn(okJson(loopingPage)));

        GcpBillingCatalogRateCard card = card();
        EgressRate rate = card
                .egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.1117587090").compareTo(rate.getPricePerGb()),
                "truncation still selects over the collected prefix");
        wireMock.verify(25, getRequestedFor(urlPathEqualTo(PATH)));

        // The truncated catalogue is memoized (every fetched page succeeded), so further lookups
        // must not restart the 25-page walk.
        assertTrue(card.egress(CloudProviderType.GOOGLE_CLOUD, "us-central1", EgressPath.PRIVATE, Term.MONTH_12)
                .isEmpty(), "no interconnect SKU in the looping fixture");
        wireMock.verify(25, getRequestedFor(urlPathEqualTo(PATH)));
    }
}
