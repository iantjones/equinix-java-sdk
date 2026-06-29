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
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock tests for {@link AwsPriceListRateCard}, exercising the public AWS
 * data-transfer bulk offer parse with the adapter pointed at the stub server.
 */
class AwsPriceListRateCardWireMockTest extends WireMockTestBase {

    private static final String PATH = "/offers/v1.0/aws/AWSDataTransfer/current/index.json";

    private AwsPriceListRateCard card() {
        return AwsPriceListRateCard.create(wireMockUrl() + PATH);
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("internet egress resolves the first paid on-demand tier for the region")
    void resolvesInternetEgress() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_datatransfer.json"))));

        EgressRate rate = card().egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.0900000000").compareTo(rate.getPricePerGb()),
                "skips the 1 GB free tier, picks the $0.09 first paid tier");
        assertEquals(PriceSource.PROVIDER_API, rate.getSource());
        assertNotNull(rate.getNote());
    }

    @Test
    @DisplayName("private egress and unknown/null region are not modelled here")
    void notModelled() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/aws_datatransfer.json"))));

        AwsPriceListRateCard card = card();
        assertTrue(card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.PRIVATE, Term.MONTH_12).isEmpty(),
                "Direct Connect egress is a separate offer");
        assertTrue(card.egress(CloudProviderType.AWS, "eu-west-99", EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "no product for an unknown region");
        assertTrue(card.egress(CloudProviderType.AWS, null, EgressPath.INTERNET, Term.MONTH_12).isEmpty(),
                "AWS egress pricing is region-specific");
        assertTrue(card.egress(CloudProviderType.AZURE, "us-east-1", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
    }

    @Test
    @DisplayName("degrades to empty when the offer file is unavailable")
    void degradesOnError() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertTrue(card().egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
    }
}
