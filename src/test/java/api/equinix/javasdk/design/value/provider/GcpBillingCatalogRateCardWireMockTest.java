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

        assertEquals(0, new BigDecimal("0.12").compareTo(rate.getPricePerGb()),
                "units 0 + nanos 120000000 = $0.12/GiB");
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

        assertEquals(0, new BigDecimal("0.02").compareTo(rate.getPricePerGb()),
                "interconnect egress nanos 20000000 = $0.02/GiB");
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
}
