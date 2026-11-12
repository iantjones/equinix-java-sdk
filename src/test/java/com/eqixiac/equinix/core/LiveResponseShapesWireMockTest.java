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

package com.eqixiac.equinix.core;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.model.HealthStatus;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.PortPackage;
import com.eqixiac.equinix.ibxsmartview.model.StreamingSubscription;
import com.eqixiac.equinix.internetaccess.model.implementation.IpBlockProductPrice;
import com.eqixiac.equinix.networkedge.model.PublicKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Response shapes the live services returned on 2026-11-11 that differ from their OpenAPI specs.
 * Each one failed deserialization ("Error mapping EquinixResponse") in the live read-only tier
 * before the fix. Field names and types are verbatim from the live responses; values are synthetic.
 */
class LiveResponseShapesWireMockTest extends WireMockTestBase {

    static Fabric fabric;
    static NetworkEdge networkEdge;
    static IBXSmartView ibx;
    static Operations portal;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        networkEdge = new NetworkEdge(testCredentials());
        ibx = new IBXSmartView(testCredentials());
        redirectToWireMock(fabric);
        redirectToWireMock(networkEdge);
        redirectToWireMock(ibx);
        portal = new Operations(testCredentials());
        redirectToWireMock(portal);
    }

    @AfterAll
    static void tearDown() throws Exception {
        fabric.close();
        networkEdge.close();
        ibx.close();
        portal.close();
    }

    @BeforeEach
    void reset() {
        resetStubs();
        stubOAuthToken();
    }

    @Test
    @DisplayName("Fabric /health: apiServices is an array of per-route statuses")
    void healthApiServicesArray() {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/health")).willReturn(okJson("""
                {"href":"https://api.equinix.com/fabric/v4/health","version":"4.34","release":"2026.5",
                 "description":"Fabric","state":"UP","apiServices":[
                   {"route":"/fabric/v4/connections","status":"UP","changedDateTime":"2026-11-11T00:00:00Z"},
                   {"route":"/fabric/v4/ports","status":"UP","changedDateTime":"2026-11-11T00:00:00Z"}]}""")));

        HealthStatus health = fabric.health();

        assertEquals(2, health.getAllApiServices().size());
        assertEquals("/fabric/v4/connections", health.getApiServices().getRoute());
        assertEquals("/fabric/v4/ports", health.getAllApiServices().get(1).getRoute());
    }

    @Test
    @DisplayName("Fabric /portPackages: a bare array, not a data envelope")
    void portPackagesBareArray() {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/portPackages")).willReturn(okJson("""
                [{"type":"PORT_PACKAGE","code":"XF_UNLIMITED","vcBandwidthMax":100000,"vcRemoteSupported":true,
                  "supportedServiceTypes":["MSP"],"supportedSourceTypes":["COLO","BMMR","REMOTE"]}]""")));

        List<PortPackage> packages = fabric.portPackages().list();

        assertEquals(1, packages.size());
        assertEquals("XF_UNLIMITED", packages.get(0).getCode());
    }

    @Test
    @DisplayName("Network Edge /publicKeys: a bare array, not a data envelope")
    void publicKeysBareArray() {
        wireMock.stubFor(get(urlPathEqualTo("/ne/v1/publicKeys")).willReturn(okJson("""
                [{"uuid":"k-1","keyName":"ops","keyValue":"ssh-rsa AAAA","keyType":"RSA","custOrgId":1,"projectId":"p-1"}]""")));

        List<PublicKey> keys = networkEdge.publicKeys().list();

        assertEquals(1, keys.size());
        assertEquals("ops", keys.get(0).getKeyName());
    }

    @Test
    @DisplayName("Network Edge /deviceTypes: licenseOptions keyed by SUB/BYOL deserialize")
    void deviceTypeLicenseOptions() {
        wireMock.stubFor(get(urlPathEqualTo("/ne/v1/deviceTypes")).willReturn(okJson("""
                {"pagination":{"offset":0,"limit":20,"total":1},"data":[{"uuid":"dt-1","deviceTypeCode":"C8000V",
                  "name":"C8000V","vendor":"Cisco","category":"ROUTER","supported":true,
                  "deviceManagementTypes":{"EQUINIX-CONFIGURED":{"type":"EQUINIX-CONFIGURED",
                    "licenseOptions":{"SUB":{"type":"Sub","name":"Subscription","fileUploadSupportedCluster":true,
                                             "cores":[{"core":2,"memory":4,"unit":"GB","tier":[0,1,2]}]},
                                      "BYOL":{"type":"BYOL","name":"Bring your own"}}}}}]}""")));

        assertEquals(1, networkEdge.devices().listDeviceTypes().size());
    }

    @Test
    @DisplayName("IBX SmartView subscriptions: meteredPower is a subscription filter with an ibx array")
    void subscriptionMeteredPowerFilter() {
        wireMock.stubFor(get(urlPathEqualTo("/smartview/v2/streaming/subscriptions")).willReturn(okJson("""
                [{"id":"s-1","status":"ACTIVE","messageType":{
                    "power":[{"accountNumber":"1","ibx":["DC2"]}],
                    "meteredPower":[{"accountNumber":"1","ibx":["DC2","DC5"]}]},
                  "channel":{"channelType":"AWS_IOT_CORE"},"orgId":"o","createdBy":"u",
                  "createdDateTime":"2026-11-11T00:00:00Z","updatedBy":null,"updatedDateTime":null}]""")));

        List<StreamingSubscription> subs = ibx.streamingSubscriptions().list();

        assertEquals(List.of("DC2", "DC5"), subs.get(0).getMessageType().getMeteredPower().get(0).getIbx());
    }

    @Test
    @DisplayName("Fabric metrics search: a live-shaped object summary renders as its JSON text")
    void metricsSearchObjectSummary() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/metrics/search")).willReturn(okJson("""
                {"pagination":{"offset":0,"limit":20,"total":1},"data":[{"type":"METRIC","name":"equinix.fabric.connection.bandwidth_tx.usage",
                  "unit":"Mbps","interval":"PT5M","subject":"/fabric/v4/connections/c-1",
                  "summary":{"max":10.5,"mean":2.1},"datapoints":[]}]}""")));

        List<Metric> metrics = fabric.metrics().forConnection("c-1",
                LocalDateTime.of(2026, 11, 10, 0, 0), LocalDateTime.of(2026, 11, 11, 0, 0),
                "equinix.fabric.connection.bandwidth_tx.usage");

        assertEquals(1, metrics.size());
        // the live service rejects a page size above 10 (HTTP 400 EQ-3045002, observed 2026-11-12)
        wireMock.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                urlPathEqualTo("/fabric/v4/metrics/search")).withRequestBody(
                com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath("$.pagination.limit",
                        com.github.tomakehurst.wiremock.client.WireMock.equalTo("10"))));
        // summary is an object live (the spec says string); it renders as its JSON text.
        assertEquals("{\"max\":10.5,\"mean\":2.1}", metrics.get(0).getSummary());
    }

    @Test
    @DisplayName("Customer Portal trouble-ticket types: the live key is troubleticketTypes")
    void troubleTicketTypesLiveKey() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/orders/troubleticket/types")).willReturn(okJson("""
                {"troubleticketTypes":[{"category":"Power","code":"P01","description":"Power issue",
                  "severity":"Sev2","additionalDetails":{}}]}""")));

        assertEquals(1, portal.troubleTicketOrders().getTypes().size());
        assertEquals("P01", portal.troubleTicketOrders().getTypes().get(0).getCode());
    }

    @Test
    @DisplayName("Internet Access v1 price: ipBlock.prefixLength is an array live, an integer in the spec")
    void eiaPricePrefixLengthArray() throws Exception {
        IpBlockProductPrice.CustomerIpBlock live = Constants.mapper()
                .readValue("{\"prefixLength\":[29,30]}", IpBlockProductPrice.CustomerIpBlock.class);
        IpBlockProductPrice.CustomerIpBlock spec = Constants.mapper()
                .readValue("{\"prefixLength\":28}", IpBlockProductPrice.CustomerIpBlock.class);

        assertEquals(List.of(29, 30), live.getPrefixLengths());
        assertEquals(29, live.getPrefixLength());
        assertEquals(28, spec.getPrefixLength());
    }
}
