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
import api.equinix.javasdk.design.value.ratecard.provider.AwsPriceListRateCard;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock tests for {@link AwsPriceListRateCard}, exercising the public AWS data-transfer
 * (internet) and Direct Connect (private) bulk-offer parses with the adapter pointed at the stub.
 */
class AwsPriceListRateCardWireMockTest extends WireMockTestBase {

    private static final String DT_PATH = "/offers/v1.0/aws/AWSDataTransfer/current/index.json";
    private static final String DX_PATH = "/offers/v1.0/aws/AWSDirectConnect/current/index.json";

    private AwsPriceListRateCard card() {
        return AwsPriceListRateCard.create(wireMockUrl() + DT_PATH, wireMockUrl() + DX_PATH);
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("internet egress resolves the first paid on-demand tier for the region")
    void resolvesInternetEgress() {
        wireMock.stubFor(get(urlPathEqualTo(DT_PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_datatransfer.json"))));

        EgressRate rate = card().egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.0900000000").compareTo(rate.getPricePerGb()),
                "skips the 1 GB free tier, picks the $0.09 first paid tier");
        assertEquals(PriceSource.PROVIDER_API, rate.getSource());
        assertNotNull(rate.getNote());
    }

    @Test
    @DisplayName("private egress resolves the lowest Direct Connect outbound rate for the region")
    void resolvesPrivateEgress() {
        wireMock.stubFor(get(urlPathEqualTo(DX_PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_directconnect.json"))));

        EgressRate rate = card().egress(CloudProviderType.AWS, "us-east-1", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.0200000000").compareTo(rate.getPricePerGb()),
                "lowest positive DX outbound rate (local $0.02), not the inter-region $0.06 or the port fee");
        assertEquals(PriceSource.PROVIDER_API, rate.getSource());
        assertNotNull(rate.getNote());
    }

    @Test
    @DisplayName("unknown/null region and other providers are empty")
    void guards() {
        wireMock.stubFor(get(urlPathEqualTo(DT_PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_datatransfer.json"))));
        wireMock.stubFor(get(urlPathEqualTo(DX_PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_directconnect.json"))));

        AwsPriceListRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AWS, "eu-west-99", EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "no product for an unknown region");
        assertTrue(card.egress(CloudProviderType.AWS, "eu-west-99", EgressPath.PRIVATE, Term.MONTH_12).isEmpty(),
                "no DX product for an unknown region");
        assertTrue(card.egress(CloudProviderType.AWS, null, EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "AWS egress pricing is region-specific");
        assertTrue(card.egress(CloudProviderType.AZURE, "us-east-1", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
    }

    @Test
    @DisplayName("connection and cloud-router lookups are not priced by the AWS egress adapter")
    void doesNotPriceInterconnect() {
        AwsPriceListRateCard card = card();
        assertTrue(card.connection(ConnectionType.EVPL_VC, 1000, null, Term.MONTH_12).isEmpty(),
                "AWS egress adapter prices egress only, never Equinix connections");
        assertTrue(card.cloudRouter("STANDARD", null, Term.MONTH_12).isEmpty(),
                "AWS egress adapter prices egress only, never Fabric Cloud Routers");
    }

    @Test
    @DisplayName("degrades to empty when the offer files are unavailable")
    void degradesOnError() {
        wireMock.stubFor(get(urlPathEqualTo(DT_PATH)).willReturn(aResponse().withStatus(500)));
        wireMock.stubFor(get(urlPathEqualTo(DX_PATH)).willReturn(aResponse().withStatus(500)));

        AwsPriceListRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
        assertTrue(card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.PRIVATE, Term.MONTH_12).isEmpty());
    }

    @Test
    @DisplayName("a transient offer-fetch failure is not memoized: the same adapter retries and succeeds")
    void transientOfferFailureIsRetried() {
        wireMock.stubFor(get(urlPathEqualTo(DT_PATH)).willReturn(aResponse().withStatus(503)));
        wireMock.stubFor(get(urlPathEqualTo(DX_PATH)).willReturn(aResponse().withStatus(503)));

        AwsPriceListRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "the outage yields no rate");
        assertTrue(card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.PRIVATE, Term.MONTH_12).isEmpty());

        // The endpoint recovers. The SAME adapter instance must fetch again — a transient failure
        // must not have been memoized as a permanent "no offer" for the adapter's lifetime.
        wireMock.stubFor(get(urlPathEqualTo(DT_PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_datatransfer.json"))));
        wireMock.stubFor(get(urlPathEqualTo(DX_PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_directconnect.json"))));

        EgressRate internet = card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.09").compareTo(internet.getPricePerGb()),
                "after the endpoint recovers the same adapter resolves the internet rate");
        EgressRate direct = card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.PRIVATE, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.02").compareTo(direct.getPricePerGb()),
                "after the endpoint recovers the same adapter resolves the Direct Connect rate");
    }

    @Test
    @DisplayName("a successful offer fetch is made once and reused; an authoritative regional miss is cached")
    void successfulOfferFetchedOnceAndReused() {
        wireMock.stubFor(get(urlPathEqualTo(DT_PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_datatransfer.json"))));

        AwsPriceListRateCard card = card();
        card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12).orElseThrow();
        assertTrue(card.egress(CloudProviderType.AWS, "eu-west-99", EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "no product for the unknown region in a successfully fetched offer");
        assertTrue(card.egress(CloudProviderType.AWS, "eu-west-99", EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "the authoritative miss is served from the result cache");
        card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12).orElseThrow();

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(DT_PATH)));
    }
}
