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

package com.eqixiac.equinix.design.value.provider;

import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.ratecard.provider.AwsPriceListRateCard;
import com.eqixiac.equinix.design.value.ratecard.provider.AzureRetailPricesRateCard;
import com.eqixiac.equinix.design.value.ratecard.provider.GcpBillingCatalogRateCard;
import com.eqixiac.equinix.design.value.ratecard.provider.OracleCloudPriceListRateCard;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The live provider-pricing adapters do not price the native multicloud path. Each adapter chose
 * between two offers with a two-way test on {@link EgressPath} ({@code INTERNET} or "the other
 * one"), so the third path added here would have been answered with a Direct Connect,
 * ExpressRoute or internet rate. These tests stub every offer with its recorded fixture — the
 * state in which an unguarded adapter would return such a rate — and require an empty result
 * with no HTTP request made.
 */
class ProviderAdaptersMulticloudPathWireMockTest extends WireMockTestBase {

    private static final String AWS_DT = "/offers/v1.0/aws/AWSDataTransfer/current/index.json";
    private static final String AWS_DX = "/offers/v1.0/aws/AWSDirectConnect/current/index.json";
    private static final String AZURE = "/api/retail/prices";
    private static final String GCP = "/v1/services/" + GcpBillingCatalogRateCard.COMPUTE_ENGINE_SERVICE + "/skus";
    private static final String OCI = "/oci-prices";

    @BeforeEach
    void stubEveryOffer() {
        resetStubs();
        wireMock.stubFor(get(urlPathEqualTo(AWS_DT)).willReturn(okJson(loadFixture("/json/provider/aws_datatransfer.json"))));
        wireMock.stubFor(get(urlPathEqualTo(AWS_DX)).willReturn(okJson(loadFixture("/json/provider/aws_directconnect.json"))));
        wireMock.stubFor(get(urlPathEqualTo(AZURE)).withQueryParam("$filter", containing("Bandwidth"))
                .willReturn(okJson(loadFixture("/json/provider/azure_bandwidth.json"))));
        wireMock.stubFor(get(urlPathEqualTo(AZURE)).withQueryParam("$filter", containing("ExpressRoute"))
                .willReturn(okJson(loadFixture("/json/provider/azure_expressroute.json"))));
        wireMock.stubFor(get(urlPathEqualTo(GCP)).willReturn(okJson(loadFixture("/json/provider/gcp_skus.json"))));
        wireMock.stubFor(get(urlPathEqualTo(OCI)).willReturn(okJson(loadFixture("/json/provider/oci_prices.json"))));
    }

    @Test
    @DisplayName("AWS adapter: MULTICLOUD_INTERCONNECT is empty, not the Direct Connect rate")
    void awsAdapterDoesNotAnswerTheNativePathWithTheDirectConnectRate() {
        AwsPriceListRateCard card = AwsPriceListRateCard.create(wireMockUrl() + AWS_DT, wireMockUrl() + AWS_DX);

        assertTrue(card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.MULTICLOUD_INTERCONNECT, Term.MONTH_12)
                .isEmpty());
        wireMock.verify(0, anyRequestedFor(urlPathEqualTo(AWS_DX)));
        wireMock.verify(0, anyRequestedFor(urlPathEqualTo(AWS_DT)));
        // The guard is specific to the new path: PRIVATE still resolves from the same stub.
        assertTrue(card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.PRIVATE, Term.MONTH_12).isPresent());
    }

    @Test
    @DisplayName("Azure adapter: MULTICLOUD_INTERCONNECT is empty, not the ExpressRoute rate")
    void azureAdapterDoesNotAnswerTheNativePathWithTheExpressRouteRate() {
        AzureRetailPricesRateCard card = AzureRetailPricesRateCard.create(wireMockUrl() + AZURE);

        assertTrue(card.egress(CloudProviderType.AZURE, "eastus", EgressPath.MULTICLOUD_INTERCONNECT, Term.MONTH_12)
                .isEmpty());
        wireMock.verify(0, anyRequestedFor(urlPathEqualTo(AZURE)));
        assertTrue(card.egress(CloudProviderType.AZURE, "eastus", EgressPath.PRIVATE, Term.MONTH_12).isPresent());
    }

    @Test
    @DisplayName("GCP adapter: MULTICLOUD_INTERCONNECT is empty, not the internet-egress rate")
    void gcpAdapterDoesNotAnswerTheNativePathWithTheInternetRate() {
        GcpBillingCatalogRateCard card = GcpBillingCatalogRateCard.create("test-key", wireMockUrl());

        assertTrue(card.egress(CloudProviderType.GOOGLE_CLOUD, "us-east4", EgressPath.MULTICLOUD_INTERCONNECT,
                Term.MONTH_12).isEmpty());
        wireMock.verify(0, anyRequestedFor(urlPathEqualTo(GCP)));
    }

    @Test
    @DisplayName("OCI adapter: MULTICLOUD_INTERCONNECT is empty")
    void ociAdapterDoesNotPriceTheNativePath() {
        OracleCloudPriceListRateCard card = OracleCloudPriceListRateCard.create(wireMockUrl() + OCI);

        assertTrue(card.egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.MULTICLOUD_INTERCONNECT,
                Term.MONTH_12).isEmpty());
        wireMock.verify(0, anyRequestedFor(urlPathEqualTo(OCI)));
    }

    @Test
    @DisplayName("no provider adapter prices the native link's flat fee")
    void noAdapterPricesTheFlatFee() {
        MulticloudLinkRequest request = MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(10_000).build();
        RateCard[] adapters = {
                AwsPriceListRateCard.create(wireMockUrl() + AWS_DT, wireMockUrl() + AWS_DX),
                AzureRetailPricesRateCard.create(wireMockUrl() + AZURE),
                GcpBillingCatalogRateCard.create("test-key", wireMockUrl()),
                OracleCloudPriceListRateCard.create(wireMockUrl() + OCI),
        };
        for (RateCard adapter : adapters) {
            assertTrue(adapter.multicloudLink(request).isEmpty(), adapter.getClass().getSimpleName());
        }
        assertTrue(wireMock.getAllServeEvents().isEmpty(), "the default seam makes no request");
    }
}
