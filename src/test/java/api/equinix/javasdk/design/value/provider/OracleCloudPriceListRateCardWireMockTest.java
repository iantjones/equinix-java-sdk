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
import api.equinix.javasdk.design.value.ratecard.provider.OracleCloudPriceListRateCard;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock tests for {@link OracleCloudPriceListRateCard}, exercising the public OCI Price List
 * outbound-data-transfer parse (geography-mapped) with the adapter pointed at the stub server.
 */
class OracleCloudPriceListRateCardWireMockTest extends WireMockTestBase {

    private static final String PATH = "/pls/apex/cetools/api/v1/products/";

    private OracleCloudPriceListRateCard card() {
        return OracleCloudPriceListRateCard.create(wireMockUrl() + PATH);
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private void stubPriceList() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson(loadFixture("/json/provider/oci_prices.json"))));
    }

    @Test
    @DisplayName("North-America region resolves the NA/EU outbound data-transfer first paid tier")
    void resolvesNorthAmericaEgress() {
        stubPriceList();

        EgressRate rate = card().egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.0085").compareTo(rate.getPricePerGb()),
                "skips the free allowance, picks the $0.0085 NA/EU paid tier (not the MySQL service SKU)");
        assertEquals(PriceSource.PROVIDER_API, rate.getSource());
        assertNotNull(rate.getNote());
    }

    @Test
    @DisplayName("APAC region maps to the APAC geography SKU")
    void resolvesApacEgress() {
        stubPriceList();

        EgressRate rate = card().egress(CloudProviderType.ORACLE_CLOUD, "ap-tokyo-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.025").compareTo(rate.getPricePerGb()));
    }

    @Test
    @DisplayName("null region defaults to North America")
    void nullRegionDefaultsToNorthAmerica() {
        stubPriceList();

        EgressRate rate = card().egress(CloudProviderType.ORACLE_CLOUD, null, EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.0085").compareTo(rate.getPricePerGb()));
    }

    @Test
    @DisplayName("prices only Oracle internet egress; PRIVATE and other providers are empty; degrades on error")
    void guardsAndDegradation() {
        stubPriceList();

        OracleCloudPriceListRateCard card = card();
        assertTrue(card.egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.PRIVATE, Term.MONTH_12).isEmpty(),
                "FastConnect egress is port-based, not a per-GB SKU");
        assertTrue(card.egress(CloudProviderType.AWS, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
        assertTrue(card.connection(null, 1000, null, Term.MONTH_12).isEmpty());
        assertEquals(PriceSource.PROVIDER_API, card.source());

        resetStubs();
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));
        assertTrue(card().egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12).isEmpty());
    }
}
