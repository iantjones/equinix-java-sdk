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
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.ratecard.provider.OracleCloudPriceListRateCard;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
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
    @DisplayName("follows ORDS pagination: a data-transfer SKU on page 2 is reached via the next link")
    void followsOrdsPagination() {
        // Page 1 carries only a non-matching service SKU plus hasMore=true and a "next" link; the
        // NA/EU outbound-data-transfer SKU lives on page 2, so resolving it proves the continuation
        // was followed and a "not found" is never reported off a truncated first page.
        String page1 = loadFixture("/json/provider/oci_prices_page1.json")
                .replace("__NEXT__", wireMockUrl() + PATH + "?offset=1");
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("offset", absent())
                .willReturn(okJson(page1)));
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("offset", equalTo("1"))
                .willReturn(okJson(loadFixture("/json/provider/oci_prices_page2.json"))));

        EgressRate rate = card().egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("0.0085").compareTo(rate.getPricePerGb()),
                "the NA/EU SKU is on page 2, only reachable by following the ORDS next link");
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH)).withQueryParam("offset", equalTo("1")));
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

    @Test
    @DisplayName("a transient fetch failure is not memoized: the same adapter retries and succeeds")
    void transientFailureIsRetriedNotCached() {
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

        OracleCloudPriceListRateCard card = card();
        assertTrue(card.egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12)
                .isEmpty(), "the outage yields no rate");

        // The endpoint recovers. The SAME adapter must fetch again — the failure must not have
        // been memoized as an empty catalogue for the adapter's lifetime.
        stubPriceList();

        EgressRate rate = card.egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.0085").compareTo(rate.getPricePerGb()),
                "after the endpoint recovers the same adapter resolves the rate");
    }

    @Test
    @DisplayName("a complete catalogue is fetched once and reused across lookups")
    void completeCatalogueFetchedOnceAcrossLookups() {
        stubPriceList();

        OracleCloudPriceListRateCard card = card();
        card.egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12).orElseThrow();
        card.egress(CloudProviderType.ORACLE_CLOUD, "ap-tokyo-1", EgressPath.INTERNET, Term.MONTH_12).orElseThrow();
        card.egress(CloudProviderType.ORACLE_CLOUD, null, EgressPath.INTERNET, Term.MONTH_12).orElseThrow();

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("over an incomplete catalogue a found SKU is a real datum, but a miss is not cached and is retried")
    void incompleteCatalogueMissIsNotCached() {
        // Page 1 carries the NA/EU SKU and a continuation; page 2 is down. The catalogue is
        // therefore incomplete: a positive match from page 1 is trustworthy, but a miss could
        // just mean the SKU lives on the unreachable page.
        String page1 = """
                {
                  "hasMore": true,
                  "items": [
                    {
                      "partNumber": "B88327",
                      "displayName": "Outbound Data Transfer - Originating in North America, Europe, and UK",
                      "currencyCodeLocalizations": [
                        {
                          "currencyCode": "USD",
                          "prices": [
                            { "model": "PAY_AS_YOU_GO", "value": 0, "rangeMin": 0 },
                            { "model": "PAY_AS_YOU_GO", "value": 0.0085, "rangeMin": 10240 }
                          ]
                        }
                      ]
                    }
                  ],
                  "links": [ { "rel": "next", "href": "__NEXT__" } ]
                }
                """.replace("__NEXT__", wireMockUrl() + PATH + "?offset=1");
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("offset", absent())
                .willReturn(okJson(page1)));
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("offset", equalTo("1"))
                .willReturn(aResponse().withStatus(502)));

        OracleCloudPriceListRateCard card = card();
        EgressRate found = card.egress(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.0085").compareTo(found.getPricePerGb()),
                "a SKU found in the retrieved pages is a real datum despite the truncation");

        assertTrue(card.egress(CloudProviderType.ORACLE_CLOUD, "ap-tokyo-1", EgressPath.INTERNET, Term.MONTH_12)
                .isEmpty(), "the APAC SKU may live on the unreachable page — a miss is not authoritative");

        // Page 2 recovers with the APAC SKU. The miss was not cached and the incomplete catalogue
        // was not memoized, so the SAME adapter retries and now finds it.
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("offset", equalTo("1"))
                .willReturn(okJson("""
                        {
                          "hasMore": false,
                          "items": [
                            {
                              "partNumber": "B93455",
                              "displayName": "Outbound Data Transfer - Originating in APAC, Japan, and South America",
                              "currencyCodeLocalizations": [
                                {
                                  "currencyCode": "USD",
                                  "prices": [
                                    { "model": "PAY_AS_YOU_GO", "value": 0, "rangeMin": 0 },
                                    { "model": "PAY_AS_YOU_GO", "value": 0.025, "rangeMin": 10240 }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                        """)));

        EgressRate apac = card.egress(CloudProviderType.ORACLE_CLOUD, "ap-tokyo-1", EgressPath.INTERNET, Term.MONTH_12)
                .orElseThrow();
        assertEquals(0, new BigDecimal("0.025").compareTo(apac.getPricePerGb()),
                "the retried, complete walk finds the page-2 APAC SKU");
    }
}
